package xin.vanilla.narcissus.util;

import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.common.util.SafeExpressionEvaluator;
import xin.vanilla.narcissus.NarcissusLang;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.TeleportCost;
import xin.vanilla.narcissus.data.client.ClientCostConfig;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumCostType;
import xin.vanilla.narcissus.enums.EnumTeleportType;

import java.util.HashMap;
import java.util.Map;


@OnlyIn(Dist.CLIENT)
public final class ClientCostCalculator {

    private ClientCostCalculator() {
    }

    /**
     * 计算并格式化传送代价显示
     */
    public static String formatCostDisplay(ClientPlayerEntity player, SafeWorldCoordinate target, EnumTeleportType type) {
        if (player == null || target == null) return "";
        TeleportCost cost = ClientCostConfig.getCost(type);
        if (cost == null || cost.getType() == EnumCostType.NONE) {
            return NarcissusLang.get().translate(EnumI18nType.WORD, "cost_free");
        }
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        double distance = NarcissusUtils.calculateDistance(new SafeWorldCoordinate(player), target);
        double adjustedDistance;
        if (player.level.dimension() == target.dimension()) {
            int limit = ClientCostConfig.getDistanceLimit();
            adjustedDistance = limit == 0 ? distance : Math.min(limit, distance);
        } else {
            adjustedDistance = ClientCostConfig.getDistanceAcrossDimension();
        }
        Map<String, Object> vars = new HashMap<>();
        vars.put("distance", adjustedDistance);
        vars.put("num", (double) cost.getNum());
        vars.put("rate", cost.getRate());
        double need;
        try {
            need = new SafeExpressionEvaluator(cost.getExp()).evaluateDouble(vars);
        } catch (Exception e) {
            need = cost.getNum() * adjustedDistance * cost.getRate();
        }
        need = Math.min(need, cost.getUpper());
        need = Math.max(need, cost.getLower());
        int cardNeed = getTeleportCardNeed(need);
        int costNeed = getTeleportCostNeed(data, cardNeed, (int) Math.ceil(need));
        String lang = NarcissusLang.getClientLanguage();
        if (costNeed < 0) {
            return NarcissusLang.get().translate(EnumI18nType.WORD, "teleport_card", lang) + " x" + cardNeed;
        }
        switch (cost.getType()) {
            case EXP_POINT:
                return costNeed + " " + NarcissusLang.get().translate(EnumI18nType.WORD, "exp_point", lang);
            case EXP_LEVEL:
                return costNeed + " " + NarcissusLang.get().translate(EnumI18nType.WORD, "exp_level", lang);
            case HEALTH:
                return costNeed + " " + NarcissusLang.get().translate(EnumI18nType.WORD, "health", lang);
            case HUNGER:
                return costNeed + " " + NarcissusLang.get().translate(EnumI18nType.WORD, "hunger", lang);
            case ITEM:
                return costNeed + " " + NarcissusLang.get().translate(EnumI18nType.WORD, "item", lang);
            case COMMAND:
                return NarcissusLang.get().translate(EnumI18nType.WORD, "cost_command", lang);
            default:
                return String.valueOf(costNeed);
        }
    }

    private static int getTeleportCardNeed(double need) {
        int ceil = (int) Math.ceil(need);
        if (!CommonConfig.get().base().teleportCard().teleportCard()) return 0;
        switch (CommonConfig.get().base().teleportCard().teleportCardType()) {
            case LIKE_COST:
            case REFUND_COST:
            case REFUND_COST_AND_COOLDOWN:
                return ceil;
            default:
                return 1;
        }
    }

    private static int getTeleportCostNeed(PlayerTeleportData data, int card, int need) {
        if (!CommonConfig.get().base().teleportCard().teleportCard()) return need;
        switch (CommonConfig.get().base().teleportCard().teleportCardType()) {
            case NONE:
                return data.getTeleportCard() >= card ? need : -1;
            case LIKE_COST:
                return data.getTeleportCard() >= card ? card : -1;
            case REFUND_COOLDOWN:
                return need;
            case REFUND_ALL_COST:
            case REFUND_ALL_COST_AND_COOLDOWN:
                return data.getTeleportCard() >= card ? 0 : need;
            case REFUND_COST:
            case REFUND_COST_AND_COOLDOWN:
            default:
                return Math.max(0, card - data.getTeleportCard());
        }
    }
}
