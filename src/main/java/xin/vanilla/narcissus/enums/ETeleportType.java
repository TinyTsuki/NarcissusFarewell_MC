package xin.vanilla.narcissus.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum ETeleportType {
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
    DEATH,
    OTHER;

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
            default:
                return null;
        }
    }

    public static ETeleportType nullableValueOf(String name) {
        for (ETeleportType value : ETeleportType.values()) {
            if (value.name().equalsIgnoreCase(name)) return value;
        }
        return null;
    }

    public static List<String> names() {
        return Arrays.stream(ETeleportType.values()).map(Enum::name).collect(Collectors.toList());
    }
}
