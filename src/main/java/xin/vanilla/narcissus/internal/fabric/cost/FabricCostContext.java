package xin.vanilla.narcissus.internal.fabric.cost;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import xin.vanilla.banira.common.util.DimensionUtils;
import xin.vanilla.narcissus.api.cost.*;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.data.cost.CostConfiguration;
import xin.vanilla.narcissus.data.cost.CostContextInput;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumSafeMode;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.internal.server.cost.CostEvaluation;

import java.util.*;

public final class FabricCostContext {
    private FabricCostContext() {
    }

    public static CostEvaluation open(long operationId, long generationId, CostPhase phase, EnumTeleportType type,
                                      CostConfiguration configuration, ServerPlayer moving, ServerPlayer paying,
                                      ServerPlayer requester, ServerPlayer targetPlayer,
                                      SafeWorldCoordinate destination, TeleportRequest request, int cooldownSeconds) {
        MinecraftServer server = Objects.requireNonNull(moving, "moving").server;
        if (server == null || !server.isSameThread() || Objects.requireNonNull(paying, "paying").server != server) {
            throw new IllegalStateException("Cost context requires the owning server thread");
        }
        if (request != null) {
            ServerPlayer expected = type == EnumTeleportType.TP_HERE ? request.getTarget() : request.getRequester();
            if (type != request.getTeleportType() || moving != expected || paying != request.getRequester()) {
                throw new IllegalArgumentException("Cost actors do not match the teleport request");
            }
        }
        CostPosition from = position(moving);
        CostPosition to = destination == null ? null : position(destination);
        double rawDistance = to == null ? 0 : Math.hypot(Math.hypot(from.x() - to.x(), from.y() - to.y()), from.z() - to.z());
        boolean across = to != null && !from.dimensionId().equals(to.dimensionId());
        CostRequestInfo info = request == null ? null : new CostRequestInfo(request.getRequestId(),
                request.getRequestTime().getTime(), request.getExpireTime(), request.isSafe(), request.isIgnore());
        NativePlayer movingView = new NativePlayer(moving);
        return CostEvaluation.open(CostContextInput.builder().operationId(operationId).generationId(generationId)
                .phase(phase).teleportType(type).parameters(configuration.parameters(type)).cardSettings(configuration.cards())
                .player(movingView).payer(moving == paying ? movingView : new NativePlayer(paying))
                .requester(requester == null ? null : new NativePlayer(requester))
                .targetPlayer(targetPlayer == null ? null : new NativePlayer(targetPlayer)).request(info)
                .source(from).destination(to).rawDistance(rawDistance)
                .distance(configuration.distance(rawDistance, across))
                .cooldownSeconds(cooldownSeconds).countdownSeconds(PlayerTeleportData.getData(moving).peekTeleportCountdownSeconds(type))
                .sourceWorld(new NativeWorld(moving.getLevel()))
                .targetWorld(to == null ? null : world(server, to.dimensionId()))
                .server(new NativeServer(server)).build());
    }

    private static CostPosition position(ServerPlayer player) {
        return new CostPosition(player.level.dimension().location().toString(), player.getX(), player.getY(), player.getZ(),
                player.yRot, player.xRot, false, EnumSafeMode.NONE);
    }

    private static CostPosition position(SafeWorldCoordinate coordinate) {
        return new CostPosition(coordinate.dimensionId(), coordinate.x(), coordinate.y(), coordinate.z(), coordinate.yaw(),
                coordinate.pitch(), coordinate.safe(), coordinate.safeMode());
    }

    private static NativeWorld world(MinecraftServer server, String id) {
        ServerLevel world = server.getLevel(DimensionUtils.parse(id));
        return world == null ? null : new NativeWorld(world);
    }

    private static List<CostPlayerView> players(List<ServerPlayer> players) {
        List<CostPlayerView> result = new ArrayList<>(players.size());
        for (ServerPlayer player : players) result.add(new NativePlayer(player));
        return result;
    }

    private static final class NativePlayer implements CostPlayerView {
        private final ServerPlayer player;

        NativePlayer(ServerPlayer player) {
            this.player = player;
        }

        public UUID uuid() {
            return player.getUUID();
        }

        public String name() {
            return player.getName().getString();
        }

        public CostPosition position() {
            return FabricCostContext.position(player);
        }

        public int experiencePoints() {
            return player.totalExperience;
        }

        public int experienceLevels() {
            return player.experienceLevel;
        }

        public float health() {
            return player.getHealth();
        }

        public float maxHealth() {
            return player.getMaxHealth();
        }

        public int foodLevel() {
            return player.getFoodData().getFoodLevel();
        }

        public int teleportCards() {
            return PlayerTeleportData.getData(player).peekTeleportCard();
        }

        public boolean alive() {
            return player.isAlive();
        }

        public boolean removed() {
            return player.removed;
        }

        public boolean creative() {
            return player.isCreative();
        }

        public boolean spectator() {
            return player.isSpectator();
        }

        public <T> T nativePlayer(Class<T> type) {
            return type.cast(player);
        }

        public <T> T nativeTeleportData(Class<T> type) {
            return type.cast(PlayerTeleportData.getData(player));
        }
    }

    private static final class NativeWorld implements CostWorldView {
        private final ServerLevel world;

        NativeWorld(ServerLevel world) {
            this.world = world;
        }

        public String dimensionId() {
            return world.dimension().location().toString();
        }

        public long gameTime() {
            return world.getGameTime();
        }

        public long dayTime() {
            return world.getDayTime();
        }

        public boolean raining() {
            return world.isRaining();
        }

        public boolean thundering() {
            return world.isThundering();
        }

        public boolean chunkLoaded(int x, int z) {
            return world.getChunkSource().getChunkNow(x, z) != null;
        }

        public Optional<String> blockId(int x, int y, int z) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!Level.isInWorldBounds(pos)) return Optional.empty();
            LevelChunk chunk = world.getChunkSource().getChunkNow(x >> 4, z >> 4);
            if (chunk == null) return Optional.empty();
            return Optional.of(net.minecraft.core.Registry.BLOCK.getKey(chunk.getBlockState(pos).getBlock()).toString());
        }

        public List<CostPlayerView> players() {
            return FabricCostContext.players(world.players());
        }

        public <T> T nativeWorld(Class<T> type) {
            return type.cast(world);
        }
    }

    private static final class NativeServer implements CostServerView {
        private final MinecraftServer server;

        NativeServer(MinecraftServer server) {
            this.server = server;
        }

        public long tick() {
            return server.getTickCount();
        }

        public int onlinePlayerCount() {
            return server.getPlayerList().getPlayerCount();
        }

        public Optional<CostPlayerView> player(UUID id) {
            return Optional.ofNullable(server.getPlayerList().getPlayer(id)).map(NativePlayer::new);
        }

        public List<CostPlayerView> players() {
            return FabricCostContext.players(server.getPlayerList().getPlayers());
        }

        public Optional<CostWorldView> world(String id) {
            return Optional.ofNullable(FabricCostContext.world(server, id));
        }

        public <T> T nativeServer(Class<T> type) {
            return type.cast(server);
        }
    }
}
