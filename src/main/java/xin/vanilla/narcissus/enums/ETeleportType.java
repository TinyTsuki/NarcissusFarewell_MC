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
        return switch (this) {
            case TP_COORDINATE -> EnumCommandType.TP_COORDINATE;
            case TP_STRUCTURE -> EnumCommandType.TP_STRUCTURE;
            case TP_ASK -> EnumCommandType.TP_ASK;
            case TP_HERE -> EnumCommandType.TP_HERE;
            case TP_RANDOM -> EnumCommandType.TP_RANDOM;
            case TP_SPAWN -> EnumCommandType.TP_SPAWN;
            case TP_WORLD_SPAWN -> EnumCommandType.TP_WORLD_SPAWN;
            case TP_TOP -> EnumCommandType.TP_TOP;
            case TP_BOTTOM -> EnumCommandType.TP_BOTTOM;
            case TP_UP -> EnumCommandType.TP_UP;
            case TP_DOWN -> EnumCommandType.TP_DOWN;
            case TP_VIEW -> EnumCommandType.TP_VIEW;
            case TP_HOME -> EnumCommandType.TP_HOME;
            case TP_STAGE -> EnumCommandType.TP_STAGE;
            case TP_BACK -> EnumCommandType.TP_BACK;
            default -> null;
        };
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
