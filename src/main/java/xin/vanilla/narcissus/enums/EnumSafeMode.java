package xin.vanilla.narcissus.enums;

public enum EnumSafeMode {
    NONE,
    /**
     * 当前位置到顶部
     */
    Y_C_TO_T,
    /**
     * 底部到当前位置
     */
    Y_B_TO_C,
    /**
     * 当前位置到底部
     */
    Y_C_TO_B,
    /**
     * 顶部到当前位置
     */
    Y_T_TO_C,
    /**
     * 当前位置+-3
     */
    Y_C_OFFSET_3,
}
