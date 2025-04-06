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

    public ECommandType toCommandType() {
        return switch (this) {
            case TP_COORDINATE -> ECommandType.TP_COORDINATE;
            case TP_STRUCTURE -> ECommandType.TP_STRUCTURE;
            case TP_ASK -> ECommandType.TP_ASK;
            case TP_HERE -> ECommandType.TP_HERE;
            case TP_RANDOM -> ECommandType.TP_RANDOM;
            case TP_SPAWN -> ECommandType.TP_SPAWN;
            case TP_WORLD_SPAWN -> ECommandType.TP_WORLD_SPAWN;
            case TP_TOP -> ECommandType.TP_TOP;
            case TP_BOTTOM -> ECommandType.TP_BOTTOM;
            case TP_UP -> ECommandType.TP_UP;
            case TP_DOWN -> ECommandType.TP_DOWN;
            case TP_VIEW -> ECommandType.TP_VIEW;
            case TP_HOME -> ECommandType.TP_HOME;
            case TP_STAGE -> ECommandType.TP_STAGE;
            case TP_BACK -> ECommandType.TP_BACK;
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
