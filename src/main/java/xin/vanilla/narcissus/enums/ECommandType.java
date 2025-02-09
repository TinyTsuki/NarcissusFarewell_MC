package xin.vanilla.narcissus.enums;

import lombok.Getter;

@Getter
public enum ECommandType {
    HELP(0),
    DIMENSION(0),
    DIMENSION_CONCISE(0),
    FEED(0),
    FEED_CONCISE(0),
    TP_COORDINATE(1),
    TP_COORDINATE_CONCISE(2),
    TP_STRUCTURE(3),
    TP_STRUCTURE_CONCISE(4),
    TP_ASK(5),
    TP_ASK_CONCISE(6),
    TP_ASK_YES(7),
    TP_ASK_YES_CONCISE(8),
    TP_ASK_NO(9),
    TP_ASK_NO_CONCISE(10),
    TP_HERE(11),
    TP_HERE_CONCISE(12),
    TP_HERE_YES(13),
    TP_HERE_YES_CONCISE(14),
    TP_HERE_NO(15),
    TP_HERE_NO_CONCISE(16),
    TP_RANDOM(17),
    TP_RANDOM_CONCISE(18),
    TP_SPAWN(19),
    TP_SPAWN_CONCISE(20),
    TP_WORLD_SPAWN(21),
    TP_WORLD_SPAWN_CONCISE(22),
    TP_TOP(23),
    TP_TOP_CONCISE(24),
    TP_BOTTOM(25),
    TP_BOTTOM_CONCISE(26),
    TP_UP(27),
    TP_UP_CONCISE(28),
    TP_DOWN(29),
    TP_DOWN_CONCISE(30),
    TP_VIEW(31),
    TP_VIEW_CONCISE(32),
    TP_HOME(33),
    TP_HOME_CONCISE(34),
    SET_HOME(35),
    SET_HOME_CONCISE(36),
    DEL_HOME(37),
    DEL_HOME_CONCISE(38),
    TP_STAGE(39),
    TP_STAGE_CONCISE(40),
    SET_STAGE(41),
    SET_STAGE_CONCISE(42),
    DEL_STAGE(43),
    DEL_STAGE_CONCISE(44),
    TP_BACK(45),
    TP_BACK_CONCISE(46);

    private final int sort;

    ECommandType(int sort) {
        this.sort = sort;
    }

    public ECommandType replaceConcise() {
        if (this.name().endsWith("_CONCISE")) {
            return ECommandType.valueOf(this.name().replace("_CONCISE", ""));
        }
        return this;
    }
}
