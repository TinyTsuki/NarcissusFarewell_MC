package xin.vanilla.narcissus.enums;

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
        switch (this) {
            case TP_COORDINATE:
                return ECommandType.TP_COORDINATE;
            case TP_STRUCTURE:
                return ECommandType.TP_STRUCTURE;
            case TP_ASK:
                return ECommandType.TP_ASK;
            case TP_HERE:
                return ECommandType.TP_HERE;
            case TP_RANDOM:
                return ECommandType.TP_RANDOM;
            case TP_SPAWN:
                return ECommandType.TP_SPAWN;
            case TP_WORLD_SPAWN:
                return ECommandType.TP_WORLD_SPAWN;
            case TP_TOP:
                return ECommandType.TP_TOP;
            case TP_BOTTOM:
                return ECommandType.TP_BOTTOM;
            case TP_UP:
                return ECommandType.TP_UP;
            case TP_DOWN:
                return ECommandType.TP_DOWN;
            case TP_VIEW:
                return ECommandType.TP_VIEW;
            case TP_HOME:
                return ECommandType.TP_HOME;
            case TP_STAGE:
                return ECommandType.TP_STAGE;
            case TP_BACK:
                return ECommandType.TP_BACK;
            default:
                return null;
        }
    }
}
