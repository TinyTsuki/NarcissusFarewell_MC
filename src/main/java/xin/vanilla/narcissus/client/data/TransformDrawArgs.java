package xin.vanilla.narcissus.client.data;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 变换绘制参数
 */
@Data
@Accessors(chain = true, fluent = true)
public class TransformDrawArgs {
    private final PoseStack stack;
    private double x;
    private double y;
    private double width;
    private double height;
}
