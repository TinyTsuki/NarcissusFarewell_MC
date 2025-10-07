package xin.vanilla.narcissus.enums;

import lombok.Getter;

@Getter
public enum EnumCommandType {
    HELP(false, false),
    LANGUAGE(false, false),
    LANGUAGE_CONCISE(),
    UUID(),
    UUID_CONCISE(),
    DIMENSION(),
    DIMENSION_CONCISE(),
    CARD(false, false),
    CARD_CONCISE(),
    SET_CARD(true),
    SET_CARD_CONCISE(),
    SHARE(),
    SHARE_CONCISE(),
    FEED(false, false),
    FEED_CONCISE(),
    FEED_OTHER(true),
    FEED_OTHER_CONCISE(true),
    TP_COORDINATE(),
    TP_COORDINATE_CONCISE(),
    TP_STRUCTURE(),
    TP_STRUCTURE_CONCISE(),
    TP_ASK(),
    TP_ASK_CONCISE(),
    TP_ASK_YES(false, false),
    TP_ASK_YES_CONCISE(),
    TP_ASK_NO(false, false),
    TP_ASK_NO_CONCISE(),
    TP_ASK_CANCEL(false, false),
    TP_ASK_CANCEL_CONCISE(),
    TP_HERE(),
    TP_HERE_CONCISE(),
    TP_HERE_YES(false, false),
    TP_HERE_YES_CONCISE(),
    TP_HERE_NO(false, false),
    TP_HERE_NO_CONCISE(),
    TP_HERE_CANCEL(false, false),
    TP_HERE_CANCEL_CONCISE(),
    TP_RANDOM(),
    TP_RANDOM_CONCISE(),
    TP_SPAWN(),
    TP_SPAWN_OTHER(true),
    TP_SPAWN_CONCISE(),
    TP_SPAWN_OTHER_CONCISE(true),
    TP_WORLD_SPAWN(),
    TP_WORLD_SPAWN_CONCISE(),
    TP_TOP(),
    TP_TOP_CONCISE(),
    TP_BOTTOM(),
    TP_BOTTOM_CONCISE(),
    TP_UP(),
    TP_UP_CONCISE(),
    TP_DOWN(),
    TP_DOWN_CONCISE(),
    TP_VIEW(),
    TP_VIEW_CONCISE(),
    TP_HOME(),
    TP_HOME_CONCISE(),
    SET_HOME(false, false),
    SET_HOME_CONCISE(),
    DEL_HOME(false, false),
    DEL_HOME_CONCISE(),
    GET_HOME(false, false),
    GET_HOME_CONCISE(),
    TP_STAGE(),
    TP_STAGE_CONCISE(),
    SET_STAGE(),
    SET_STAGE_CONCISE(),
    DEL_STAGE(),
    DEL_STAGE_CONCISE(),
    GET_STAGE(),
    GET_STAGE_CONCISE(),
    TP_BACK(),
    TP_BACK_CONCISE(),
    FLY(),
    FLY_CONCISE(),
    VIRTUAL_OP(),
    VIRTUAL_OP_CONCISE();

    /**
     * 在帮助信息内忽略
     */
    private final boolean ignore;
    /**
     * 是否简短指令
     */
    private final boolean concise = this.name().endsWith("_CONCISE");
    /**
     * 是否被虚拟权限管理
     */
    private final boolean op;

    EnumCommandType() {
        this.ignore = false;
        this.op = !this.concise;
    }

    EnumCommandType(boolean ig) {
        this.ignore = ig;
        this.op = !this.concise;
    }

    EnumCommandType(boolean ig, boolean op) {
        this.ignore = ig;
        this.op = !this.concise && op;
    }

    public int getSort() {
        return this.ordinal();
    }

    public EnumCommandType replaceConcise() {
        if (this.name().endsWith("_CONCISE")) {
            return EnumCommandType.valueOf(this.name().replace("_CONCISE", ""));
        }
        return this;
    }

    public EnumTeleportType toTeleportType() {
        return switch (this) {
            case TP_COORDINATE, TP_COORDINATE_CONCISE -> EnumTeleportType.TP_COORDINATE;
            case TP_STRUCTURE, TP_STRUCTURE_CONCISE -> EnumTeleportType.TP_STRUCTURE;
            // case TP_ASK_YES:
            // case TP_ASK_NO:
            // case TP_ASK_CANCEL:
            case TP_ASK, TP_ASK_CONCISE ->
                // case TP_ASK_YES_CONCISE:
                // case TP_ASK_NO_CONCISE:
                // case TP_ASK_CANCEL_CONCISE:
                    EnumTeleportType.TP_ASK;
            // case TP_HERE_YES:
            // case TP_HERE_NO:
            // case TP_HERE_CANCEL:
            case TP_HERE, TP_HERE_CONCISE ->
                // case TP_HERE_YES_CONCISE:
                // case TP_HERE_NO_CONCISE:
                // case TP_HERE_CANCEL_CONCISE:
                    EnumTeleportType.TP_HERE;
            case TP_RANDOM, TP_RANDOM_CONCISE -> EnumTeleportType.TP_RANDOM;
            case TP_SPAWN, TP_SPAWN_OTHER, TP_SPAWN_CONCISE, TP_SPAWN_OTHER_CONCISE -> EnumTeleportType.TP_SPAWN;
            case TP_WORLD_SPAWN, TP_WORLD_SPAWN_CONCISE -> EnumTeleportType.TP_WORLD_SPAWN;
            case TP_TOP, TP_TOP_CONCISE -> EnumTeleportType.TP_TOP;
            case TP_BOTTOM, TP_BOTTOM_CONCISE -> EnumTeleportType.TP_BOTTOM;
            case TP_UP, TP_UP_CONCISE -> EnumTeleportType.TP_UP;
            case TP_DOWN, TP_DOWN_CONCISE -> EnumTeleportType.TP_DOWN;
            case TP_VIEW, TP_VIEW_CONCISE -> EnumTeleportType.TP_VIEW;
            // case SET_HOME:
            // case DEL_HOME:
            // case GET_HOME:
            case TP_HOME, TP_HOME_CONCISE ->
                // case SET_HOME_CONCISE:
                // case DEL_HOME_CONCISE:
                // case GET_HOME_CONCISE:
                    EnumTeleportType.TP_HOME;
            // case SET_STAGE:
            // case DEL_STAGE:
            // case GET_STAGE:
            case TP_STAGE, TP_STAGE_CONCISE ->
                // case SET_STAGE_CONCISE:
                // case DEL_STAGE_CONCISE:
                // case GET_STAGE_CONCISE:
                    EnumTeleportType.TP_STAGE;
            case TP_BACK, TP_BACK_CONCISE -> EnumTeleportType.TP_BACK;
            default -> null;
        };
    }
}
