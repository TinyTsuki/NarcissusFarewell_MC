package xin.vanilla.narcissus.internal.forge.cost;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.api.cost.*;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;

import java.util.function.BiPredicate;

final class ForgeCostExtras {
    private ForgeCostExtras() { }
    static CostResourceAccess cards(ServerPlayer player) {
        return new CostResourceAccess() {
            private final PlayerTeleportData data = PlayerTeleportData.getData(player);
            public String snapshot() { return json(player, "cards", data.peekTeleportCard()).toString(); }
            public CostFailure check(ResourceCost cost, int units, String before) {
                return value(before, "cards") >= Math.multiplyExact(cost.amount(), units)
                        ? CostFailure.NONE : CostFailure.of("narcissus:insufficient_cards");
            }
            public String after(ResourceCost cost, int units, String before) {
                return json(player, "cards", value(before, "cards") - Math.multiplyExact(cost.amount(), units)).toString();
            }
            public CostFailure debit(String before, String after) {
                if (!before.equals(snapshot())) return CostFailure.of("narcissus:insufficient_cards");
                int amount = value(before, "cards") - value(after, "cards");
                return amount >= 0 && data.tryConsumeTeleportCards(amount) ? CostFailure.NONE : CostFailure.of("narcissus:insufficient_cards");
            }
            public CostFailure restore(String before, String after) {
                if (!after.equals(snapshot())) return CostFailure.UNCERTAIN;
                int amount = value(before, "cards") - value(after, "cards");
                if (amount < 0) return CostFailure.UNCERTAIN;
                data.refundTeleportCards(amount);
                return before.equals(snapshot()) ? CostFailure.NONE : CostFailure.UNCERTAIN;
            }
            public boolean reversible() { return true; }
            public CostFailure compensateReservation(String before, String after) {
                JsonObject original = new JsonParser().parse(before).getAsJsonObject();
                if (!player.getUUID().toString().equals(original.get("payer").getAsString())) return CostFailure.UNCERTAIN;
                int reserved = value(before, "cards") - value(after, "cards");
                int current = data.peekTeleportCard();
                if (reserved < 0 || current < 0 || current > Integer.MAX_VALUE - reserved) return CostFailure.UNCERTAIN;
                data.refundTeleportCards(reserved);
                return data.peekTeleportCard() == current + reserved ? CostFailure.NONE : CostFailure.UNCERTAIN;
            }
        };
    }

    static CostResourceAccess command(ServerPlayer player, String command, BiPredicate<ServerPlayer, String> executor) {
        return new CostResourceAccess() {
            private boolean executed;
            public String snapshot() {
                JsonObject json = json(player, "runs", executed ? 1 : 0);
                json.addProperty("command", command);
                return json.toString();
            }
            public CostFailure check(ResourceCost cost, int units, String before) {
                return command.trim().isEmpty() || !command.equals(cost.specification()) || value(before, "runs") != 0
                        ? CostFailure.INVALID_PAYMENT : CostFailure.NONE;
            }
            public String after(ResourceCost cost, int units, String before) {
                JsonObject json = new JsonParser().parse(before).getAsJsonObject();
                json.addProperty("runs", 1);
                return json.toString();
            }
            public CostFailure debit(String before, String after) {
                if (!before.equals(snapshot()) || executed) return CostFailure.INVALID_OPERATION;
                if (!executor.test(player, command)) return CostFailure.of("narcissus:command_failed");
                executed = true;
                return CostFailure.NONE;
            }
            public CostFailure restore(String before, String after) { return CostFailure.UNCERTAIN; }
            public boolean reversible() { return false; }
        };
    }

    private static JsonObject json(ServerPlayer player, String field, int amount) {
        JsonObject json = new JsonObject();
        json.addProperty("schema", 1);
        json.addProperty("payer", player.getUUID().toString());
        json.addProperty(field, amount);
        return json;
    }
    private static int value(String snapshot, String field) {
        return new JsonParser().parse(snapshot).getAsJsonObject().get(field).getAsInt();
    }
}
