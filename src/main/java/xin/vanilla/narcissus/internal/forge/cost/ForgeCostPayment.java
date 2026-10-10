package xin.vanilla.narcissus.internal.forge.cost;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.api.cost.CostContext;
import xin.vanilla.narcissus.api.cost.CostPhase;
import xin.vanilla.narcissus.api.cost.CostPlayerView;
import xin.vanilla.narcissus.api.cost.CostRequestInfo;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.data.cost.CostPaymentPlan;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumCostFailure;
import xin.vanilla.narcissus.enums.EnumCostType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.service.cost.CostPaymentService;

import java.util.Objects;
import java.util.ArrayList;
import java.util.List;
import xin.vanilla.banira.api.BaniraCosts;
import xin.vanilla.banira.api.cost.*;
import xin.vanilla.narcissus.service.cost.NarcissusPaymentPolicy;
import net.minecraft.core.Registry;
import java.util.function.*;

public final class ForgeCostPayment implements CostPaymentService.PaymentAccess {
    private final LongSupplier clock;
    private final Predicate<ServerPlayer> connected;
    private final Function<String, TeleportRequest> requests;
    private final BiPredicate<ServerPlayer, String> commands;
    private final Consumer<ServerPlayer> sync;

    public ForgeCostPayment() {
        this(System::currentTimeMillis, player -> player.server != null && player.server.isSameThread()
                        && !player.hasDisconnected() && player.server.getPlayerList().getPlayer(player.getUUID()) == player,
                id -> NarcissusFarewell.getTeleportRequest().get(id), CommandUtils::executeCommand,
                PlayerTeleportData::syncPlayerData);
    }

    ForgeCostPayment(LongSupplier clock, Predicate<ServerPlayer> connected, Function<String, TeleportRequest> requests,
                     BiPredicate<ServerPlayer, String> commands, Consumer<ServerPlayer> sync) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.connected = Objects.requireNonNull(connected, "connected");
        this.requests = Objects.requireNonNull(requests, "requests");
        this.commands = Objects.requireNonNull(commands, "commands");
        this.sync = Objects.requireNonNull(sync, "sync");
    }

    @Override
    public EnumCostFailure validate(CostContext context) {
        ServerPlayer moving = context.player().nativePlayer(ServerPlayer.class);
        ServerPlayer paying = context.payer().nativePlayer(ServerPlayer.class);
        if (!available(moving) || !available(paying) || moving.server != paying.server
                || moving.server != context.nativeServer(net.minecraft.server.MinecraftServer.class)
                || !moving.getUUID().equals(context.player().uuid()) || !paying.getUUID().equals(context.payer().uuid())
                || context.requester().filter(view -> !actor(view, moving)).isPresent()
                || context.targetPlayer().filter(view -> !actor(view, moving)).isPresent()) {
            return EnumCostFailure.ACTOR_UNAVAILABLE;
        }
        if (!context.destination().isPresent() || !context.targetWorld().isPresent()
                || !context.destination().get().dimensionId().equals(context.targetWorld().get().dimensionId())) {
            return EnumCostFailure.UNKNOWN_TARGET;
        }
        if (context.targetPlayer().isPresent()) {
            ServerPlayer endpoint = context.teleportType() == EnumTeleportType.TP_HERE ? paying
                    : context.targetPlayer().get().nativePlayer(ServerPlayer.class);
            if (!context.destination().get().dimensionId().equals(endpoint.getLevel().dimension().location().toString())) {
                return EnumCostFailure.UNKNOWN_TARGET;
            }
        }
        if (context.request().isPresent()) {
            CostRequestInfo info = context.request().get();
            TeleportRequest request = requests.apply(info.id());
            // A prospective request has not been registered yet; CHECK never authorizes a payment.
            if (request == null && context.phase() == CostPhase.CHECK && info.expiresAt() > clock.getAsLong()
                    && context.requester().isPresent() && context.targetPlayer().isPresent()) {
                ServerPlayer requester = context.requester().get().nativePlayer(ServerPlayer.class);
                ServerPlayer target = context.targetPlayer().get().nativePlayer(ServerPlayer.class);
                if (requester == paying && (context.teleportType() == EnumTeleportType.TP_HERE ? target : requester) == moving
                        && (context.teleportType() == EnumTeleportType.TP_ASK || context.teleportType() == EnumTeleportType.TP_HERE)) {
                    return EnumCostFailure.NONE;
                }
            }
            if (request == null || request.getRequestTime() == null || request.getExpireTime() <= clock.getAsLong()
                    || info.createdAt() != request.getRequestTime().getTime() || info.expiresAt() != request.getExpireTime()
                    || request.getTeleportType() != context.teleportType() || request.getRequester() != paying
                    || !context.requester().isPresent() || !context.targetPlayer().isPresent()
                    || context.requester().get().nativePlayer(ServerPlayer.class) != request.getRequester()
                    || context.targetPlayer().get().nativePlayer(ServerPlayer.class) != request.getTarget()
                    || !available(request.getTarget()) || request.getTarget().server != paying.server
                    || (request.getTeleportType() == EnumTeleportType.TP_HERE ? request.getTarget() : request.getRequester()) != moving) {
                return EnumCostFailure.REQUEST_UNAVAILABLE;
            }
        }
        return EnumCostFailure.NONE;
    }

    private boolean actor(CostPlayerView view, ServerPlayer moving) {
        ServerPlayer player = view.nativePlayer(ServerPlayer.class);
        return available(player) && player.server == moving.server && player.getUUID().equals(view.uuid());
    }

    private boolean available(ServerPlayer player) {
        return player != null && connected.test(player) && player.isAlive() && !player.isRemoved();
    }

    @Override
    public EnumCostFailure check(CostContext context, CostPaymentPlan plan) {
        return NarcissusPaymentPolicy.failure(prepare(context, plan).plan().failure());
    }

    @Override
    public CostPaymentService.PreparedPayment prepare(CostContext context, CostPaymentPlan plan) {
        ServerPlayer player = context.payer().nativePlayer(ServerPlayer.class);
        CostAccess resources = BaniraCosts.access(player, InventoryScope.ALL)
                .withResource("narcissus:teleport_card", ForgeCostExtras.cards(player));
        List<CostRequirement> requirements = new ArrayList<>();
        if (plan.cardAmount() > 0) requirements.add(CostRequirement.leaf(new ResourceCost(
                "narcissus:teleport_card", plan.cardAmount(), "")));
        if (plan.resourceAmount() > 0 && context.costType() != EnumCostType.NONE) {
            if (context.costType() == EnumCostType.COMMAND) {
                String command = context.parameters().command().replace("{amount}", Integer.toString(plan.resourceAmount()));
                resources = resources.withResource("narcissus:command", ForgeCostExtras.command(player, command, commands));
                requirements.add(CostRequirement.leaf(new ResourceCost("narcissus:command", plan.resourceAmount(), command)));
            } else requirements.add(CostRequirement.leaf(resource(context, plan.resourceAmount())));
        }
        CostAccess access = resources;
        xin.vanilla.banira.api.cost.CostPaymentPlan shared = access.preview(CostRequirement.all(requirements), 1);
        return new CostPaymentService.PreparedPayment() {
            public xin.vanilla.banira.api.cost.CostPaymentPlan plan() { return shared; }
            public EnumCostFailure pay() {
                EnumCostFailure validity = validate(context);
                return validity == EnumCostFailure.NONE ? NarcissusPaymentPolicy.failure(access.debit(shared)) : validity;
            }
            public List<xin.vanilla.banira.api.cost.CostPaymentPlan.ResourceSnapshot> receipt() { return access.receipt(shared).snapshots(); }
            public void afterPayment() { ForgeCostPayment.this.afterPayment(context, plan.paid()); }
        };
    }

    private static ResourceCost resource(CostContext context, int amount) {
        String id;
        switch (context.costType()) {
            case EXP_POINT: id = "banira:xp_point"; break;
            case EXP_LEVEL: id = "banira:xp_level"; break;
            case HEALTH: id = "banira:health"; break;
            case HUNGER: id = "banira:hunger"; break;
            case ITEM: id = "banira:item"; break;
            default: throw new IllegalArgumentException("Unsupported teleport resource");
        }
        return new ResourceCost(id, amount, context.costType() == EnumCostType.ITEM ? context.parameters().item() : "");
    }

    @Override
    public EnumCostFailure pay(CostContext context, CostPaymentPlan plan) {
        CostPaymentService.PreparedPayment prepared = prepare(context, plan);
        return prepared.plan().isSuccess() ? prepared.pay() : NarcissusPaymentPolicy.failure(prepared.plan().failure());
    }

    @Override
    public void afterPayment(CostContext context, CostPaymentPlan plan) {
        ServerPlayer player = context.payer().nativePlayer(ServerPlayer.class);
        if (context.costType() == EnumCostType.ITEM && plan.resourceAmount() > 0) {
            player.getInventory().setChanged();
            if (player.inventoryMenu != null) player.inventoryMenu.broadcastChanges();
        }
        if (plan.cardAmount() > 0) {
            PlayerTeleportData data = PlayerTeleportData.getData(player);
            try {
                data.saveEx();
            } catch (RuntimeException error) {
                data.setDirty();
                throw error;
            }
            sync.accept(player);
        }
    }

    public static boolean hasSupportItemAfterPayment(CostContext context, CostPaymentPlan plan, ItemStack support) {
        ServerPlayer moving = context.player().nativePlayer(ServerPlayer.class);
        List<CostRequirement> requirements = new ArrayList<>();
        if (context.payer().nativePlayer(ServerPlayer.class) == moving && context.costType() == EnumCostType.ITEM
                && plan.resourceAmount() > 0) requirements.add(CostRequirement.leaf(resource(context, plan.resourceAmount())));
        String specification = Registry.ITEM.getKey(support.getItem()).toString()
                + (support.hasTag() ? support.getTag().toString() : "");
        requirements.add(CostRequirement.leaf(new ResourceCost("banira:item", 1, specification)));
        return BaniraCosts.access(moving, InventoryScope.ALL).preview(CostRequirement.all(requirements), 1).isSuccess();
    }
}
