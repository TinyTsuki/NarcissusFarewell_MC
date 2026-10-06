package xin.vanilla.narcissus.internal.server;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.api.BaniraConfigs;
import xin.vanilla.banira.api.BaniraDataPaths;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.api.cost.CostContext;
import xin.vanilla.narcissus.api.cost.CostPhase;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.config.CommonCostConfiguration;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.data.cost.CostConfiguration;
import xin.vanilla.narcissus.data.cost.CostOperation;
import xin.vanilla.narcissus.data.cost.CostPaymentPlan;
import xin.vanilla.narcissus.data.cost.CostQuoteRequest;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumCostFailure;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.internal.forge.cost.ForgeCostContext;
import xin.vanilla.narcissus.internal.forge.cost.ForgeCostPayment;
import xin.vanilla.narcissus.internal.forge.cost.ForgeCostQuoteAccess;
import xin.vanilla.narcissus.internal.server.cost.CostEvaluation;
import xin.vanilla.narcissus.network.packet.CostCapabilitiesToClient;
import xin.vanilla.narcissus.network.packet.CostQuoteToClient;
import xin.vanilla.narcissus.service.cost.CostPaymentService;
import xin.vanilla.narcissus.service.cost.CostQuoteService;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;

/**
 * One server lifecycle, one configuration generation and one payment per teleport.
 */
public final class NarcissusCostService implements AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger();
    private static NarcissusCostService current;
    private final MinecraftServer server;
    private final CommonCostConfiguration config;
    private final ExecutorService worker;
    private final NarcissusCostRuntime runtime;
    private final ForgeCostPayment nativePayment = new ForgeCostPayment();
    private final CostPaymentService payment;
    private final CostQuoteService quotes;
    private final Map<UUID, UUID> connections = new HashMap<>();
    private final Map<UUID, Ticket> pending = new HashMap<>();
    private long sequence, advertised;

    private NarcissusCostService(MinecraftServer server) {
        this.server = server;
        ConfigHolder holder = BaniraConfigs.holder(CommonConfig.class);
        config = new CommonCostConfiguration(holder);
        worker = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Narcissus cost sources");
            t.setDaemon(true);
            return t;
        });
        runtime = new NarcissusCostRuntime(BaniraDataPaths.configPath().resolve("narcissus_farewell/cost/sources"),
                config, worker, server::execute, error -> LOGGER.error("Narcissus cost reload failed", error));
        CostPaymentService.PaymentAccess guarded = new CostPaymentService.PaymentAccess() {
            public EnumCostFailure validate(CostContext context) {
                EnumCostFailure failure = nativePayment.validate(context);
                if (failure != EnumCostFailure.NONE) return failure;
                ServerPlayer payer = context.payer().nativePlayer(ServerPlayer.class);
                ServerPlayer moving = context.player().nativePlayer(ServerPlayer.class);
                EnumCommandType command = context.teleportType().toCommandType();
                if (command != null && (!NarcissusUtils.isCommandEnabled(command)
                        || !NarcissusUtils.hasCommandPermission(payer.createCommandSourceStack(), command)))
                    return EnumCostFailure.INVALID_OPERATION;
                if (context.crossDimension() && (!CommonConfig.get().base().teleportLimit().teleportAcrossDimension()
                        || !NarcissusUtils.isTeleportTypeAcrossDimensionEnabled(payer, context.teleportType())))
                    return EnumCostFailure.INVALID_OPERATION;
                if (CommonConfig.get().base().teleportTogether().tpWithEnemy() && NarcissusUtils.isTargetedByHostile(moving))
                    return EnumCostFailure.INVALID_OPERATION;
                if (context.phase() != CostPhase.CHECK && context.request().isPresent() && !PlayerTeleportData.getData(
                        context.targetPlayer().get().nativePlayer(ServerPlayer.class)).acceptsTeleportFrom(payer.getUUID()))
                    return EnumCostFailure.INVALID_OPERATION;
                Ticket ticket = pending.get(moving.getUUID());
                if (context.phase() == CostPhase.COMMIT && (ticket == null || ticket.id != context.operationId()
                        || !ticket.targetValid.getAsBoolean())) return EnumCostFailure.UNKNOWN_TARGET;
                return EnumCostFailure.NONE;
            }

            public EnumCostFailure check(CostContext context, CostPaymentPlan plan) {
                EnumCostFailure failure = nativePayment.check(context, plan);
                Ticket ticket = pending.get(context.player().uuid());
                if (failure == EnumCostFailure.NONE && context.phase() == CostPhase.COMMIT && ticket != null
                        && ticket.supportItem != null && !ForgeCostPayment.hasSupportItemAfterPayment(context, plan, ticket.supportItem)) {
                    return EnumCostFailure.INSUFFICIENT_RESOURCE;
                }
                return failure;
            }

            public EnumCostFailure pay(CostContext context, CostPaymentPlan plan) {
                EnumCostFailure failure = validate(context);
                if (failure == EnumCostFailure.NONE) failure = check(context, plan);
                return failure == EnumCostFailure.NONE ? nativePayment.pay(context, plan) : failure;
            }

            public void afterPayment(CostContext context, CostPaymentPlan plan) {
                nativePayment.afterPayment(context, plan);
            }
        };
        payment = new CostPaymentService(runtime::calculate, guarded);
        quotes = new CostQuoteService(System::nanoTime, runtime::generationId,
                new ForgeCostQuoteAccess(() -> server, config::selection, NarcissusUtils::getTeleportCoolDown), runtime::calculate, guarded);
        runtime.watch(holder);
        try {
            reload();
        } catch (RuntimeException error) {
            LOGGER.error("Initial teleport cost configuration unavailable", error);
        }
    }

    public static void start(MinecraftServer server) {
        if (current != null) current.close();
        current = null;
        current = new NarcissusCostService(server);
    }

    public static NarcissusCostService get() {
        return current;
    }

    public static void stop() {
        if (current != null) {
            current.close();
            current = null;
        }
    }

    public CompletableFuture<Boolean> reload() {
        return runtime.prepare(config.snapshot());
    }

    public void tick() {
        long generation = runtime.generationId();
        if (generation <= 0 || generation == advertised) return;
        advertised = generation;
        for (UUID id : new ArrayList<>(connections.keySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) sendCapabilities(player, connections.get(id));
            else disconnect(id);
        }
    }

    public void connect(ServerPlayer player) {
        UUID session = connections.computeIfAbsent(player.getUUID(), id -> UUID.randomUUID());
        quotes.openConnection(player.getUUID(), session);
        sendCapabilities(player, session);
    }

    private void sendCapabilities(ServerPlayer player, UUID session) {
        long generation = runtime.generationId();
        if (generation > 0) PacketUtils.sendPacketToPlayer(new CostCapabilitiesToClient(session, generation), player);
    }

    public void disconnect(UUID player) {
        connections.remove(player);
        quotes.closeConnection(player);
        Ticket ticket = pending.remove(player);
        if (ticket != null) ticket.cancel();
        for (Ticket other : new ArrayList<>(pending.values()))
            if (other.payer.equals(player) || Objects.equals(other.target, player)) other.cancel();
    }

    public void quote(ServerPlayer player, CostQuoteRequest request) {
        if (connections.containsKey(player.getUUID()))
            PacketUtils.sendPacketToPlayer(new CostQuoteToClient(quotes.quote(player.getUUID(), request)), player);
    }

    public CostPaymentPlan checkRequest(TeleportRequest request) {
        ServerPlayer moving = request.getTeleportType() == EnumTeleportType.TP_HERE ? request.getTarget() : request.getRequester();
        ServerPlayer endpoint = request.getTeleportType() == EnumTeleportType.TP_HERE ? request.getRequester() : request.getTarget();
        try (CostEvaluation evaluation = ForgeCostContext.open(++sequence, Math.max(1, runtime.generationId()), CostPhase.CHECK,
                request.getTeleportType(), config.selection(request.getTeleportType()), moving, request.getRequester(),
                request.getRequester(), request.getTarget(), new SafeWorldCoordinate(endpoint), request,
                NarcissusUtils.getTeleportCoolDown(request.getRequester(), request.getTeleportType(), request))) {
            return payment.plan(runtime.calculate(evaluation.context()), evaluation.context());
        } catch (RuntimeException failure) {
            return CostPaymentPlan.failed(EnumCostFailure.CONFIGURATION_UNAVAILABLE);
        }
    }

    public Ticket begin(ServerPlayer moving, ServerPlayer payer, ServerPlayer target,
                        TeleportRequest request, EnumTeleportType type) {
        if (!server.isSameThread() || moving.server != server || payer.server != server)
            throw new IllegalStateException("Teleport requires owner thread");
        if (request != null && pending.values().stream().anyMatch(t -> t.request == request && !t.cancelled))
            return null;
        CostConfiguration selection = config.selection(type);
        Ticket ticket = new Ticket(++sequence, Math.max(1, runtime.generationId()), selection, moving.getUUID(), payer.getUUID(),
                target == null ? null : target.getUUID(), request, type);
        Ticket old = pending.put(ticket.moving, ticket);
        if (old != null) old.cancel();
        return ticket;
    }

    public final class Ticket {
        private final long id, generation;
        private final CostConfiguration selection;
        private final UUID moving, payer, target;
        private final TeleportRequest request;
        private final EnumTeleportType type;
        private boolean cancelled;
        private CostOperation operation;
        private SafeWorldCoordinate destination;
        private ItemStack supportItem;
        private BooleanSupplier targetValid = () -> true;

        private Ticket(long id, long generation, CostConfiguration selection, UUID moving, UUID payer, UUID target, TeleportRequest request, EnumTeleportType type) {
            this.id = id;
            this.generation = generation;
            this.selection = selection;
            this.moving = moving;
            this.payer = payer;
            this.target = target;
            this.request = request;
            this.type = type;
        }

        public boolean live() {
            return !cancelled && pending.get(moving) == this;
        }

        public void targetGuard(BooleanSupplier guard) {
            targetValid = Objects.requireNonNull(guard);
        }

        public void requireSupportItem(ItemStack item) {
            supportItem = item.copy();
        }

        public void cancel() {
            if (!server.isSameThread()) throw new IllegalStateException("Teleport cancellation requires owner thread");
            cancelled = true;
            pending.remove(moving, this);
            if (operation != null) operation.cancel();
        }

        public boolean resolve(SafeWorldCoordinate coordinate) {
            if (!live()) return false;
            destination = coordinate.clone();
            destination.y(Math.floor(coordinate.y()) + .1);
            try (CostEvaluation evaluation = open(selection, generation, CostPhase.PREVIEW)) {
                operation = CostOperation.create(evaluation.context());
                return true;
            } catch (RuntimeException error) {
                cancel();
                return false;
            }
        }

        private CostEvaluation open(CostConfiguration cfg, long gen, CostPhase phase) {
            ServerPlayer actor = server.getPlayerList().getPlayer(moving), paying = server.getPlayerList().getPlayer(payer);
            ServerPlayer other = target == null ? null : server.getPlayerList().getPlayer(target);
            if (actor == null || paying == null || target != null && other == null)
                throw new IllegalStateException("Teleport actor disconnected");
            return ForgeCostContext.open(id, gen, phase, type, cfg, actor, paying, request == null ? null : paying,
                    other, destination, request, NarcissusUtils.getTeleportCoolDown(paying, type, request));
        }

        public SafeWorldCoordinate destination() {
            return destination.clone();
        }

        public CostPaymentPlan commit() {
            if (!live() || operation == null) return CostPaymentPlan.failed(EnumCostFailure.INVALID_OPERATION);
            CostPaymentPlan result;
            try (CostEvaluation evaluation = open(config.selection(type), Math.max(1, runtime.generationId()), CostPhase.COMMIT)) {
                result = payment.commit(operation, evaluation.context());
            } catch (RuntimeException error) {
                LOGGER.error("Teleport payment unavailable", error);
                result = CostPaymentPlan.failed(EnumCostFailure.CONFIGURATION_UNAVAILABLE);
            }
            cancelled = true;
            pending.remove(moving, this);
            return result;
        }

        public void completed() {
            if (request != null) NarcissusFarewell.getTeleportRequest().remove(request.getRequestId(), request);
        }
    }

    @Override
    public void close() {
        for (Ticket ticket : new ArrayList<>(pending.values())) ticket.cancel();
        connections.clear();
        quotes.clear();
        runtime.close();
        worker.shutdownNow();
    }
}
