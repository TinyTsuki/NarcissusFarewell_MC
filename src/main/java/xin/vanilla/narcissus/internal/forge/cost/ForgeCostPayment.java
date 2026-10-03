package xin.vanilla.narcissus.internal.forge.cost;

import com.mojang.brigadier.StringReader;
import net.minecraft.command.arguments.ItemInput;
import net.minecraft.command.arguments.ItemParser;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.api.cost.*;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.data.cost.CostPaymentPlan;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.*;
import xin.vanilla.narcissus.service.cost.CostPaymentService;

import java.util.Objects;
import java.util.function.*;

public final class ForgeCostPayment implements CostPaymentService.PaymentAccess {
    private final LongSupplier clock;
    private final Predicate<ServerPlayerEntity> connected;
    private final Function<String, TeleportRequest> requests;
    private final BiPredicate<ServerPlayerEntity, String> commands;
    private final Consumer<ServerPlayerEntity> sync;

    public ForgeCostPayment() {
        this(System::currentTimeMillis, player -> player.server != null && player.server.isSameThread()
                        && !player.hasDisconnected() && player.server.getPlayerList().getPlayer(player.getUUID()) == player,
                id -> NarcissusFarewell.getTeleportRequest().get(id), CommandUtils::executeCommand,
                PlayerTeleportData::syncPlayerData);
    }

    ForgeCostPayment(LongSupplier clock, Predicate<ServerPlayerEntity> connected, Function<String, TeleportRequest> requests,
                     BiPredicate<ServerPlayerEntity, String> commands, Consumer<ServerPlayerEntity> sync) {
        this.clock = Objects.requireNonNull(clock, "clock"); this.connected = Objects.requireNonNull(connected, "connected");
        this.requests = Objects.requireNonNull(requests, "requests"); this.commands = Objects.requireNonNull(commands, "commands");
        this.sync = Objects.requireNonNull(sync, "sync");
    }

    @Override public EnumCostFailure validate(CostContext context) {
        ServerPlayerEntity moving = context.player().nativePlayer(ServerPlayerEntity.class);
        ServerPlayerEntity paying = context.payer().nativePlayer(ServerPlayerEntity.class);
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
        if (context.request().isPresent()) {
            CostRequestInfo info = context.request().get();
            TeleportRequest request = requests.apply(info.id());
            if (request == null || request.getRequestTime() == null || request.getExpireTime() <= clock.getAsLong()
                    || info.createdAt() != request.getRequestTime().getTime() || info.expiresAt() != request.getExpireTime()
                    || request.getTeleportType() != context.teleportType() || request.getRequester() != paying
                    || !context.requester().isPresent() || !context.targetPlayer().isPresent()
                    || context.requester().get().nativePlayer(ServerPlayerEntity.class) != request.getRequester()
                    || context.targetPlayer().get().nativePlayer(ServerPlayerEntity.class) != request.getTarget()
                    || !available(request.getTarget()) || request.getTarget().server != paying.server
                    || (request.getTeleportType() == EnumTeleportType.TP_HERE ? request.getTarget() : request.getRequester()) != moving) {
                return EnumCostFailure.REQUEST_UNAVAILABLE;
            }
        }
        return EnumCostFailure.NONE;
    }

    private boolean actor(CostPlayerView view, ServerPlayerEntity moving) {
        ServerPlayerEntity player = view.nativePlayer(ServerPlayerEntity.class);
        return available(player) && player.server == moving.server && player.getUUID().equals(view.uuid());
    }

    private boolean available(ServerPlayerEntity player) {
        return player != null && connected.test(player) && player.isAlive() && !player.removed;
    }

    @Override public EnumCostFailure check(CostContext context, CostPaymentPlan plan) {
        ServerPlayerEntity player = context.payer().nativePlayer(ServerPlayerEntity.class);
        if (plan.cardAmount() > 0 && PlayerTeleportData.getData(player).peekTeleportCard() < plan.cardAmount()) {
            return EnumCostFailure.INSUFFICIENT_CARDS;
        }
        int amount = plan.resourceAmount();
        if (amount == 0 || context.costType() == EnumCostType.NONE) return EnumCostFailure.NONE;
        boolean enough;
        switch (context.costType()) {
            case EXP_POINT: enough = player.totalExperience >= amount; break;
            case EXP_LEVEL: enough = player.experienceLevel >= amount; break;
            case HEALTH: enough = Float.isFinite(player.getHealth()) && player.getHealth() > amount; break;
            case HUNGER: enough = player.getFoodData().getFoodLevel() >= amount; break;
            case ITEM:
                ItemStack required = item(context.parameters().item());
                if (required == null) return EnumCostFailure.INVALID_PAYMENT;
                enough = count(player.inventory, required) >= amount; break;
            case COMMAND:
                return context.parameters().command().trim().isEmpty() ? EnumCostFailure.INVALID_PAYMENT : EnumCostFailure.NONE;
            default: return EnumCostFailure.INVALID_PAYMENT;
        }
        return enough ? EnumCostFailure.NONE : EnumCostFailure.INSUFFICIENT_RESOURCE;
    }

    @Override public EnumCostFailure pay(CostContext context, CostPaymentPlan plan) {
        EnumCostFailure failure = validate(context);
        if (failure == EnumCostFailure.NONE) failure = check(context, plan);
        if (failure != EnumCostFailure.NONE) return failure;
        ServerPlayerEntity player = context.payer().nativePlayer(ServerPlayerEntity.class);
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        if (!data.tryConsumeTeleportCards(plan.cardAmount())) return EnumCostFailure.INSUFFICIENT_CARDS;
        boolean success = false;
        try {
            int amount = plan.resourceAmount();
            if (amount > 0) {
                switch (context.costType()) {
                    case EXP_POINT:
                    case EXP_LEVEL:
                        if (!debitExperience(player, context.costType(), amount)) return EnumCostFailure.PAYMENT_FAILED;
                        break;
                    case HEALTH: player.setHealth(player.getHealth() - amount); break;
                    case HUNGER: player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - amount); break;
                    case ITEM: remove(player.inventory, Objects.requireNonNull(item(context.parameters().item())), amount); break;
                    case COMMAND:
                        if (!commands.test(player, context.parameters().command().replace("{amount}", Integer.toString(amount)))) {
                            return EnumCostFailure.COMMAND_FAILED;
                        }
                        break;
                    case NONE: break;
                    default: return EnumCostFailure.INVALID_PAYMENT;
                }
            }
            success = true;
            return EnumCostFailure.NONE;
        } finally {
            if (!success) data.refundTeleportCards(plan.cardAmount());
        }
    }

    @Override public void afterPayment(CostContext context, CostPaymentPlan plan) {
        ServerPlayerEntity player = context.payer().nativePlayer(ServerPlayerEntity.class);
        if (context.costType() == EnumCostType.ITEM && plan.resourceAmount() > 0) {
            player.inventory.setChanged();
            if (player.inventoryMenu != null) player.inventoryMenu.broadcastChanges();
        }
        if (plan.cardAmount() > 0) {
            PlayerTeleportData data = PlayerTeleportData.getData(player);
            try { data.saveEx(); }
            catch (RuntimeException error) { data.setDirty(); throw error; }
            sync.accept(player);
        }
    }

    private static ItemStack item(String specification) {
        try {
            StringReader reader = new StringReader(specification.trim());
            ItemParser parser = new ItemParser(reader, false).parse();
            if (reader.canRead()) return null;
            ItemStack item = new ItemInput(parser.getItem(), parser.getNbt()).createItemStack(1, false);
            return item.isEmpty() ? null : item;
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException | IllegalArgumentException error) { return null; }
    }

    private static boolean debitExperience(ServerPlayerEntity player, EnumCostType type, int amount) {
        int points = player.totalExperience, levels = player.experienceLevel;
        float progress = player.experienceProgress;
        boolean success = false;
        try {
            if (type == EnumCostType.EXP_POINT) {
                player.giveExperiencePoints(-amount);
                success = player.totalExperience == points - amount;
            } else {
                player.giveExperienceLevels(-amount);
                success = player.experienceLevel == levels - amount;
            }
            return success;
        } finally {
            // Forge experience events may cancel or alter the debit; never treat that as successful payment.
            if (!success) {
                player.totalExperience = points; player.experienceLevel = levels; player.experienceProgress = progress;
            }
        }
    }

    private static boolean matches(ItemStack stack, ItemStack required) {
        return !stack.isEmpty() && stack.getItem() == required.getItem() && ItemStack.tagMatches(stack, required);
    }

    private static long count(PlayerInventory inventory, ItemStack required) {
        long count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (matches(stack, required)) count += stack.getCount();
        }
        return count;
    }

    private static void remove(PlayerInventory inventory, ItemStack required, int amount) {
        int remaining = amount;
        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (matches(stack, required)) {
                int take = Math.min(remaining, stack.getCount()); stack.shrink(take); remaining -= take;
            }
        }
        if (remaining != 0) throw new IllegalStateException("Inventory changed during payment");
    }
}
