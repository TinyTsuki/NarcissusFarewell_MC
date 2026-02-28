package xin.vanilla.narcissus.client.data;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 变换绘制参数
 */
@Data
@Accessors(chain = true, fluent = true)
public class TransformDrawArgs {
    private final MatrixStack stack;
    private double x;
    private double y;
    private double width;
    private double height;
}
