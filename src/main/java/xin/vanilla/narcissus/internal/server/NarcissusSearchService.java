package xin.vanilla.narcissus.internal.server;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.api.BaniraConfigs;
import xin.vanilla.banira.common.util.DimensionUtils;
import xin.vanilla.banira.common.util.ItemUtils;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.config.CommonSearchConfiguration;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.internal.fabric.search.FabricSearchChunkBackend;
import xin.vanilla.narcissus.internal.fabric.search.SectionSearchPruner;
import xin.vanilla.narcissus.internal.server.search.NarcissusSearchRequest;
import xin.vanilla.narcissus.notification.NarcissusNotificationTypes;
import xin.vanilla.narcissus.search.*;
import xin.vanilla.narcissus.util.SafeBlockChecker;
import xin.vanilla.narcissus.util.TeleportCountdownTracker;
import java.util.*;
import java.util.function.Consumer;

/** Owns searches for one server lifecycle. All world access stays on its owner thread. */
public final class NarcissusSearchService implements AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger();
    private static NarcissusSearchService current;
    private final MinecraftServer server;
    private final CommonSearchConfiguration configuration;
    private final SearchChunkPool pool;
    private final SearchCoordinator coordinator;

    private NarcissusSearchService(MinecraftServer server) {
        this.server = server;
        configuration = new CommonSearchConfiguration(BaniraConfigs.holder(CommonConfig.class));
        pool = new SearchChunkPool(new FabricSearchChunkBackend(server), server::isSameThread);
        coordinator = new SearchCoordinator(server::isSameThread, configuration::execution, System::nanoTime);
    }
    public static void start(MinecraftServer server) {
        stop();
        current = new NarcissusSearchService(server);
    }
    public static NarcissusSearchService get() { return current; }
    public static void stop() {
        NarcissusSearchService old = current;
        current = null;
        if (old != null) {
            try { old.close(); }
            catch (RuntimeException error) { LOGGER.error("Teleport search shutdown failed; remaining server cleanup must continue", error); }
        }
    }
    private void checkOwner() { if (!server.isSameThread()) throw new IllegalStateException("Teleport search requires owner thread"); }
    public void tick() {
        checkOwner();
        try { pool.beginTick(); coordinator.tick(); }
        catch (RuntimeException error) {
            LOGGER.error("Teleport chunk maintenance failed; cancelling searches", error);
            close();
        }
    }
    public void disconnect(UUID player) {
        coordinator.cancel(player);
        coordinator.pruneInvalid();
    }
    public int activeCount() { return coordinator.activeCount(); }
    public int leaseCount() { return pool.leaseCount(); }
    public int pendingCount() { return pool.pendingCount(); }
    public boolean searchDestination(ServerPlayer player, SafeWorldCoordinate destination, EnumTeleportType type,
                                     int range, NarcissusCostService.Ticket ticket, Consumer<Session> callback) {
        return admit(player, destination, type, false, ticket, callback,
                request -> request.destination(destination, type, range));
    }
    public boolean searchView(ServerPlayer player, boolean safe, int range,
                              NarcissusCostService.Ticket ticket, Consumer<Session> callback) {
        return admit(player, new SafeWorldCoordinate(player), EnumTeleportType.TP_VIEW, true, ticket, callback, request -> {
            Vec3 eye = player.getEyePosition(1);
            Vec3 step = player.getViewVector(1).normalize().scale(.75);
            request.view(eye.x, eye.y, eye.z, step.x, step.y, step.z, range, safe);
        });
    }
    private boolean admit(ServerPlayer player, SafeWorldCoordinate target, EnumTeleportType type, boolean view,
                          NarcissusCostService.Ticket ticket, Consumer<Session> callback, Consumer<NarcissusSearchRequest> setup) {
        checkOwner();
        if (ticket == null || !ticket.live()) return false;
        SearchChunkPool.Lease lease = null;
        try {
            CommonSearchConfiguration.Snapshot config = configuration.capture(type, view);
            ServerLevel world = server.getLevel(target.dimension());
            if (world == null || player.server != server) throw new IllegalStateException("Teleport world is unavailable");
            NativeAccess access = new NativeAccess(player, world, config, ticket);
            lease = pool.open(target.dimension().location());
            NarcissusSearchRequest request = new NarcissusSearchRequest(player.getUUID(), new SafeWorldCoordinate(player),
                    config, access, lease, r -> callback.accept(new Session(this, r)));
            setup.accept(request);
            return coordinator.submit(request);
        } catch (RuntimeException error) {
            try { ticket.cancel(); }
            finally { if (lease != null) lease.close(); }
            LOGGER.error("Teleport search admission failed", error);
            notifyFailure(player, SearchTask.Failure.ERROR);
            return false;
        }
    }
    public void continueDestination(Session session, SafeWorldCoordinate destination, EnumTeleportType type, int range,
                                    Consumer<Session> callback) {
        checkSession(session);
        session.request.callback(r -> callback.accept(new Session(this, r)));
        session.request.destination(destination, type, range);
    }
    public boolean complete(Session session, Runnable action) {
        checkSession(session);
        return session.request.complete(action);
    }
    private void checkSession(Session session) {
        checkOwner();
        if (session.owner != this) throw new IllegalArgumentException("Search belongs to another server lifecycle");
    }
    @Override public void close() {
        checkOwner();
        try { coordinator.close(); }
        finally { pool.close(); }
    }
    public static final class Session {
        private final NarcissusSearchService owner;
        private final NarcissusSearchRequest request;
        private Session(NarcissusSearchService owner, NarcissusSearchRequest request) { this.owner = owner; this.request = request; }
        public SafeWorldCoordinate destination() { return request.destination(); }
        public NarcissusSearchRequest.ResultKind kind() { return request.kind(); }
        public BlockState supportState() { return request.supportState(); }
        public ItemStack supportItem() { return request.supportItem().copy(); }
        public void waitForCountdown() { owner.checkSession(this); request.waitForCountdown(); }
        public void attachCountdown(TeleportCountdownTracker.Session countdown) {
            owner.checkSession(this);
            if (countdown != null) request.onClose(countdown::cancel);
        }
        public void cancel() {
            owner.checkSession(this);
            try { request.cancel(SearchTask.Failure.CANCELLED); }
            finally { request.close(); }
        }
    }
    public static boolean validActor(ServerPlayer captured, ServerPlayer online, ServerLevel source, boolean ticketLive) {
        return ticketLive && online == captured && captured.isAlive() && !captured.isRemoved() && captured.serverLevel() == source;
    }
    private final class NativeAccess implements NarcissusSearchRequest.Access {
        private final ServerPlayer player;
        private final ServerLevel source, world;
        private final CommonSearchConfiguration.Snapshot config;
        private final NarcissusCostService.Ticket ticket;
        private final SafeBlockChecker checker;
        private final SectionSearchPruner pruner;
        private final Map<Long, LevelChunk> sectionChunks = new HashMap<>();
        private final List<ItemStack> inventory;
        NativeAccess(ServerPlayer player, ServerLevel world, CommonSearchConfiguration.Snapshot config, NarcissusCostService.Ticket ticket) {
            this.player = player;
            this.source = player.serverLevel();
            this.world = world;
            this.config = config;
            this.ticket = ticket;
            checker = new SafeBlockChecker(world, player, config.policy());
            pruner = new SectionSearchPruner(new SectionSearchPruner.ChunkAccess() {
                public boolean ready(int x, int z) {
                    long key = ChunkPos.asLong(x, z);
                    if (sectionChunks.containsKey(key)) return true;
                    LevelChunk chunk = world.getChunkSource().getChunkNow(x, z);
                    if (chunk == null) return false;
                    sectionChunks.put(key, chunk);
                    return true;
                }
                public LevelChunkSection section(int x, int z, int y) {
                    LevelChunk chunk = sectionChunks.get(ChunkPos.asLong(x, z));
                    if (chunk == null) throw new IllegalStateException("Section requested without ready chunk");
                    LevelChunkSection[] sections = chunk.getSections();
                    int index = y - (world.getMinBuildHeight() >> 4);
                    return index < 0 || index >= sections.length ? null : sections[index];
                }
            }, config.policy());
            inventory = new ArrayList<>();
            if (config.getBlockFromInventory()) for (ItemStack item : ItemUtils.getAllPlayerItems(player)) inventory.add(item.copy());
        }
        public boolean live() {
            checkOwner();
            return validActor(player, server.getPlayerList().getPlayer(player.getUUID()), source, ticket.live())
                    && player.server == server && server.getLevel(world.dimension()) == world;
        }
        public boolean policyMatches() { return true; }
        public void beginSlice() { checkOwner(); checker.beginSlice(); pruner.beginSlice(); sectionChunks.clear(); }
        public SafeCandidateCursor.YFilter heightFilter(SearchBox box, boolean belowAir) {
            checkOwner();
            return world.isDebug() ? null : pruner.filter(box.minZ, box.maxZ, belowAir);
        }
        public boolean safe(BlockPos pos, boolean belowAir) { checkOwner(); return checker.isSafeBlock(pos, belowAir); }
        public boolean blocksMotion(BlockPos pos) { checkOwner(); return world.getBlockState(pos).blocksMotion(); }
        public int minY() { return DimensionUtils.getWorldMinY(world); }
        public int maxY() { return DimensionUtils.getWorldMaxY(world); }
        public BlockState supportState() {
            for (BlockState state : config.policy().supportStates()) {
                ItemStack item = new ItemStack(state.getBlock());
                if (!config.getBlockFromInventory() || inventory.stream().anyMatch(stack -> !stack.isEmpty() && stack.getItem() == item.getItem())) return state;
            }
            return null;
        }
        public boolean hasSupportItem(ItemStack item) {
            return item.isEmpty() || ItemUtils.getAllPlayerItems(player).stream().anyMatch(stack -> !stack.isEmpty() && stack.getItem() == item.getItem());
        }
        public SafeWorldCoordinate random(SafeWorldCoordinate origin, int range) {
            return SafeWorldCoordinate.random(origin, range, world.dimension(), minY(), maxY(), config.randomDistanceLimit());
        }
        public void cancelTicket() { ticket.cancel(); }
        public void failed(SearchTask.Failure reason) { notifyFailure(player, reason); }
        public void viewNotFound(boolean safe) {
            MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto(safe ? "tp_view_safe_not_found" : "tp_view_not_found"), NarcissusNotificationTypes.TELEPORT_ERROR);
        }
    }
    private static void notifyFailure(ServerPlayer player, SearchTask.Failure reason) {
        String key = reason == SearchTask.Failure.BUSY ? "search_busy" : reason == SearchTask.Failure.TIMEOUT ? "search_timeout"
                : reason == SearchTask.Failure.POLICY_CHANGED ? "search_changed" : "search_failed";
        MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto(key), NarcissusNotificationTypes.TELEPORT_ERROR);
    }
}
