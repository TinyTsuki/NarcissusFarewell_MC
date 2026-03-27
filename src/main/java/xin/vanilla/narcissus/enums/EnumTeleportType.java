package xin.vanilla.narcissus.enums;

import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.IEnumDescribable;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;
import xin.vanilla.narcissus.NarcissusComponent;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum EnumTeleportType implements IEnumDescribable {
    TP_COORDINATE,
    TP_STRUCTURE,
    TP_ASK,
    TP_HERE,
    TP_RANDOM,
    TP_SPAWN,
    TP_WORLD_SPAWN,
    TP_TOP,
    TP_BOTTOM,
    TP_UP,
    TP_DOWN,
    TP_VIEW,
    TP_HOME,
    TP_STAGE,
    TP_BACK,
    TP_GRAVE,
    DEATH,
    OTHER,
    ;

    private static final List<EnumTeleportType> COUNTDOWN_CONFIGURABLE = Collections.unmodifiableList(Arrays.asList(
            TP_COORDINATE, TP_STRUCTURE, TP_ASK, TP_HERE, TP_RANDOM, TP_SPAWN, TP_WORLD_SPAWN,
            TP_TOP, TP_BOTTOM, TP_UP, TP_DOWN, TP_VIEW, TP_HOME, TP_STAGE, TP_BACK, TP_GRAVE
    ));

    /**
     * 与传送指令对应、可在玩家数据中配置传送倒计时（秒）的类型。
     */
    public static List<EnumTeleportType> countdownConfigurableTypes() {
        return COUNTDOWN_CONFIGURABLE;
    }

    public EnumCommandType toCommandType() {
        switch (this) {
            case TP_COORDINATE:
                return EnumCommandType.TP_COORDINATE;
            case TP_STRUCTURE:
                return EnumCommandType.TP_STRUCTURE;
            case TP_ASK:
                return EnumCommandType.TP_ASK;
            case TP_HERE:
                return EnumCommandType.TP_HERE;
            case TP_RANDOM:
                return EnumCommandType.TP_RANDOM;
            case TP_SPAWN:
                return EnumCommandType.TP_SPAWN;
            case TP_WORLD_SPAWN:
                return EnumCommandType.TP_WORLD_SPAWN;
            case TP_TOP:
                return EnumCommandType.TP_TOP;
            case TP_BOTTOM:
                return EnumCommandType.TP_BOTTOM;
            case TP_UP:
                return EnumCommandType.TP_UP;
            case TP_DOWN:
                return EnumCommandType.TP_DOWN;
            case TP_VIEW:
                return EnumCommandType.TP_VIEW;
            case TP_HOME:
                return EnumCommandType.TP_HOME;
            case TP_STAGE:
                return EnumCommandType.TP_STAGE;
            case TP_BACK:
                return EnumCommandType.TP_BACK;
            case TP_GRAVE:
                return EnumCommandType.TP_GRAVE;
            default:
                return null;
        }
    }

    public static EnumTeleportType valueOfEx(Object obj) {
        if (obj instanceof EnumTeleportType) return (EnumTeleportType) obj;
        if (obj instanceof String) {
            for (EnumTeleportType value : values()) {
                if (value.name().equalsIgnoreCase((String) obj)) {
                    return value;
                }
            }
        }
        return null;
    }

    public static EnumTeleportType valueOfDefault(Object obj) {
        EnumTeleportType value = valueOfEx(obj);
        return value == null ? OTHER : value;
    }

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(NarcissusComponent.get(), this);
    }
}
