package xin.vanilla.narcissus.config.preset;

import xin.vanilla.banira.common.config.ConfigEntryDescriptor;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.narcissus.enums.EnumCostType;
import java.util.ArrayList;
import java.util.List;

/** Business presets; configuration access is generated from the declarations. */
public final class CommonConfigPresets {
    private CommonConfigPresets() {}

    private static void applyDefaults(ConfigHolder holder) {
        // Only registered entries belong to this preset, not unrelated backend values.
        for (ConfigEntryDescriptor entry : holder.getDescriptors()) {
            Object value = entry.getDefaultValue();
            holder.set(entry.getPath(), value instanceof List ? new ArrayList<>((List<?>) value) : value);
        }
    }

    public static void resetConfig(ConfigHolder h) {
        if (h == null) {
            return;
        }
        applyDefaults(h);
        h.save();
    }

    public static void resetConfigWithMode1(ConfigHolder h) {
        if (h == null) {
            return;
        }
        applyDefaults(h);
        h.set("base.teleportLimit.teleportBackSkipType", new ArrayList<>());
        h.set("command.tpHome.commandTpHome", "home");
        h.set("command.tpHome.commandSetHome", "home_set");
        h.set("command.tpHome.commandDelHome", "home_del");
        h.set("command.tpHome.commandGetHome", "home_get");
        h.set("command.tpStage.commandTpStage", "warp");
        h.set("command.tpStage.commandSetStage", "warp_set");
        h.set("command.tpStage.commandDelStage", "warp_del");
        h.set("command.tpStage.commandGetStage", "warp_get");
        h.set("command.commandTpTop", "top");
        h.set("command.commandTpUp", "up");
        h.set("command.commandTpDown", "down");
        h.set("command.commandTpBottom", "bottom");
        h.save();
    }

    public static void resetConfigWithMode2(ConfigHolder h) {
        if (h == null) {
            return;
        }
        resetConfigWithMode1(h);
        h.set("featureSwitch.switchFeed", false);
        h.set("featureSwitch.switchTpStructure", false);
        h.set("featureSwitch.switchTpRandom", false);
        h.set("featureSwitch.switchTpSpawn", false);
        h.set("featureSwitch.switchTpWorldSpawn", false);
        h.set("featureSwitch.switchTpBottom", false);
        h.set("featureSwitch.switchTpDown", false);
        h.set("featureSwitch.switchTpUp", false);
        h.set("featureSwitch.switchTpView", false);
        h.save();
    }

    public static void resetConfigWithMode3(ConfigHolder h) {
        if (h == null) {
            return;
        }
        applyDefaults(h);
        h.set("concise.tpAsk.conciseTpAskCancel", false);
        h.set("concise.tpHere.conciseTpHereCancel", false);
        h.set("concise.conciseTpRandom", false);
        h.set("concise.conciseTpSpawn", false);
        h.set("concise.conciseTpWorldSpawn", false);
        h.set("concise.conciseTpTop", false);
        h.set("concise.conciseTpUp", false);
        h.set("concise.conciseTpBottom", false);
        h.set("concise.conciseTpDown", false);
        h.set("concise.conciseTpView", false);
        String[] subs = {"tpCoordinate", "tpStructure", "tpAsk", "tpHere", "tpRandom", "tpSpawn", "tpWorldSpawn",
                "tpTop", "tpBottom", "tpUp", "tpDown", "tpView", "tpHome", "tpStage", "tpBack", "tpGrave"};
        String[] stems = {"costTpCoordinate", "costTpStructure", "costTpAsk", "costTpHere", "costTpRandom", "costTpSpawn",
                "costTpWorldSpawn", "costTpTop", "costTpBottom", "costTpUp", "costTpDown", "costTpView", "costTpHome",
                "costTpStage", "costTpBack", "costTpGrave"};
        for (int i = 0; i < subs.length; i++) {
            h.set("cost." + subs[i] + "." + stems[i] + "Type", EnumCostType.EXP_POINT);
        }
        h.set("cost.tpGrave.costTpGraveType", EnumCostType.HUNGER);
        h.save();
    }

}
