package xin.vanilla.narcissus.client.data;


import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Data;
import lombok.experimental.Accessors;
import xin.vanilla.narcissus.client.enums.EnumRotationCenter;

/**
 * 变换参数
 */
@Data
@Accessors(chain = true, fluent = true)
public class TransformArgs {
    private PoseStack stack;
    private double x;
    private double y;
    private double width;
    private double height;
    /**
     * 透明度
     */
    private double alpha = 0xFF;
    /**
     * 缩放比例
     */
    private double scale = 1.0;
    /**
     * 水平翻转
     */
    private boolean flipHorizontal;
    /**
     * 垂直翻转
     */
    private boolean flipVertical;
    /**
     * 旋转角度
     */
    private double angle = 0;
    /**
     * 旋转中心
     */
    private EnumRotationCenter center = EnumRotationCenter.CENTER;
    /**
     * 混合模式
     */
    private boolean blend = false;

    public TransformArgs(PoseStack stack) {
        this.stack = stack;
    }

    public TransformArgs coordinate(ScreenCoordinate coordinate) {
        this.x = coordinate.x();
        this.y = coordinate.y();
        this.width = coordinate.width();
        this.height = coordinate.height();
        return this;
    }

    public double getWidthScaled() {
        return this.width * this.scale;
    }

    public double getHeightScaled() {
        return this.height * this.scale;
    }

}
