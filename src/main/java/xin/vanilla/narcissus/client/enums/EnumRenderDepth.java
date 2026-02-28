package xin.vanilla.narcissus.client.enums;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * 渲染深度
 */
@Getter
@Accessors(fluent = true)
public enum EnumRenderDepth {
    /**
     * 背景
     */
    BACKGROUND(0),
    /**
     * 默认
     */
    DEFAULT(1),
    /**
     * 前景
     */
    FOREGROUND(250),
    /**
     * 悬浮窗
     */
    OVERLAY(500),
    /**
     * 提示
     */
    TOOLTIP(750),
    /**
     * 弹出提示
     */
    POPUP_TIPS(900),
    /**
     * 鼠标
     */
    MOUSE(1000);

    private final int depth;

    EnumRenderDepth(int depth) {
        this.depth = depth;
    }
}
