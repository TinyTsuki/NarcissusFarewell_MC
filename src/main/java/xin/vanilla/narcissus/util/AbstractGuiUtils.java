package xin.vanilla.narcissus.util;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import xin.vanilla.narcissus.client.component.Text;
import xin.vanilla.narcissus.client.component.TextList;
import xin.vanilla.narcissus.client.data.*;
import xin.vanilla.narcissus.client.enums.EnumAlignment;
import xin.vanilla.narcissus.client.enums.EnumEllipsisPosition;
import xin.vanilla.narcissus.client.enums.EnumRenderDepth;
import xin.vanilla.narcissus.client.enums.EnumRotationCenter;
import xin.vanilla.narcissus.data.Color;
import xin.vanilla.narcissus.data.KeyValue;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * AbstractGui工具类
 */
@OnlyIn(Dist.CLIENT)
public final class AbstractGuiUtils {
    private AbstractGuiUtils() {
    }

    public static final int ITEM_ICON_SIZE = 16;

    private static final Random random = new Random();


    public static void renderByDepth(GuiGraphics graphics, Consumer<GuiGraphics> drawFunc) {
        AbstractGuiUtils.renderByDepth(graphics, EnumRenderDepth.DEFAULT, drawFunc);
    }

    public static void renderByDepth(GuiGraphics graphics, EnumRenderDepth depth, Consumer<GuiGraphics> drawFunc) {
        if (depth != null) {
            renderByDepth(graphics, depth.depth(), drawFunc);
        } else {
            drawFunc.accept(graphics);
        }
    }

    public static void renderByDepth(GuiGraphics graphics, int depth, Consumer<GuiGraphics> drawFunc) {
        boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);

        try {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, depth);

            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);

            drawFunc.accept(graphics);
        } finally {
            graphics.pose().popPose();

            if (!depthTest) {
                RenderSystem.disableDepthTest();
            } else {
                RenderSystem.enableDepthTest();
            }
            RenderSystem.depthFunc(depthFunc);
        }
    }


    public static void bindTexture(ResourceLocation resourceLocation) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, resourceLocation);
    }

    public static void blit(GuiGraphics graphics, int x0, int y0, int z, int destWidth, int destHeight, TextureAtlasSprite sprite) {
        graphics.blit(x0, y0, z, destWidth, destHeight, sprite);
    }

    public static void blitBlend(GuiGraphics graphics, int x0, int y0, int z, int destWidth, int destHeight, TextureAtlasSprite sprite) {
        blitByBlend(() ->
                graphics.blit(x0, y0, z, destWidth, destHeight, sprite)
        );
    }

    public static void blit(GuiGraphics graphics, ResourceLocation location, int x0, int y0, int z, double u0, double v0, int width, int height, int textureHeight, int textureWidth) {
        graphics.blit(location, x0, y0, z, (float) u0, (float) v0, width, height, textureHeight, textureWidth);
    }

    public static void blitBlend(GuiGraphics graphics, ResourceLocation location, int x0, int y0, int z, double u0, double v0, int width, int height, int textureHeight, int textureWidth) {
        blitByBlend(() ->
                graphics.blit(location, x0, y0, z, (float) u0, (float) v0, width, height, textureHeight, textureWidth)
        );
    }

    public static void blit(GuiGraphics graphics, ResourceLocation location, int x0, int y0, int destWidth, int destHeight, double u0, double v0, int srcWidth, int srcHeight, int textureWidth, int textureHeight) {
        graphics.blit(location, x0, y0, destWidth, destHeight, (float) u0, (float) v0, srcWidth, srcHeight, textureWidth, textureHeight);
    }

    public static void blitBlend(GuiGraphics graphics, ResourceLocation location, int x0, int y0, int destWidth, int destHeight, double u0, double v0, int srcWidth, int srcHeight, int textureWidth, int textureHeight) {
        blitByBlend(() ->
                graphics.blit(location, x0, y0, destWidth, destHeight, (float) u0, (float) v0, srcWidth, srcHeight, textureWidth, textureHeight)
        );
    }

    public static void blit(GuiGraphics graphics, ResourceLocation location, int x0, int y0, double u0, double v0, int destWidth, int destHeight, int textureWidth, int textureHeight) {
        graphics.blit(location, x0, y0, (float) u0, (float) v0, destWidth, destHeight, textureWidth, textureHeight);
    }

    public static void blitBlend(GuiGraphics graphics, ResourceLocation location, int x0, int y0, double u0, double v0, int destWidth, int destHeight, int textureWidth, int textureHeight) {
        blitByBlend(() ->
                graphics.blit(location, x0, y0, (float) u0, (float) v0, destWidth, destHeight, textureWidth, textureHeight)
        );
    }

    /**
     * 启用混合模式来绘制纹理
     */
    public static void blitByBlend(Runnable drawFunc) {
        // 启用混合模式来正确处理透明度
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        drawFunc.run();
        RenderSystem.disableBlend();
    }

    /**
     * 变换后绘制
     *
     * @param args 变换参数
     */
    public static void renderByTransform(TransformArgs args, Consumer<TransformDrawArgs> drawFunc) {

        // 保存当前矩阵状态
        args.stack().pushPose();

        // 计算目标点
        double tranX = 0, tranY = 0;
        double tranW = 0, tranH = 0;
        // 旋转角度为0不需要进行变换
        if (args.angle() % 360 == 0) args.center(EnumRotationCenter.TOP_LEFT);
        switch (args.center()) {
            case CENTER:
                tranW = args.getWidthScaled() / 2.0;
                tranH = args.getHeightScaled() / 2.0;
                tranX = args.x() + tranW;
                tranY = args.y() + tranH;
                break;
            case TOP_LEFT:
                tranX = args.x();
                tranY = args.y();
                break;
            case TOP_RIGHT:
                tranW = args.getWidthScaled();
                tranX = args.x() + tranW;
                tranY = args.y();
                break;
            case TOP_CENTER:
                tranW = args.getWidthScaled() / 2.0;
                tranX = args.x() + tranW;
                tranY = args.y();
                break;
            case BOTTOM_LEFT:
                tranH = args.getHeightScaled();
                tranX = args.x();
                tranY = args.y() + tranH;
                break;
            case BOTTOM_RIGHT:
                tranW = args.getWidthScaled();
                tranH = args.getHeightScaled();
                tranX = args.x() + tranW;
                tranY = args.y() + tranH;
                break;
            case BOTTOM_CENTER:
                tranW = args.getWidthScaled() / 2.0;
                tranH = args.getHeightScaled();
                tranX = args.x() + tranW;
                tranY = args.y() + tranH;
                break;
        }
        // 移至目标点
        args.stack().translate(tranX, tranY, 0);

        // 缩放
        args.stack().scale((float) args.scale(), (float) args.scale(), 1);

        // 旋转
        if (args.angle() % 360 != 0) {
            args.stack().mulPose(new Quaternionf().rotateZ((float) Math.toRadians(args.angle())));
        }

        // 翻转
        if (args.flipHorizontal()) {
            args.stack().mulPose(new Quaternionf().rotateY((float) Math.toRadians(180)));
        }
        if (args.flipVertical()) {
            args.stack().mulPose(new Quaternionf().rotateX((float) Math.toRadians(180)));
        }

        // 返回原点
        args.stack().translate(-tranW, -tranH, 0);

        // 关闭背面剔除
        RenderSystem.disableCull();
        // 绘制方法
        TransformDrawArgs drawArgs = new TransformDrawArgs(args.stack());
        drawArgs.x(0).y(0).width(args.width()).height(args.height());

        // 启用混合模式
        if (args.blend() || args.alpha() < 0xFF) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            // 设置透明度
            if (args.alpha() < 0xFF)
                RenderSystem.setShaderColor(1, 1, 1, (float) args.alpha() / 0xFF);
        }

        drawFunc.accept(drawArgs);

        // 关闭混合模式
        if (args.blend() || args.alpha() < 0xFF) {
            // 还原透明度
            if (args.alpha() < 0xFF)
                RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.disableBlend();
        }

        // 恢复背面剔除
        RenderSystem.enableCull();

        // 恢复矩阵状态
        args.stack().popPose();
    }

    // endregion 绘制纹理


    // region 文本高度

    /**
     * 获取多行文本的高度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getTextHeight(Text... text) {
        return getTextHeight(AbstractGuiUtils.getFont(), text);
    }

    /**
     * 获取多行文本的高度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getTextHeight(Collection<Text> text) {
        return getTextHeight(AbstractGuiUtils.getFont(), text);
    }

    /**
     * 获取多行文本的高度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getTextHeight(TextList text) {
        return getTextHeight(AbstractGuiUtils.getFont(), text);
    }

    /**
     * 获取多行文本的高度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getComponentHeight(Component... text) {
        return getComponentHeight(AbstractGuiUtils.getFont(), text);
    }

    /**
     * 获取多行文本的高度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getComponentHeight(Collection<Component> text) {
        return getComponentHeight(AbstractGuiUtils.getFont(), text);
    }

    /**
     * 获取多行文本的高度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getStringHeight(String... text) {
        return getStringHeight(AbstractGuiUtils.getFont(), text);
    }

    /**
     * 获取多行文本的高度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getStringHeight(Collection<String> text) {
        return getStringHeight(AbstractGuiUtils.getFont(), text);
    }

    /**
     * 获取多行文本的高度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getTextHeight(Font font, Text... text) {
        return getStringHeight(font, Arrays.stream(text).map(Text::content).collect(Collectors.toList()));
    }

    /**
     * 获取多行文本的高度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getTextHeight(Font font, Collection<Text> text) {
        return getStringHeight(font, text.stream().map(Text::content).collect(Collectors.toList()));
    }

    /**
     * 获取多行文本的高度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getTextHeight(Font font, TextList text) {
        return getStringHeight(font, text.stream().map(Text::content).collect(Collectors.toList()));
    }

    /**
     * 获取多行文本的高度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getComponentHeight(Font font, Component... text) {
        return getStringHeight(font, Arrays.stream(text).map(component -> component.getString(I18nUtils.getClientLanguage())).collect(Collectors.toList()));
    }

    /**
     * 获取多行文本的高度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getComponentHeight(Font font, Collection<Component> text) {
        return getStringHeight(font, text.stream().map(component -> component.getString(I18nUtils.getClientLanguage())).collect(Collectors.toList()));
    }

    /**
     * 获取多行文本的高度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getStringHeight(Font font, String... text) {
        return getStringHeight(font, Arrays.asList(text));
    }

    /**
     * 获取多行文本的高度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getStringHeight(Font font, Collection<String> text) {
        return text.stream().mapToInt(t -> StringUtils.replaceLineBreak(t).split("\n").length * font.lineHeight).sum();
    }

    // endregion 文本高度


    // region 文本宽度

    /**
     * 获取多行文本的宽度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getTextWidth(Text... text) {
        return getTextWidth(AbstractGuiUtils.getFont(), text);
    }

    /**
     * 获取多行文本的宽度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getTextWidth(Collection<Text> text) {
        return getTextWidth(AbstractGuiUtils.getFont(), text);
    }

    /**
     * 获取多行文本的宽度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getTextWidth(TextList text) {
        return getTextWidth(AbstractGuiUtils.getFont(), text);
    }

    /**
     * 获取多行文本的宽度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getComponentWidth(Component... text) {
        return getComponentWidth(AbstractGuiUtils.getFont(), text);
    }

    /**
     * 获取多行文本的宽度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getComponentWidth(Collection<Component> text) {
        return getComponentWidth(AbstractGuiUtils.getFont(), text);
    }

    /**
     * 获取多行文本的宽度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getStringWidth(String... text) {
        return getStringWidth(AbstractGuiUtils.getFont(), text);
    }

    /**
     * 获取多行文本的宽度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getStringWidth(Collection<String> text) {
        return getStringWidth(AbstractGuiUtils.getFont(), text);
    }

    /**
     * 获取多行文本的宽度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getTextWidth(Font font, Text... text) {
        return getStringWidth(font, Arrays.stream(text).map(Text::content).collect(Collectors.toList()));
    }

    /**
     * 获取多行文本的宽度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getTextWidth(Font font, Collection<Text> text) {
        return getStringWidth(font, text.stream().map(Text::content).collect(Collectors.toList()));
    }

    /**
     * 获取多行文本的宽度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getTextWidth(Font font, TextList text) {
        return getStringWidth(font, text.stream().map(Text::content).collect(Collectors.toList()));
    }

    /**
     * 获取多行文本的宽度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getComponentWidth(Font font, Component... text) {
        return getStringWidth(font, Arrays.stream(text).map(component -> component.getString(I18nUtils.getClientLanguage())).collect(Collectors.toList()));
    }

    /**
     * 获取多行文本的宽度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getComponentWidth(Font font, Collection<Component> text) {
        return getStringWidth(font, text.stream().map(component -> component.getString(I18nUtils.getClientLanguage())).collect(Collectors.toList()));
    }

    /**
     * 获取多行文本的宽度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getStringWidth(Font font, String... text) {
        return getStringWidth(font, Arrays.asList(text));
    }

    /**
     * 获取多行文本的宽度，以\n为换行符
     *
     * @param text 要绘制的文本
     */
    public static int getStringWidth(Font font, Collection<String> text) {
        return text.stream()
                .map(t -> StringUtils.replaceLineBreak(t).split("\n"))
                .flatMap(Arrays::stream)
                .mapToInt(font::width)
                .max().orElse(0);
    }

    // endregion 文本宽度


    // region 绘制文字

    /**
     * 绘制多行文本，以\n为换行符
     *
     * @param argbs 文本颜色
     */
    public static void drawMultilineText(@Nonnull FontDrawArgs args, int... argbs) {
        if (StringUtils.isNotNullOrEmpty(args.text().content())) {
            String[] lines = StringUtils.replaceLineBreak(args.text().content()).split("\n");
            FontDrawArgs clone = args.clone();
            double y = clone.y();
            for (int i = 0; i < lines.length; i++) {
                int argb;
                if (argbs.length == lines.length) {
                    argb = argbs[i];
                } else if (argbs.length > 0) {
                    argb = argbs[i % argbs.length];
                } else {
                    argb = args.text().colorArgb();
                }
                clone.text().text(lines[i]).color(Color.argb(argb));
                AbstractGuiUtils.drawLimitedText(clone.y(y + i * clone.text().font().lineHeight));
            }
        }
    }

    /**
     * 将文本按最大宽度自动换行
     *
     * @param text     要换行的文本
     * @param maxWidth 最大宽度
     * @return 换行后的文本列表
     */
    private static List<String> wrapText(Font font, String text, int maxWidth) {
        List<String> wrappedLines = new ArrayList<>();
        if (maxWidth <= 0 || text == null || text.isEmpty()) {
            if (text != null && !text.isEmpty()) {
                wrappedLines.add(text);
            }
            return wrappedLines;
        }

        // 定义分隔符模式，\p{Punct} 匹配所有标点符号
        String separatorPattern = "[\\s\\p{Punct}]+";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(separatorPattern);

        // 使用正则表达式分割文本，保留分隔符
        List<String> segments = StringUtils.splitStrings(text, pattern);

        // 如果文本中没有分隔符，直接按字符处理
        if (segments.isEmpty()) {
            return splitLongSegment(font, text, maxWidth);
        }

        StringBuilder currentLine = new StringBuilder();

        for (String segment : segments) {
            // 判断是否是分隔符
            boolean isSeparator = pattern.matcher(segment).matches();

            // 构建测试行
            String testLine;
            if (currentLine.length() == 0) {
                testLine = segment;
            } else if (isSeparator) {
                testLine = currentLine + segment;
            } else {
                // 检查前一个字符是否是分隔符
                String lastChar = currentLine.length() > 0 ?
                        String.valueOf(currentLine.charAt(currentLine.length() - 1)) : "";
                boolean lastIsSeparator = !lastChar.isEmpty() && pattern.matcher(lastChar).matches();

                if (lastIsSeparator) {
                    testLine = currentLine + segment;
                } else {
                    testLine = currentLine + " " + segment;
                }
            }

            int testWidth = font.width(testLine);

            if (testWidth > maxWidth && currentLine.length() > 0) {
                // 当前行已满，保存当前行并开始新行
                wrappedLines.add(currentLine.toString());

                // 处理当前段
                if (isSeparator) {
                    // 分隔符保留在下一行开头
                    currentLine = new StringBuilder(segment);
                } else {
                    // 检查单个段是否超过最大宽度
                    if (font.width(segment) > maxWidth) {
                        // 强制换行
                        List<String> splitSegments = splitLongSegment(font, segment, maxWidth);
                        if (!splitSegments.isEmpty()) {
                            currentLine = new StringBuilder(splitSegments.get(0));
                            // 将剩余部分添加到新行
                            for (int i = 1; i < splitSegments.size(); i++) {
                                wrappedLines.add(splitSegments.get(i));
                            }
                        } else {
                            currentLine = new StringBuilder();
                        }
                    } else {
                        // 开始新行
                        currentLine = new StringBuilder(segment);
                    }
                }
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }

        // 添加最后一行
        if (currentLine.length() > 0) {
            wrappedLines.add(currentLine.toString());
        }

        // 若仍然超过最大宽度，进行强制换行
        List<String> finalLines = new ArrayList<>();
        for (String line : wrappedLines) {
            if (font.width(line) > maxWidth) {
                finalLines.addAll(splitLongSegment(font, line, maxWidth));
            } else {
                finalLines.add(line);
            }
        }

        return finalLines.isEmpty() ? wrappedLines : finalLines;
    }

    /**
     * 将超长的文本段按最大宽度强制换行
     *
     * @param segment  要分割的文本段
     * @param maxWidth 最大宽度
     * @return 分割后的文本列表
     */
    private static List<String> splitLongSegment(Font font, String segment, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (segment == null || segment.isEmpty() || maxWidth <= 0) {
            if (segment != null && !segment.isEmpty()) {
                lines.add(segment);
            }
            return lines;
        }

        // 如果整个段都在最大宽度内，直接返回
        int segmentWidth = font.width(segment);
        if (segmentWidth <= maxWidth) {
            lines.add(segment);
            return lines;
        }

        int startIdx = 0;

        // 使用批量处理策略：先尝试较大的步长，然后逐步缩小
        while (startIdx < segment.length()) {
            int endIdx = Math.min(startIdx + maxWidth / 4, segment.length()); // 初始步长为估计的字符数
            String testLine = segment.substring(startIdx, endIdx);
            int testWidth = font.width(testLine);

            // 如果宽度合适，尝试扩展
            if (testWidth <= maxWidth) {
                // 尝试扩展直到接近最大宽度
                while (endIdx < segment.length()) {
                    int nextEndIdx = endIdx + 1;
                    String nextTestLine = segment.substring(startIdx, nextEndIdx);
                    int nextWidth = font.width(nextTestLine);
                    if (nextWidth > maxWidth) {
                        break;
                    }
                    testLine = nextTestLine;
                    testWidth = nextWidth;
                    endIdx = nextEndIdx;
                }
                lines.add(testLine);
                startIdx = endIdx;
            } else {
                // 宽度超出，需要缩小：使用二分查找找到合适的截断点
                int left = startIdx;
                int right = endIdx;
                while (left < right) {
                    int mid = (left + right) / 2;
                    String midLine = segment.substring(startIdx, mid);
                    if (font.width(midLine) <= maxWidth) {
                        left = mid + 1;
                    } else {
                        right = mid;
                    }
                }
                // 确保至少保留一个字符
                if (left <= startIdx + 1) {
                    left = startIdx + 1;
                }
                lines.add(segment.substring(startIdx, left - 1));
                startIdx = left - 1;
            }
        }

        return lines;
    }

    /**
     * 计算文字绘制后的最终宽高
     *
     * @param args 绘制参数
     */
    public static KeyValue<Integer, Integer> calculateLimitedTextSize(@Nonnull FontDrawArgs args) {
        Text text = args.text();
        if (StringUtils.isNullOrEmpty(text.content())) {
            return new KeyValue<>(0, 0);
        }

        String ellipsis = "...";
        Font font = text.font();
        int ellipsisWidth = font.width(ellipsis);

        // 缩放比例
        float scale = args.fontSize() > 0 ? args.fontSize() / font.lineHeight : 1.0f;

        // 实际绘制区域
        double drawX = args.x() + args.paddingLeft();
        // 可用宽度 = 原始可用宽度 / 缩放比例
        int availableWidth = args.maxWidth() > 0 ? (int) ((args.maxWidth() - args.paddingLeft() - args.paddingRight()) / scale) : 0;

        if (args.inScreen() && availableWidth > 0 && !args.wrap()) {
            KeyValue<Integer, Integer> screenSize = getScreenSize();
            int screenWidth = screenSize.key();

            // 确保文本不超出屏幕右边界
            if (drawX + availableWidth > screenWidth - args.marginRight()) {
                availableWidth = Math.max(0, screenWidth - (int) drawX - args.marginRight());
            }
            // 确保文本不超出屏幕左边界
            if (drawX < args.marginLeft()) {
                availableWidth = Math.max(0, availableWidth - args.marginLeft() + (int) args.x());
            }
        }

        // 拆分文本行
        List<String> lines = new ArrayList<>();
        String[] originalLines = StringUtils.replaceLineBreak(text.content()).split("\n");

        // 如果启用自动换行且设置了最大宽度，对每行进行换行处理
        if (args.wrap() && availableWidth > 0) {
            for (String originalLine : originalLines) {
                lines.addAll(wrapText(font, originalLine, availableWidth));
            }
        } else {
            lines.addAll(Arrays.asList(originalLines));
        }

        int actualMaxLine = args.maxLine();
        if (actualMaxLine <= 0 || actualMaxLine >= lines.size()) {
            actualMaxLine = lines.size();
        }

        List<String> outputLines = new ArrayList<>();
        if (actualMaxLine > 1 && lines.size() > actualMaxLine) {
            switch (args.position()) {
                case START:
                    // 显示最后 maxLine 行，开头加省略号
                    outputLines.add(ellipsis);
                    outputLines.addAll(lines.subList(lines.size() - actualMaxLine + 1, lines.size()));
                    break;
                case MIDDLE:
                    // 显示前后各一部分，中间加省略号
                    int midStart = actualMaxLine / 2;
                    int midEnd = lines.size() - (actualMaxLine - midStart) + 1;
                    outputLines.addAll(lines.subList(0, midStart));
                    outputLines.add(ellipsis);
                    outputLines.addAll(lines.subList(midEnd, lines.size()));
                    break;
                case END:
                    // 显示前 maxLine 行，结尾加省略号
                    outputLines.addAll(lines.subList(0, actualMaxLine - 1));
                    outputLines.add(ellipsis);
                    break;
                default:
                    outputLines.addAll(lines);
                    break;
            }
        } else {
            if (actualMaxLine == 1) {
                outputLines.add(lines.get(0));
            } else {
                // 正常显示所有行
                outputLines.addAll(lines);
            }
        }

        // 处理每行的截断
        List<String> finalLines = new ArrayList<>();
        for (String line : outputLines) {
            // 若宽度超出可用宽度，进行截断并加省略号
            line = ellipsisString(args, ellipsis, font, ellipsisWidth, availableWidth, line);
            finalLines.add(line);
        }

        // 计算文本区域尺寸
        int maxLineWidth = getStringWidth(font, finalLines);
        if (availableWidth > 0) {
            maxLineWidth = Math.min(maxLineWidth, availableWidth);
        }
        // 实际行高使用
        float actualLineHeight = args.fontSize() > 0 ? args.fontSize() : font.lineHeight;
        int totalHeight = (int) (finalLines.size() * actualLineHeight);

        // 最终宽高
        int finalWidth = (int) Math.ceil(maxLineWidth * scale) + args.paddingLeft() + args.paddingRight();
        int finalHeight = totalHeight + args.paddingTop() + args.paddingBottom();

        return new KeyValue<>(finalWidth, finalHeight);
    }

    /**
     * 使用二分查找优化字符串截断，减少字体宽度计算次数
     */
    private static String ellipsisString(@Nonnull FontDrawArgs args, String ellipsis, Font font, int ellipsisWidth, int availableWidth, String line) {
        if (availableWidth <= 0) {
            return line;
        }

        int lineWidth = font.width(line);
        if (lineWidth <= availableWidth) {
            return line;
        }

        if (args.position() == EnumEllipsisPosition.START) {
            // 截断前部：使用二分查找找到合适的起始位置
            int left = 0;
            int right = line.length();
            while (left < right) {
                int mid = (left + right + 1) / 2;
                String testLine = ellipsis + line.substring(mid);
                if (font.width(testLine) <= availableWidth) {
                    right = mid - 1;
                } else {
                    left = mid;
                }
            }
            // 确保至少保留一个字符
            if (left >= line.length() - 1) {
                left = Math.max(0, line.length() - 1);
            }
            return ellipsis + line.substring(left + 1);
        } else if (args.position() == EnumEllipsisPosition.END) {
            // 截断后部：使用二分查找找到合适的结束位置
            int left = 0;
            int right = line.length();
            while (left < right) {
                int mid = (left + right) / 2;
                String testLine = line.substring(0, mid) + ellipsis;
                if (font.width(testLine) <= availableWidth) {
                    left = mid + 1;
                } else {
                    right = mid;
                }
            }
            // 确保至少保留一个字符
            if (left <= 1) {
                left = Math.min(line.length(), 1);
            }
            return line.substring(0, left - 1) + ellipsis;
        } else {
            // 截断两侧（默认处理）：使用二分查找分别处理前后两部分
            int halfWidth = (availableWidth - ellipsisWidth) / 2;
            if (halfWidth <= 0) {
                return ellipsis;
            }

            // 查找前半部分的结束位置
            int left = 0;
            int right = line.length();
            while (left < right) {
                int mid = (left + right + 1) / 2;
                if (font.width(line.substring(0, mid)) <= halfWidth) {
                    left = mid;
                } else {
                    right = mid - 1;
                }
            }
            String start = line.substring(0, left);

            // 查找后半部分的起始位置
            left = 0;
            right = line.length();
            while (left < right) {
                int mid = (left + right) / 2;
                if (font.width(line.substring(mid)) <= halfWidth) {
                    right = mid;
                } else {
                    left = mid + 1;
                }
            }
            String end = line.substring(left);

            // 确保前后两部分不重叠
            if (start.length() + end.length() >= line.length()) {
                // 如果重叠，则只保留前半部分
                return start + ellipsis;
            }

            return start + ellipsis + end;
        }
    }

    /**
     * 绘制限制长度的文本，超出部分以省略号表示，可选择省略号的位置
     */
    public static void drawLimitedText(@Nonnull FontDrawArgs args) {
        Text text = args.text();
        if (StringUtils.isNotNullOrEmpty(text.content())) {
            String ellipsis = "...";
            Font font = text.font();
            int ellipsisWidth = font.width(ellipsis);

            // 计算缩放比例
            float scale = args.fontSize() > 0 ? args.fontSize() / font.lineHeight : 1.0f;

            // 实际绘制区域
            double drawX = args.x() + args.paddingLeft();
            double drawY = args.y() + args.paddingTop();
            // 可用宽度需 = 原始可用宽度 / 缩放比例
            int availableWidth = args.maxWidth() > 0 ? (int) ((args.maxWidth() - args.paddingLeft() - args.paddingRight()) / scale) : 0;

            if (args.inScreen() && availableWidth > 0) {
                KeyValue<Integer, Integer> screenSize = getScreenSize();
                int screenWidth = screenSize.key();

                // 确保文本不超出屏幕右边界
                if (drawX + availableWidth > screenWidth - args.marginRight()) {
                    availableWidth = Math.max(0, screenWidth - (int) drawX - args.marginRight());
                }
                // 确保文本不超出屏幕左边界
                if (drawX < args.marginLeft()) {
                    drawX = args.marginLeft();
                    availableWidth = Math.max(0, availableWidth - args.marginLeft() + (int) args.x());
                }
            }

            // 拆分文本行
            List<String> lines = new ArrayList<>();
            String[] originalLines = StringUtils.replaceLineBreak(text.content()).split("\n");

            // 若启用自动换行且设置了最大宽度，对每行进行换行处理
            if (args.wrap() && availableWidth > 0) {
                for (String originalLine : originalLines) {
                    lines.addAll(wrapText(font, originalLine, availableWidth));
                }
            } else {
                lines.addAll(Arrays.asList(originalLines));
            }

            int actualMaxLine = args.maxLine();
            if (actualMaxLine <= 0 || actualMaxLine >= lines.size()) {
                actualMaxLine = lines.size();
            }

            List<String> outputLines = new ArrayList<>();
            if (actualMaxLine > 1 && lines.size() > actualMaxLine) {
                switch (args.position()) {
                    case START:
                        // 显示最后 maxLine 行，开头加省略号
                        outputLines.add(ellipsis);
                        outputLines.addAll(lines.subList(lines.size() - actualMaxLine + 1, lines.size()));
                        break;
                    case MIDDLE:
                        // 显示前后各一部分，中间加省略号
                        int midStart = actualMaxLine / 2;
                        int midEnd = lines.size() - (actualMaxLine - midStart) + 1;
                        outputLines.addAll(lines.subList(0, midStart));
                        outputLines.add(ellipsis);
                        outputLines.addAll(lines.subList(midEnd, lines.size()));
                        break;
                    case END:
                        // 显示前 maxLine 行，结尾加省略号
                        outputLines.addAll(lines.subList(0, actualMaxLine - 1));
                        outputLines.add(ellipsis);
                        break;
                    default:
                        outputLines.addAll(lines);
                        break;
                }
            } else {
                if (actualMaxLine == 1) {
                    outputLines.add(lines.get(0));
                } else {
                    // 正常显示所有行
                    outputLines.addAll(lines);
                }
            }

            // 实际行高使用
            final float actualLineHeight = args.fontSize() > 0 ? args.fontSize() : font.lineHeight;
            float totalHeight = outputLines.size() * actualLineHeight;

            // 预处理：处理省略号和计算宽度，避免在循环中重复计算
            // 使用数组存储处理后的行和对应的宽度，减少重复计算
            String[] processedLines = new String[outputLines.size()];
            int[] lineWidths = new int[outputLines.size()];
            int maxLineWidth = 0;

            for (int i = 0; i < outputLines.size(); i++) {
                String line = outputLines.get(i);
                // 若宽度超出可用宽度，进行截断并加省略号
                line = ellipsisString(args, ellipsis, font, ellipsisWidth, availableWidth, line);
                processedLines[i] = line;
                int width = font.width(line);
                lineWidths[i] = width;
                if (width > maxLineWidth) {
                    maxLineWidth = width;
                }
            }

            if (availableWidth > 0) {
                maxLineWidth = Math.min(maxLineWidth, availableWidth);
            }

            // 绘制背景
            GuiGraphics graphics = text.graphics();
            if (args.bgArgb() != 0) {
                int bgX = (int) args.x();
                int bgY = (int) args.y();
                // 背景宽度需要考虑缩放后的文本宽度
                int bgWidth = (int) (maxLineWidth * scale) + args.paddingLeft() + args.paddingRight();
                int bgHeight = (int) (totalHeight + args.paddingTop() + args.paddingBottom());

                // 绘制圆角矩形背景
                AbstractGuiUtils.drawRoundedRect(graphics, bgX, bgY, bgWidth, bgHeight, args.bgArgb(), args.bgBorderRadius());

                // 绘制边框
                if (args.bgBorderThickness() > 0) {
                    int borderArgb = ColorUtils.softenArgb(args.bgArgb());

                    AbstractGuiUtils.drawRoundedRectOutLineRough(graphics, bgX, bgY, bgWidth, bgHeight, args.bgBorderThickness(), borderArgb, args.bgBorderRadius());
                }
            }

            // 应用缩放变换
            boolean needsScale = Math.abs(scale - 1.0f) > 0.001f;
            if (needsScale) {
                graphics.pose().pushPose();
                // 移动到绘制起始位置
                graphics.pose().translate(drawX, drawY, 0);
                // 应用缩放
                graphics.pose().scale(scale, scale, 1.0f);
                // 调整绘制坐标
                drawX = 0;
                drawY = 0;
            }

            // 复用 Text 对象，减少对象创建
            Text textTemplate = text.copyWithoutChildren();
            EnumAlignment alignment = args.align() != null ? args.align() : text.align();
            float alignWidth = availableWidth > 0 ? availableWidth : maxLineWidth;
            boolean hasShadow = text.shadow();
            int textColor = text.colorArgb();
            boolean hasBgColor = !text.bgColorEmpty();
            int bgColor = hasBgColor ? text.bgColorArgb() : 0;

            // 绘制文本
            for (int index = 0; index < processedLines.length; index++) {
                String line = processedLines[index];
                int lineWidth = lineWidths[index];

                // 计算水平偏移
                float xOffset;
                switch (alignment) {
                    case CENTER:
                        xOffset = (alignWidth - lineWidth) / 2.0f;
                        break;
                    case END:
                        xOffset = alignWidth - lineWidth;
                        break;
                    default:
                        xOffset = 0;
                        break;
                }

                // 计算垂直位置
                float yPos;
                if (needsScale) {
                    yPos = (float) drawY + index * font.lineHeight;
                } else {
                    yPos = (float) drawY + index * actualLineHeight;
                }

                // 绘制文本背景
                if (hasBgColor) {
                    if (needsScale) {
                        AbstractGuiUtils.fill(graphics, (int) (xOffset), (int) (yPos), lineWidth, font.lineHeight, bgColor);
                    } else {
                        AbstractGuiUtils.fill(graphics, (int) (drawX + xOffset), (int) (yPos), lineWidth, font.lineHeight, bgColor);
                    }
                }

                // 绘制文本 - 复用 Text 对象，只更新文本内容
                Text lineText = textTemplate.text(line);
                graphics.drawString(font, lineText.toComponent().toTextComponent(I18nUtils.getClientLanguage()), (int) (drawX + xOffset), (int) yPos, textColor, hasShadow);
            }

            // 恢复矩阵状态
            if (needsScale) {
                graphics.pose().popPose();
            }
        }
    }

    // endregion 绘制文字


    // region 绘制图标

    /**
     * 绘制效果图标
     *
     * @param effectInstance 待绘制的效果实例
     * @param x              矩形的左上角x坐标
     * @param y              矩形的左上角y坐标
     * @param width          目标矩形的宽度，决定了图像在屏幕上的宽度
     * @param height         目标矩形的高度，决定了图像在屏幕上的高度
     * @param showText       是否显示效果等级和持续时间
     */
    public static void drawEffectIcon(GuiGraphics graphics, MobEffectInstance effectInstance, int x, int y, int width, int height, boolean showText) {
        AbstractGuiUtils.drawEffectIcon(graphics, getFont(), effectInstance, x, y, width, height, showText);
    }

    /**
     * 绘制效果图标
     *
     * @param effectInstance 待绘制的效果实例
     * @param x              矩形的左上角x坐标
     * @param y              矩形的左上角y坐标
     * @param width          目标矩形的宽度，决定了图像在屏幕上的宽度
     * @param height         目标矩形的高度，决定了图像在屏幕上的高度
     * @param showText       是否显示效果等级和持续时间
     */
    public static void drawEffectIcon(GuiGraphics graphics, Font font, MobEffectInstance effectInstance, int x, int y, int width, int height, boolean showText) {
        ResourceLocation effectIcon = TextureUtils.getEffectTexture(effectInstance);
        if (effectIcon != null) {
            AbstractGuiUtils.blit(graphics, effectIcon, x, y, 0, 0, width, height, width, height);
        }
        if (showText) {
            // 效果等级
            if (effectInstance.getAmplifier() >= 0) {
                Component amplifierString = Component.literal(NumberUtils.intToRoman(effectInstance.getAmplifier() + 1));
                int amplifierWidth = font.width(amplifierString.toString());
                float fontX = x + width - (float) amplifierWidth / 2;
                float fontY = y - 1;
                int argb = 0xFFFFFFFF;
                graphics.drawString(font, amplifierString.color(Color.argb(argb)).toTextComponent(), (int) fontX, (int) fontY, argb, true);
            }
            // 效果持续时间
            if (effectInstance.getDuration() > 0) {
                Component durationString = Component.literal(DateUtils.toMaxUnitString(effectInstance.getDuration(), DateUtils.DateUnit.SECOND, 0, 1));
                int durationWidth = font.width(durationString.toString());
                float fontX = x + width - (float) durationWidth / 2 - 2;
                float fontY = y + (float) height / 2 + 1;
                int argb = 0xFFFFFFFF;
                graphics.drawString(font, durationString.color(Color.argb(argb)).toTextComponent(), (int) fontX, (int) fontY, argb, true);
            }
        }
    }

    /**
     * 绘制效果图标
     *
     * @param effectInstance 待绘制的效果实例
     * @param x              矩形的左上角x坐标
     * @param y              矩形的左上角y坐标
     * @param showText       是否显示效果等级和持续时间
     */
    public static void drawEffectIcon(GuiGraphics graphics, MobEffectInstance effectInstance, int x, int y, boolean showText) {
        AbstractGuiUtils.drawEffectIcon(graphics, getFont(), effectInstance, x, y, showText);
    }

    /**
     * 绘制效果图标
     *
     * @param effectInstance 待绘制的效果实例
     * @param x              矩形的左上角x坐标
     * @param y              矩形的左上角y坐标
     * @param showText       是否显示效果等级和持续时间
     */
    public static void drawEffectIcon(GuiGraphics graphics, Font font, MobEffectInstance effectInstance, int x, int y, boolean showText) {
        AbstractGuiUtils.drawEffectIcon(graphics, font, effectInstance, x, y, ITEM_ICON_SIZE, ITEM_ICON_SIZE, showText);
    }

    public static void renderItem(GuiGraphics graphics, ItemRenderer itemRenderer, ItemStack itemStack, int x, int y, boolean showText) {
        AbstractGuiUtils.renderItem(graphics, itemRenderer, getFont(), itemStack, x, y, showText);
    }

    public static void renderItem(GuiGraphics graphics, ItemRenderer itemRenderer, Font font, ItemStack itemStack, int x, int y, boolean showText) {
        graphics.renderFakeItem(itemStack, x, y);
        if (showText) {
            graphics.renderItemDecorations(font, itemStack, x, y, String.valueOf(itemStack.getCount()));
        }
    }

    //  endregion 绘制图标


    //  region 绘制形状

    /**
     * 统一绘制形状方法
     */
    public static void drawShape(ShapeDrawArgs args) {
        if (args == null || args.graphics() == null) return;

        switch (args.type()) {
            case RECT:
                drawRectShape(args);
                break;
            case CIRCLE:
                drawCircleShape(args);
                break;
            case ELLIPSE:
                drawEllipseShape(args);
                break;
            case SECTOR:
                drawSectorShape(args);
                break;
            case SECTOR_RING:
                drawSectorRingShape(args);
                break;
            case POLYGON:
                drawPolygonShape(args);
                break;
        }
    }

    /**
     * 绘制矩形形状
     */
    private static void drawRectShape(ShapeDrawArgs args) {
        ShapeDrawArgs.RectParams rect = args.rect();
        GuiGraphics graphics = args.graphics();
        int color = args.color();

        if (rect.border() > 0) {
            if (rect.hasRadius()) {
                ShapeDrawArgs.RoundedCornerMode mode = rect.cornerMode();
                drawRoundedRectOutLine(graphics, rect.x(), rect.y(), rect.width(), rect.height(),
                        rect.topLeft(), rect.topRight(), rect.bottomLeft(), rect.bottomRight(),
                        rect.border(), color, mode);
            } else {
                fillOutLine(graphics, (int) rect.x(), (int) rect.y(), (int) rect.width(), (int) rect.height(), (int) rect.border(), color);
            }
        } else {
            if (rect.hasRadius() && rect.isUniformRadius()) {
                ShapeDrawArgs.RoundedCornerMode mode = args.rect().cornerMode();
                if (mode == ShapeDrawArgs.RoundedCornerMode.ROUGH || (mode == ShapeDrawArgs.RoundedCornerMode.AUTO && rect.topLeft() <= 10)) {
                    drawRoundedRect(graphics, (int) rect.x(), (int) rect.y(), (int) rect.width(), (int) rect.height(), color, (int) rect.topLeft());
                } else {
                    drawRoundedRect(graphics, rect.x(), rect.y(), rect.width(), rect.height(), rect.topLeft(), color);
                }
            } else if (rect.hasRadius() && !rect.isUniformRadius()) {
                float maxRadius = Math.max(Math.max(rect.topLeft(), rect.topRight()), Math.max(rect.bottomLeft(), rect.bottomRight()));
                ShapeDrawArgs.RoundedCornerMode mode = args.rect().cornerMode();
                if (mode == ShapeDrawArgs.RoundedCornerMode.ROUGH || (mode == ShapeDrawArgs.RoundedCornerMode.AUTO && maxRadius <= 10)) {
                    drawRoundedRect(graphics, (int) rect.x(), (int) rect.y(), (int) rect.width(), (int) rect.height(), color, (int) maxRadius);
                } else {
                    drawRoundedRect(graphics, rect.x(), rect.y(), rect.width(), rect.height(), rect.topLeft(), rect.topRight(), rect.bottomLeft(), rect.bottomRight(), color);
                }
            } else {
                fill(graphics, (int) rect.x(), (int) rect.y(), (int) rect.width(), (int) rect.height(), color);
            }
        }
    }

    /**
     * 绘制圆形形状
     */
    private static void drawCircleShape(ShapeDrawArgs args) {
        ShapeDrawArgs.CircleParams circle = args.circle();
        GuiGraphics graphics = args.graphics();
        int color = args.color();

        int segments = circle.segments();
        if (segments <= 0) {
            segments = calculateCircleSegments(circle.radius());
        }

        if (circle.border() > 0) {
            drawCircleRing(graphics, circle.centerX(), circle.centerY(), circle.radius(), circle.border(), segments, color);
        } else {
            drawCircle(graphics, circle.centerX(), circle.centerY(), circle.radius(), segments, color);
        }
    }

    /**
     * 绘制椭圆形状
     */
    private static void drawEllipseShape(ShapeDrawArgs args) {
        ShapeDrawArgs.EllipseParams ellipse = args.ellipse();
        GuiGraphics graphics = args.graphics();
        int color = args.color();

        int segments = ellipse.segments();
        if (segments <= 0) {
            float maxRadius = Math.max(ellipse.radiusX(), ellipse.radiusY());
            segments = calculateCircleSegments(maxRadius);
        }

        if (ellipse.border() > 0) {
            if (ellipse.rotation() != 0) {
                drawEllipseRing(graphics, ellipse.centerX(), ellipse.centerY(), ellipse.radiusX(), ellipse.radiusY(), ellipse.rotation(), ellipse.border(), segments, color);
            } else {
                drawEllipseRing(graphics, ellipse.centerX(), ellipse.centerY(), ellipse.radiusX(), ellipse.radiusY(), ellipse.border(), segments, color);
            }
        } else {
            if (ellipse.rotation() != 0) {
                drawEllipseRad(graphics, ellipse.centerX(), ellipse.centerY(), ellipse.radiusX(), ellipse.radiusY(), Math.toRadians(ellipse.rotation()), segments, color);
            } else {
                drawEllipse(graphics, ellipse.centerX(), ellipse.centerY(), ellipse.radiusX(), ellipse.radiusY(), segments, color);
            }
        }
    }

    /**
     * 绘制多边形形状
     */
    private static void drawPolygonShape(ShapeDrawArgs args) {
        ShapeDrawArgs.PolygonParams polygon = args.polygon();
        GuiGraphics graphics = args.graphics();
        int color = args.color();

        if (polygon.border() > 0) {
            drawPolygonBorder(graphics, polygon, color);
        } else {
            drawPolygon(graphics, polygon.centerX(), polygon.centerY(), polygon.radius(),
                    polygon.sides(), polygon.rotation(), color);
        }
    }

    /**
     * 绘制扇形形状
     */
    private static void drawSectorShape(ShapeDrawArgs args) {
        ShapeDrawArgs.SectorParams sector = args.sector();
        GuiGraphics graphics = args.graphics();
        int color = args.color();

        int segments = sector.segments();
        if (segments <= 0) {
            segments = calculateCircleSegments(sector.radius());
        }

        if (sector.useRadians()) {
            drawSectorRad(graphics, sector.centerX(), sector.centerY(), sector.radius(), sector.startAngle(), sector.endAngle(), segments, color);
        } else {
            drawSector(graphics, sector.centerX(), sector.centerY(), sector.radius(), sector.startAngle(), sector.endAngle(), segments, color);
        }
    }

    /**
     * 绘制有宽度的线段
     */
    public static void drawLine(GuiGraphics graphics, float x1, float y1, float x2, float y2, float lineWidth, int color) {
        if (lineWidth <= 0) return;

        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= 0) return;

        float nx = dx / length;
        float ny = dy / length;
        float perpX = -ny;
        float perpY = nx;
        float halfWidth = lineWidth * 0.5f;

        // 计算四个顶点
        float x1Top = x1 + perpX * halfWidth;
        float y1Top = y1 + perpY * halfWidth;
        float x1Bottom = x1 - perpX * halfWidth;
        float y1Bottom = y1 - perpY * halfWidth;
        float x2Top = x2 + perpX * halfWidth;
        float y2Top = y2 + perpY * halfWidth;
        float x2Bottom = x2 - perpX * halfWidth;
        float y2Bottom = y2 - perpY * halfWidth;

        setupBlendRender();

        Matrix4f m4 = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        addVertexWithColor(builder, m4, x1Top, y1Top, 0, color);
        addVertexWithColor(builder, m4, x2Top, y2Top, 0, color);
        addVertexWithColor(builder, m4, x1Bottom, y1Bottom, 0, color);

        addVertexWithColor(builder, m4, x2Top, y2Top, 0, color);
        addVertexWithColor(builder, m4, x2Bottom, y2Bottom, 0, color);
        addVertexWithColor(builder, m4, x1Bottom, y1Bottom, 0, color);

        finishBlendRender(builder);
    }

    /**
     * 绘制扇环形状
     */
    private static void drawSectorRingShape(ShapeDrawArgs args) {
        ShapeDrawArgs.SectorRingParams params = args.sectorRing();
        GuiGraphics graphics = args.graphics();
        int color = args.color();

        int segments = params.segments();
        if (segments <= 0) {
            segments = calculateCircleSegments(params.outerRadius());
        }

        float actualInnerRadius = params.getActualInnerRadius();

        if (actualInnerRadius > 0) {
            drawFilledSectorRing(graphics, params, actualInnerRadius, segments, color);
        } else {
            drawFilledSectorRingFromCenter(graphics, params, segments, color);
        }
    }

    /**
     * 绘制实心扇环
     */
    private static void drawFilledSectorRing(GuiGraphics graphics, ShapeDrawArgs.SectorRingParams params, float innerRadius, int segments, int color) {
        float centerX = params.centerX();
        float centerY = params.centerY();
        float outerRadius = params.outerRadius();
        double startAngle = params.startAngle();
        double endAngle = params.endAngle();
        boolean useRadians = params.useRadians();

        if (innerRadius >= outerRadius || innerRadius < 0) return;

        double startRad, endRad;
        if (useRadians) {
            startRad = startAngle;
            endRad = endAngle;
        } else {
            startRad = Math.toRadians(startAngle);
            endRad = Math.toRadians(endAngle);
        }

        Matrix4f m4 = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        double angleRange = endRad - startRad;
        if (angleRange < 0) angleRange += 2.0 * Math.PI;
        double angleStep = angleRange / segments;

        for (int i = 0; i <= segments; i++) {
            double angle = startRad + i * angleStep;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            addVertexWithColor(builder, m4, centerX + cos * outerRadius, centerY + sin * outerRadius, 0, color);
            addVertexWithColor(builder, m4, centerX + cos * innerRadius, centerY + sin * innerRadius, 0, color);
        }

        setupBlendRender();
        finishBlendRender(builder);
    }

    /**
     * 绘制实心扇环
     */
    private static void drawFilledSectorRingFromCenter(GuiGraphics graphics, ShapeDrawArgs.SectorRingParams params, int segments, int color) {
        float centerX = params.centerX();
        float centerY = params.centerY();
        float radius = params.outerRadius();
        double startAngle = params.startAngle();
        double endAngle = params.endAngle();
        boolean useRadians = params.useRadians();

        double startRad, endRad;
        if (useRadians) {
            startRad = startAngle;
            endRad = endAngle;
        } else {
            startRad = Math.toRadians(startAngle);
            endRad = Math.toRadians(endAngle);
        }

        Matrix4f m4 = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        double angleRange = endRad - startRad;
        if (angleRange < 0) angleRange += 2.0 * Math.PI;
        double angleStep = angleRange / segments;

        float[] xCoords = new float[segments + 1];
        float[] yCoords = new float[segments + 1];
        for (int i = 0; i <= segments; i++) {
            double angle = startRad + i * angleStep;
            xCoords[i] = centerX + (float) (Math.cos(angle) * radius);
            yCoords[i] = centerY + (float) (Math.sin(angle) * radius);
        }

        addVertexWithColor(builder, m4, xCoords[0], yCoords[0], 0, color);
        addVertexWithColor(builder, m4, centerX, centerY, 0, color);

        for (int i = 1; i <= segments; i++) {
            addVertexWithColor(builder, m4, xCoords[i], yCoords[i], 0, color);
            if (i < segments) {
                addVertexWithColor(builder, m4, centerX, centerY, 0, color);
            }
        }

        setupBlendRender();
        finishBlendRender(builder);
    }

    // endregion 绘制形状


    // region 绘制矩形

    /**
     * 绘制像素点
     *
     * @param x    像素的 X 坐标
     * @param y    像素的 Y 坐标
     * @param argb 像素的颜色
     */
    public static void drawPixel(GuiGraphics graphics, int x, int y, int argb) {
        graphics.fill(x, y, x + 1, y + 1, argb);
    }

    /**
     * 绘制正方形
     */
    public static void fill(GuiGraphics graphics, int x, int y, int width, int argb) {
        AbstractGuiUtils.fill(graphics, x, y, width, width, argb);
    }

    /**
     * 绘制矩形
     */
    public static void fill(GuiGraphics graphics, int x, int y, int width, int height, int argb) {
        AbstractGuiUtils.drawRoundedRect(graphics, x, y, width, height, argb, 0);
    }

    /**
     * 绘制矩形
     */
    public static void fillEx(GuiGraphics graphics, float x, float y, float width, float height, int color) {
        if (width <= 0 || height <= 0) return;

        setupBlendRender();

        Matrix4f m4 = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        addVertexWithColor(builder, m4, x + width, y, 0, color);
        addVertexWithColor(builder, m4, x, y, 0, color);
        addVertexWithColor(builder, m4, x, y + height, 0, color);
        addVertexWithColor(builder, m4, x + width, y + height, 0, color);

        finishBlendRender(builder);
    }

    /**
     * 绘制矩形边框
     *
     * @param thickness 边框厚度
     * @param argb      边框颜色
     */
    public static void fillOutLine(GuiGraphics graphics, int x, int y, int width, int height, int thickness, int argb) {
        // 上边
        AbstractGuiUtils.fill(graphics, x, y, width - thickness, thickness, argb);
        // 下边
        AbstractGuiUtils.fill(graphics, x + thickness, y + height - thickness, width - thickness, thickness, argb);
        // 左边
        AbstractGuiUtils.fill(graphics, x, y + thickness, thickness, height - thickness, argb);
        // 右边
        AbstractGuiUtils.fill(graphics, x + width - thickness, y, thickness, height - thickness, argb);
    }

    /**
     * 绘制圆角矩形
     *
     * @param x      矩形的左上角X坐标
     * @param y      矩形的左上角Y坐标
     * @param width  矩形的宽度
     * @param height 矩形的高度
     * @param argb   矩形的颜色
     * @param radius 圆角半径(0-10)
     */
    public static void drawRoundedRect(GuiGraphics graphics, int x, int y, int width, int height, int argb, int radius) {
        if (radius <= 0) {
            graphics.fill(x, y, x + width, y + height, argb);
            return;
        }

        // 限制半径最大值为10
        radius = Math.min(radius, 10);

        // 绘制中心矩形
        AbstractGuiUtils.fill(graphics, x + radius + 1, y + radius + 1, width - 2 * (radius + 1), height - 2 * (radius + 1), argb);

        // 绘制四条边
        // 上边
        AbstractGuiUtils.fill(graphics, x + radius + 1, y, width - 2 * radius - 2, radius, argb);
        AbstractGuiUtils.fill(graphics, x + radius + 1, y + radius, width - 2 * (radius + 1), 1, argb);
        // 下边
        AbstractGuiUtils.fill(graphics, x + radius + 1, y + height - radius, width - 2 * radius - 2, radius, argb);
        AbstractGuiUtils.fill(graphics, x + radius + 1, y + height - radius - 1, width - 2 * (radius + 1), 1, argb);
        // 左边
        AbstractGuiUtils.fill(graphics, x, y + radius + 1, radius, height - 2 * radius - 2, argb);
        AbstractGuiUtils.fill(graphics, x + radius, y + radius + 1, 1, height - 2 * (radius + 1), argb);
        // 右边
        AbstractGuiUtils.fill(graphics, x + width - radius, y + radius + 1, radius, height - 2 * radius - 2, argb);
        AbstractGuiUtils.fill(graphics, x + width - radius - 1, y + radius + 1, 1, height - 2 * (radius + 1), argb);

        // 绘制四个圆角
        // 左上角
        AbstractGuiUtils.drawCircleQuadrant(graphics, x + radius, y + radius, radius, argb, 1);
        // 右上角
        AbstractGuiUtils.drawCircleQuadrant(graphics, x + width - radius - 1, y + radius, radius, argb, 2);
        // 左下角
        AbstractGuiUtils.drawCircleQuadrant(graphics, x + radius, y + height - radius - 1, radius, argb, 3);
        // 右下角
        AbstractGuiUtils.drawCircleQuadrant(graphics, x + width - radius - 1, y + height - radius - 1, radius, argb, 4);
    }

    /**
     * 绘制四分之一圆
     *
     * @param centerX  圆角中心点X坐标
     * @param centerY  圆角中心点Y坐标
     * @param radius   圆角半径
     * @param argb     圆角颜色
     * @param quadrant 指定绘制的象限（1=左上，2=右上，3=左下，4=右下）
     */
    private static void drawCircleQuadrant(GuiGraphics graphics, int centerX, int centerY, int radius, int argb, int quadrant) {
        for (int dx = 0; dx <= radius; dx++) {
            for (int dy = 0; dy <= radius; dy++) {
                if (dx * dx + dy * dy <= radius * radius) {
                    drawCircleQuadrantPixel(graphics, centerX, centerY, argb, quadrant, dx, dy);
                }
            }
        }
    }

    private static void drawCircleQuadrantPixel(GuiGraphics graphics, int centerX, int centerY, int argb, int quadrant, int dx, int dy) {
        switch (quadrant) {
            case 1: // 左上角
                AbstractGuiUtils.drawPixel(graphics, centerX - dx, centerY - dy, argb);
                break;
            case 2: // 右上角
                AbstractGuiUtils.drawPixel(graphics, centerX + dx, centerY - dy, argb);
                break;
            case 3: // 左下角
                AbstractGuiUtils.drawPixel(graphics, centerX - dx, centerY + dy, argb);
                break;
            case 4: // 右下角
                AbstractGuiUtils.drawPixel(graphics, centerX + dx, centerY + dy, argb);
                break;
        }
    }

    /**
     * 绘制圆角矩形边框
     *
     * @param x         矩形左上角X坐标
     * @param y         矩形左上角Y坐标
     * @param width     矩形宽度
     * @param height    矩形高度
     * @param thickness 边框厚度
     * @param argb      边框颜色
     * @param radius    圆角半径（0-10）
     */
    public static void drawRoundedRectOutLineRough(GuiGraphics graphics, int x, int y, int width, int height, int thickness, int argb, int radius) {
        drawRoundedRectOutLineRough(graphics, x, y, width, height, thickness, argb, radius, radius, radius, radius);
    }

    /**
     * 绘制圆角矩形边框（粗糙模式，支持四个不同的圆角半径）
     */
    public static void drawRoundedRectOutLineRough(GuiGraphics graphics, int x, int y, int width, int height, int thickness, int argb,
                                                   int topLeft, int topRight, int bottomLeft, int bottomRight) {
        if (thickness <= 0) return;

        float halfW = width * 0.5f;
        float halfH = height * 0.5f;
        topLeft = (int) Math.min(Math.min(topLeft, 10), Math.min(halfW, halfH));
        topRight = (int) Math.min(Math.min(topRight, 10), Math.min(halfW, halfH));
        bottomLeft = (int) Math.min(Math.min(bottomLeft, 10), Math.min(halfW, halfH));
        bottomRight = (int) Math.min(Math.min(bottomRight, 10), Math.min(halfW, halfH));

        if (topLeft <= 0 && topRight <= 0 && bottomLeft <= 0 && bottomRight <= 0) {
            AbstractGuiUtils.fillOutLine(graphics, x, y, width, height, thickness, argb);
            return;
        }

        // 绘制四条内边
        // 上边
        int topStartX = topLeft > 0 ? x + topLeft + 1 : x + thickness;
        int topEndX = topRight > 0 ? x + width - topRight - 1 : x + width - thickness;
        if (topEndX > topStartX) {
            AbstractGuiUtils.fill(graphics, topStartX, y, topEndX - topStartX, thickness, argb);
        }

        // 下边
        int bottomStartX = bottomLeft > 0 ? x + bottomLeft + 1 : x + thickness;
        int bottomEndX = bottomRight > 0 ? x + width - bottomRight - 1 : x + width - thickness;
        if (bottomEndX > bottomStartX) {
            AbstractGuiUtils.fill(graphics, bottomStartX, y + height - thickness, bottomEndX - bottomStartX, thickness, argb);
        }

        // 左边
        int leftStartY = topLeft > 0 ? y + topLeft + 1 : y + thickness;
        int leftEndY = bottomLeft > 0 ? y + height - bottomLeft - 1 : y + height - thickness;
        if (leftEndY > leftStartY) {
            AbstractGuiUtils.fill(graphics, x, leftStartY, thickness, leftEndY - leftStartY, argb);
        }

        // 右边
        int rightStartY = topRight > 0 ? y + topRight + 1 : y + thickness;
        int rightEndY = bottomRight > 0 ? y + height - bottomRight - 1 : y + height - thickness;
        if (rightEndY > rightStartY) {
            AbstractGuiUtils.fill(graphics, x + width - thickness, rightStartY, thickness, rightEndY - rightStartY, argb);
        }

        // 绘制四个角
        // 左上角
        if (topLeft == 0) {
            AbstractGuiUtils.fill(graphics, x, y, thickness, thickness, argb);
        } else {
            drawCircleBorder(graphics, x + topLeft, y + topLeft, topLeft, thickness, argb, 1);
        }
        // 右上角
        if (topRight == 0) {
            AbstractGuiUtils.fill(graphics, x + width - thickness, y, thickness, thickness, argb);
        } else {
            drawCircleBorder(graphics, x + width - topRight - 1, y + topRight, topRight, thickness, argb, 2);
        }
        // 左下角
        if (bottomLeft == 0) {
            AbstractGuiUtils.fill(graphics, x, y + height - thickness, thickness, thickness, argb);
        } else {
            drawCircleBorder(graphics, x + bottomLeft, y + height - bottomLeft - 1, bottomLeft, thickness, argb, 3);
        }
        // 右下角
        if (bottomRight == 0) {
            AbstractGuiUtils.fill(graphics, x + width - thickness, y + height - thickness, thickness, thickness, argb);
        } else {
            drawCircleBorder(graphics, x + width - bottomRight - 1, y + height - bottomRight - 1, bottomRight, thickness, argb, 4);
        }
    }

    /**
     * 绘制圆角边框
     *
     * @param centerX   圆角中心点X坐标
     * @param centerY   圆角中心点Y坐标
     * @param radius    圆角半径
     * @param thickness 边框厚度
     * @param argb      边框颜色
     * @param quadrant  指定绘制的象限（1=左上，2=右上，3=左下，4=右下）
     */
    private static void drawCircleBorder(GuiGraphics graphics, int centerX, int centerY, int radius, int thickness, int argb, int quadrant) {
        for (int dx = 0; dx <= radius; dx++) {
            for (int dy = 0; dy <= radius; dy++) {
                double sqrt = Math.sqrt(dx * dx + dy * dy);
                if (sqrt <= radius && sqrt >= radius - thickness) {
                    drawCircleQuadrantPixel(graphics, centerX, centerY, argb, quadrant, dx, dy);
                }
            }
        }
    }

    /**
     * 绘制非统一圆角矩形边框
     * 分别绘制矩形内边（原边长-两端圆角半径之和），再判断四个角是否有半径，若有则绘制，若无则补齐直角边（不能重叠）
     *
     * @param x           矩形左上角X坐标
     * @param y           矩形左上角Y坐标
     * @param width       矩形宽度
     * @param height      矩形高度
     * @param topLeft     左上角圆角半径
     * @param topRight    右上角圆角半径
     * @param bottomLeft  左下角圆角半径
     * @param bottomRight 右下角圆角半径
     * @param border      边框厚度
     * @param color       边框颜色
     */
    private static void drawRoundedRectOutLine(GuiGraphics graphics, float x, float y, float width, float height,
                                               float topLeft, float topRight, float bottomLeft, float bottomRight,
                                               float border, int color, ShapeDrawArgs.RoundedCornerMode mode) {
        if (border <= 0) return;

        // 计算绘制模式
        float maxRadius = Math.max(Math.max(topLeft, topRight), Math.max(bottomLeft, bottomRight));
        boolean useRough = false;
        if (mode == ShapeDrawArgs.RoundedCornerMode.ROUGH) {
            useRough = true;
        } else if (mode == ShapeDrawArgs.RoundedCornerMode.AUTO) {
            useRough = maxRadius <= 10;
        }

        // 粗糙模式
        if (useRough) {
            drawRoundedRectOutLineRough(graphics, (int) x, (int) y, (int) width, (int) height,
                    (int) border, color, (int) topLeft, (int) topRight, (int) bottomLeft, (int) bottomRight);
        }
        // 精细模式
        else {
            drawRoundedRectOutLineFine(graphics, x, y, width, height,
                    topLeft, topRight, bottomLeft, bottomRight, border, color);
        }
    }

    /**
     * 绘制圆角矩形边框
     */
    private static void drawRoundedRectOutLineFine(GuiGraphics graphics, float x, float y, float width, float height,
                                                   float topLeft, float topRight, float bottomLeft, float bottomRight,
                                                   float border, int color) {
        if (border <= 0) return;

        // 限制圆角半径
        float halfW = width * 0.5f;
        float halfH = height * 0.5f;
        topLeft = clampRadius(topLeft, halfW, halfH);
        topRight = clampRadius(topRight, halfW, halfH);
        bottomLeft = clampRadius(bottomLeft, halfW, halfH);
        bottomRight = clampRadius(bottomRight, halfW, halfH);

        if (topLeft <= 0 && topRight <= 0 && bottomLeft <= 0 && bottomRight <= 0) {
            fillOutLine(graphics, (int) x, (int) y, (int) width, (int) height, (int) border, color);
            return;
        }

        // 上边
        float topStartX;
        // 左上角为实心扇形
        if (topLeft > 0 && topLeft <= border) {
            topStartX = (x + border);
        }
        // 左上角为扇环
        else if (topLeft > 0) {
            topStartX = (x + topLeft);
        }
        // 左上角为直角
        else {
            topStartX = (x + border);
        }

        float topEndX;
        if (topRight > 0 && topRight <= border) {
            topEndX = (x + width - border);
        } else if (topRight > 0) {
            topEndX = (x + width - topRight);
        } else {
            topEndX = (x + width);
        }
        if (topEndX > topStartX) {
            fillEx(graphics, topStartX, y, topEndX - topStartX, border, color);
        }

        // 下边
        float bottomStartX;
        if (bottomLeft > 0 && bottomLeft <= border) {
            bottomStartX = (x + border);
        } else if (bottomLeft > 0) {
            bottomStartX = (x + bottomLeft);
        } else {
            bottomStartX = (x);
        }

        float bottomEndX;
        if (bottomRight > 0 && bottomRight <= border) {
            bottomEndX = (x + width - border);
        } else if (bottomRight > 0) {
            bottomEndX = (x + width - bottomRight);
        } else {
            bottomEndX = (x + width - border);
        }
        if (bottomEndX > bottomStartX) {
            fillEx(graphics, bottomStartX, (y + height - border), bottomEndX - bottomStartX, border, color);
        }

        // 左边
        float leftStartY;
        if (topLeft > 0 && topLeft <= border) {
            leftStartY = (y + border);
        } else if (topLeft > 0) {
            leftStartY = (y + topLeft);
        } else {
            leftStartY = (y);
        }

        float leftEndY;
        if (bottomLeft > 0 && bottomLeft <= border) {
            leftEndY = (y + height - border);
        } else if (bottomLeft > 0) {
            leftEndY = (y + height - bottomLeft);
        } else {
            leftEndY = (y + height - border);
        }
        if (leftEndY > leftStartY) {
            fillEx(graphics, x, leftStartY, border, leftEndY - leftStartY, color);
        }

        // 右边
        float rightStartY;
        if (topRight > 0 && topRight <= border) {
            rightStartY = (y + border);
        } else if (topRight > 0) {
            rightStartY = (y + topRight);
        } else {
            rightStartY = (y + border);
        }

        float rightEndY;
        if (bottomRight > 0 && bottomRight <= border) {
            rightEndY = (y + height - border);
        } else if (bottomRight > 0) {
            rightEndY = (y + height - bottomRight);
        } else {
            rightEndY = (y + height);
        }
        if (rightEndY > rightStartY) {
            fillEx(graphics, (x + width - border), rightStartY, border, rightEndY - rightStartY, color);
        }

        // 绘制四个圆角
        // 计算分段数
        float maxRadius = Math.max(Math.max(topLeft, topRight), Math.max(bottomLeft, bottomRight));
        int segments = calculateOptimalSegments(maxRadius);

        // 左上角
        if (topLeft > 0) {
            float centerX = x + topLeft;
            float centerY = y + topLeft;
            if (topLeft <= border) {
                // 绘制实心扇形
                drawSector(graphics, centerX, centerY, topLeft, 180, 270, segments, color);
                // 填充边与圆角之间的连接区域
                float radiusSize = topLeft;
                float remainingSize = border - radiusSize;
                fillEx(graphics, centerX, centerY - radiusSize, remainingSize, radiusSize, color);
                fillEx(graphics, centerX - radiusSize, centerY, radiusSize, remainingSize, color);
                fillEx(graphics, centerX, centerY, remainingSize, remainingSize, color);
            } else {
                // 绘制扇环
                drawSectorRing(graphics, centerX, centerY, topLeft, 180, 270, border, segments, color);
            }
        }

        // 右上角
        if (topRight > 0) {
            float centerX = x + width - topRight;
            float centerY = y + topRight;
            if (topRight <= border) {
                // 绘制实心扇形
                drawSector(graphics, centerX, centerY, topRight, 270, 360, segments, color);
                // 填充边与圆角之间的连接区域
                float radiusSize = topRight;
                float remainingSize = border - radiusSize;
                fillEx(graphics, centerX - remainingSize, centerY - radiusSize, remainingSize, radiusSize, color);
                fillEx(graphics, centerX, centerY, radiusSize, remainingSize, color);
                fillEx(graphics, centerX - remainingSize, centerY, remainingSize, remainingSize, color);
            } else {
                // 绘制扇环
                drawSectorRing(graphics, centerX, centerY, topRight, 270, 360, border, segments, color);
            }
        }

        // 右下角
        if (bottomRight > 0) {
            float centerX = x + width - bottomRight;
            float centerY = y + height - bottomRight;
            if (bottomRight <= border) {
                // 绘制实心扇形
                drawSector(graphics, centerX, centerY, bottomRight, 0, 90, segments, color);
                // 填充边与圆角之间的连接区域
                float radiusSize = topRight;
                float remainingSize = border - radiusSize;
                fillEx(graphics, centerX, centerY - remainingSize, radiusSize, remainingSize, color);
                fillEx(graphics, centerX - remainingSize, centerY, remainingSize, radiusSize, color);
                fillEx(graphics, centerX - remainingSize, centerY - remainingSize, remainingSize, remainingSize, color);
            } else {
                // 绘制扇环
                drawSectorRing(graphics, centerX, centerY, bottomRight, 0, 90, border, segments, color);
            }
        }

        // 左下角
        if (bottomLeft > 0) {
            float centerX = x + bottomLeft;
            float centerY = y + height - bottomLeft;
            if (bottomLeft <= border) {
                // 绘制实心扇形
                drawSector(graphics, centerX, centerY, bottomLeft, 90, 180, segments, color);
                // 填充边与圆角之间的连接区域
                float radiusSize = topRight;
                float remainingSize = border - radiusSize;
                fillEx(graphics, centerX - radiusSize, centerY - remainingSize, radiusSize, remainingSize, color);
                fillEx(graphics, centerX, centerY, remainingSize, radiusSize, color);
                fillEx(graphics, centerX, centerY - remainingSize, remainingSize, remainingSize, color);
            } else {
                // 绘制扇环
                drawSectorRing(graphics, centerX, centerY, bottomLeft, 90, 180, border, segments, color);
            }
        }
    }

    // endregion 绘制矩形


    // region 绘制圆

    private static void setColor(BufferBuilder builder, int argb) {
        builder.setColor((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >> 24) & 0xFF);
    }

    private static void addVertexWithColor(BufferBuilder builder, Matrix4f m4, float x, float y, float z, int argb) {
        builder.addVertex(m4, x, y, z);
        setColor(builder, argb);
    }

    /**
     * 计算圆角分段数
     */
    private static int calculateOptimalSegments(float radius) {
        if (radius <= 0) return 0;
        return Math.min(Math.max(32, (int) (radius * 2.5f + 16)), 128);
    }

    /**
     * 计算圆形高质量分段数
     */
    private static int calculateCircleSegments(float radius) {
        if (radius <= 0) return 0;
        return Math.min(Math.max(64, (int) (radius * 6.0f + 32)), 256);
    }

    private static void setupBlendRender() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static void finishBlendRender(BufferBuilder builder) {
        BufferUploader.drawWithShader(builder.buildOrThrow());
        RenderSystem.disableBlend();
    }

    /**
     * 绘制圆角矩形框
     */
    public static void drawRoundedRect(GuiGraphics graphics, float x, float y, float w, float h, float r, int color) {
        drawRoundedRect(graphics, x, y, w, h, r, calculateOptimalSegments(r), color);
    }

    /**
     * 绘制带可变四角圆角的填充矩形
     */
    public static void drawRoundedRect(GuiGraphics graphics, float x, float y, float w, float h, float topLeft, float topRight, float bottomLeft, float bottomRight, int color) {
        float maxRadius = Math.max(Math.max(topLeft, topRight), Math.max(bottomLeft, bottomRight));
        drawRoundedRect(graphics, x, y, w, h, topLeft, topRight, bottomLeft, bottomRight, calculateOptimalSegments(maxRadius), color);
    }

    /**
     * 绘制带可变四角圆角的填充矩形
     */
    public static void drawRoundedRect(GuiGraphics graphics, float x, float y, float w, float h, float topLeft, float topRight, float bottomLeft, float bottomRight, int part, int color) {
        float halfW = w * 0.5f;
        float halfH = h * 0.5f;
        topLeft = clampRadius(topLeft, halfW, halfH);
        topRight = clampRadius(topRight, halfW, halfH);
        bottomLeft = clampRadius(bottomLeft, halfW, halfH);
        bottomRight = clampRadius(bottomRight, halfW, halfH);

        if (topLeft <= 0f && topRight <= 0f && bottomLeft <= 0f && bottomRight <= 0f) {
            fill(graphics, (int) x, (int) y, (int) (x + w), (int) (y + h), color);
            return;
        }
        if (part < 1) part = 1;

        List<float[]> verts = new ArrayList<>();
        verts.add(new float[]{x + topLeft, y});
        if (topRight > 0) addArc(verts, x + w - topRight, y + topRight, topRight, 270f, 360f, part);
        else verts.add(new float[]{x + w, y});

        verts.add(new float[]{x + w, y + topRight});
        if (bottomRight > 0) addArc(verts, x + w - bottomRight, y + h - bottomRight, bottomRight, 0f, 90f, part);
        else verts.add(new float[]{x + w, y + h});

        verts.add(new float[]{x + w - bottomRight, y + h});
        if (bottomLeft > 0) addArc(verts, x + bottomLeft, y + h - bottomLeft, bottomLeft, 90f, 180f, part);
        else verts.add(new float[]{x, y + h});

        verts.add(new float[]{x, y + h - bottomLeft});
        if (topLeft > 0) addArc(verts, x + topLeft, y + topLeft, topLeft, 180f, 270f, part);
        else verts.add(new float[]{x, y});

        Matrix4f m4 = graphics.pose().last().pose();
        setupBlendRender();
        RenderSystem.disableCull();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        float centerX = x + w * 0.5f;
        float centerY = y + h * 0.5f;

        if (verts.size() >= 2) {
            for (int i = 0; i < verts.size(); i++) {
                float[] a = verts.get(i);
                float[] b = verts.get((i + 1) % verts.size());
                addVertexWithColor(builder, m4, centerX, centerY, 0f, color);
                addVertexWithColor(builder, m4, a[0], a[1], 0f, color);
                addVertexWithColor(builder, m4, b[0], b[1], 0f, color);
            }
        }

        BufferUploader.drawWithShader(builder.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static float clampRadius(float r, float halfW, float halfH) {
        if (r <= 0f) return 0f;
        return Math.min(r, Math.min(halfW, halfH));
    }

    private static void addArc(List<float[]> verts, float cx, float cy, float r, float startDeg, float endDeg, int part) {
        for (int i = 0; i <= part; i++) {
            float t = (float) i / part;
            float ang = (float) Math.toRadians(startDeg + (endDeg - startDeg) * t);
            verts.add(new float[]{cx + (float) Math.cos(ang) * r, cy + (float) Math.sin(ang) * r});
        }
    }

    /**
     * 绘制圆角矩形
     */
    public static void drawRoundedRect(GuiGraphics graphics, float x, float y, float w, float h, float r, int part, int color) {
        drawRoundedRect(graphics, x, y, w, h, r, r, r, r, part, color);
    }

    /**
     * 绘制填充圆形
     */
    public static void drawCircle(GuiGraphics graphics, float centerX, float centerY, float radius, int color) {
        drawCircle(graphics, centerX, centerY, radius, calculateCircleSegments(radius), color);
    }

    /**
     * 绘制填充圆形
     */
    public static void drawCircle(GuiGraphics graphics, float centerX, float centerY, float radius, int segments, int color) {
        if (radius <= 0 || segments < 3) return;

        Matrix4f m4 = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        double angleStep = 2.0 * Math.PI / segments;
        float[] xCoords = new float[segments + 1];
        float[] yCoords = new float[segments + 1];
        for (int i = 0; i <= segments; i++) {
            double angle = i * angleStep;
            xCoords[i] = centerX + (float) (Math.cos(angle) * radius);
            yCoords[i] = centerY + (float) (Math.sin(angle) * radius);
        }

        addVertexWithColor(builder, m4, xCoords[0], yCoords[0], 0, color);
        addVertexWithColor(builder, m4, centerX, centerY, 0, color);

        for (int i = 1; i <= segments; i++) {
            addVertexWithColor(builder, m4, xCoords[i], yCoords[i], 0, color);
            if (i < segments) {
                addVertexWithColor(builder, m4, centerX, centerY, 0, color);
            }
        }

        setupBlendRender();
        finishBlendRender(builder);
    }

    /**
     * 绘制圆环
     */
    public static void drawCircleRing(GuiGraphics graphics, float centerX, float centerY, float radius, float lineWidth, int color) {
        drawCircleRing(graphics, centerX, centerY, radius, lineWidth, calculateCircleSegments(radius), color);
    }

    /**
     * 绘制圆环
     */
    public static void drawCircleRing(GuiGraphics graphics, float centerX, float centerY, float radius, float lineWidth, int segments, int color) {
        if (radius <= 0 || lineWidth <= 0 || segments < 3) return;

        Matrix4f m4 = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        float innerRadius = Math.max(0, radius - lineWidth);
        double angleStep = 2.0 * Math.PI / segments;

        for (int i = 0; i <= segments; i++) {
            double angle = i * angleStep;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            addVertexWithColor(builder, m4, centerX + cos * radius, centerY + sin * radius, 0, color);
            addVertexWithColor(builder, m4, centerX + cos * innerRadius, centerY + sin * innerRadius, 0, color);
        }

        setupBlendRender();
        finishBlendRender(builder);
    }

    /**
     * 绘制填充椭圆
     */
    public static void drawEllipse(GuiGraphics graphics, float centerX, float centerY, float radiusX, float radiusY, int color) {
        float maxRadius = Math.max(radiusX, radiusY);
        drawEllipse(graphics, centerX, centerY, radiusX, radiusY, calculateCircleSegments(maxRadius), color);
    }

    /**
     * 绘制填充椭圆
     */
    public static void drawEllipse(GuiGraphics graphics, float centerX, float centerY, float radiusX, float radiusY, int segments, int color) {
        if (radiusX <= 0 || radiusY <= 0 || segments < 3) return;

        Matrix4f m4 = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        double angleStep = 2.0 * Math.PI / segments;
        float[] xCoords = new float[segments + 1];
        float[] yCoords = new float[segments + 1];
        for (int i = 0; i <= segments; i++) {
            double angle = i * angleStep;
            xCoords[i] = centerX + (float) (Math.cos(angle) * radiusX);
            yCoords[i] = centerY + (float) (Math.sin(angle) * radiusY);
        }

        addVertexWithColor(builder, m4, xCoords[0], yCoords[0], 0, color);
        addVertexWithColor(builder, m4, centerX, centerY, 0, color);

        for (int i = 1; i <= segments; i++) {
            addVertexWithColor(builder, m4, xCoords[i], yCoords[i], 0, color);
            if (i < segments) {
                addVertexWithColor(builder, m4, centerX, centerY, 0, color);
            }
        }

        setupBlendRender();
        finishBlendRender(builder);
    }

    /**
     * 绘制椭圆环
     */
    public static void drawEllipseRing(GuiGraphics graphics, float centerX, float centerY, float radiusX, float radiusY, float lineWidth, int color) {
        float maxRadius = Math.max(radiusX, radiusY);
        drawEllipseRing(graphics, centerX, centerY, radiusX, radiusY, lineWidth, calculateCircleSegments(maxRadius), color);
    }

    /**
     * 绘制椭圆环
     */
    public static void drawEllipseRing(GuiGraphics graphics, float centerX, float centerY, float radiusX, float radiusY, float lineWidth, int segments, int color) {
        if (radiusX <= 0 || radiusY <= 0 || lineWidth <= 0 || segments < 3) return;

        Matrix4f m4 = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        float innerRadiusX = Math.max(0, radiusX - lineWidth);
        float innerRadiusY = Math.max(0, radiusY - lineWidth);
        double angleStep = 2.0 * Math.PI / segments;

        for (int i = 0; i <= segments; i++) {
            double angle = i * angleStep;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            addVertexWithColor(builder, m4, centerX + cos * radiusX, centerY + sin * radiusY, 0, color);
            addVertexWithColor(builder, m4, centerX + cos * innerRadiusX, centerY + sin * innerRadiusY, 0, color);
        }

        setupBlendRender();
        finishBlendRender(builder);
    }

    /**
     * 绘制填充椭圆
     *
     * @param rotation 旋转角度, 0为正右, 顺时针
     */
    public static void drawEllipse(GuiGraphics graphics, float centerX, float centerY, float radiusX, float radiusY, double rotation, int color) {
        float maxRadius = Math.max(radiusX, radiusY);
        drawEllipse(graphics, centerX, centerY, radiusX, radiusY, rotation, calculateCircleSegments(maxRadius), color);
    }

    /**
     * 绘制填充椭圆
     *
     * @param rotation 旋转角度, 0为正右, 顺时针
     */
    public static void drawEllipse(GuiGraphics graphics, float centerX, float centerY, float radiusX, float radiusY, double rotation, int segments, int color) {
        drawEllipseRad(graphics, centerX, centerY, radiusX, radiusY, Math.toRadians(rotation), segments, color);
    }

    /**
     * 绘制填充椭圆
     *
     * @param rotation 旋转弧度, 0为正右, 顺时针
     */
    public static void drawEllipseRad(GuiGraphics graphics, float centerX, float centerY, float radiusX, float radiusY, double rotation, int color) {
        float maxRadius = Math.max(radiusX, radiusY);
        drawEllipseRad(graphics, centerX, centerY, radiusX, radiusY, rotation, calculateCircleSegments(maxRadius), color);
    }

    /**
     * 绘制填充椭圆
     *
     * @param rotation 旋转弧度, 0为正右, 顺时针
     */
    public static void drawEllipseRad(GuiGraphics graphics, float centerX, float centerY, float radiusX, float radiusY, double rotation, int segments, int color) {
        if (radiusX <= 0 || radiusY <= 0 || segments < 3) return;

        Matrix4f m4 = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        double angleStep = 2.0 * Math.PI / segments;
        float cosRot = (float) Math.cos(rotation);
        float sinRot = (float) Math.sin(rotation);

        float[] xCoords = new float[segments + 1];
        float[] yCoords = new float[segments + 1];
        for (int i = 0; i <= segments; i++) {
            double angle = i * angleStep;
            float x = (float) (Math.cos(angle) * radiusX);
            float y = (float) (Math.sin(angle) * radiusY);
            xCoords[i] = centerX + x * cosRot - y * sinRot;
            yCoords[i] = centerY + x * sinRot + y * cosRot;
        }

        addVertexWithColor(builder, m4, xCoords[0], yCoords[0], 0, color);
        addVertexWithColor(builder, m4, centerX, centerY, 0, color);

        for (int i = 1; i <= segments; i++) {
            addVertexWithColor(builder, m4, xCoords[i], yCoords[i], 0, color);
            if (i < segments) {
                addVertexWithColor(builder, m4, centerX, centerY, 0, color);
            }
        }

        setupBlendRender();
        finishBlendRender(builder);
    }

    /**
     * 绘制椭圆环
     *
     * @param rotation 旋转角度, 0为正右, 顺时针
     */
    public static void drawEllipseRing(GuiGraphics graphics, float centerX, float centerY, float radiusX, float radiusY, double rotation, float lineWidth, int color) {
        float maxRadius = Math.max(radiusX, radiusY);
        drawEllipseRing(graphics, centerX, centerY, radiusX, radiusY, rotation, lineWidth, calculateCircleSegments(maxRadius), color);
    }

    /**
     * 绘制椭圆环
     *
     * @param rotation 旋转角度, 0为正右, 顺时针
     */
    public static void drawEllipseRing(GuiGraphics graphics, float centerX, float centerY, float radiusX, float radiusY, double rotation, float lineWidth, int segments, int color) {
        drawEllipseRingRad(graphics, centerX, centerY, radiusX, radiusY, Math.toRadians(rotation), lineWidth, segments, color);
    }

    /**
     * 绘制椭圆环
     *
     * @param rotation 旋转弧度, 0为正右, 顺时针
     */
    public static void drawEllipseRingRad(GuiGraphics graphics, float centerX, float centerY, float radiusX, float radiusY, double rotation, float lineWidth, int color) {
        float maxRadius = Math.max(radiusX, radiusY);
        drawEllipseRingRad(graphics, centerX, centerY, radiusX, radiusY, rotation, lineWidth, calculateCircleSegments(maxRadius), color);
    }

    /**
     * 绘制椭圆环
     *
     * @param rotation 旋转弧度, 0为正右, 顺时针
     */
    public static void drawEllipseRingRad(GuiGraphics graphics, float centerX, float centerY, float radiusX, float radiusY, double rotation, float lineWidth, int segments, int color) {
        if (radiusX <= 0 || radiusY <= 0 || lineWidth <= 0 || segments < 3) return;

        Matrix4f m4 = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        float innerRadiusX = Math.max(0, radiusX - lineWidth);
        float innerRadiusY = Math.max(0, radiusY - lineWidth);
        double angleStep = 2.0 * Math.PI / segments;
        float cosRot = (float) Math.cos(rotation);
        float sinRot = (float) Math.sin(rotation);

        for (int i = 0; i <= segments; i++) {
            double angle = i * angleStep;
            float xOuter = (float) (Math.cos(angle) * radiusX);
            float yOuter = (float) (Math.sin(angle) * radiusY);
            float xInner = (float) (Math.cos(angle) * innerRadiusX);
            float yInner = (float) (Math.sin(angle) * innerRadiusY);

            addVertexWithColor(builder, m4, centerX + xOuter * cosRot - yOuter * sinRot, centerY + xOuter * sinRot + yOuter * cosRot, 0, color);
            addVertexWithColor(builder, m4, centerX + xInner * cosRot - yInner * sinRot, centerY + xInner * sinRot + yInner * cosRot, 0, color);
        }

        setupBlendRender();
        finishBlendRender(builder);
    }

    /**
     * 绘制扇形
     *
     * @param startAngle 起始角度, 0为正右
     * @param endAngle   结束角度, start至end顺时针旋转
     */
    public static void drawSector(GuiGraphics graphics, float centerX, float centerY, float radius, double startAngle, double endAngle, int color) {
        drawSectorRad(graphics, centerX, centerY, radius, Math.toRadians(startAngle), Math.toRadians(endAngle), calculateCircleSegments(radius), color);
    }

    /**
     * 绘制扇形
     *
     * @param startAngle 起始角度, 0为正右
     * @param endAngle   结束角度, start至end顺时针旋转
     */
    public static void drawSector(GuiGraphics graphics, float centerX, float centerY, float radius, double startAngle, double endAngle, int segments, int color) {
        drawSectorRad(graphics, centerX, centerY, radius, Math.toRadians(startAngle), Math.toRadians(endAngle), segments, color);
    }

    /**
     * 绘制扇形
     *
     * @param startAngle 起始弧度, 0为正右
     * @param endAngle   结束弧度, start至end顺时针旋转
     */
    public static void drawSectorRad(GuiGraphics graphics, float centerX, float centerY, float radius, double startAngle, double endAngle, int color) {
        drawSectorRad(graphics, centerX, centerY, radius, startAngle, endAngle, calculateCircleSegments(radius), color);
    }

    /**
     * 绘制扇形
     *
     * @param startAngle 起始弧度, 0为正右
     * @param endAngle   结束弧度, start至end顺时针旋转
     */
    public static void drawSectorRad(GuiGraphics graphics, float centerX, float centerY, float radius, double startAngle, double endAngle, int segments, int color) {
        if (radius <= 0 || segments < 2) return;

        Matrix4f m4 = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        double angleRange = endAngle - startAngle;
        if (angleRange < 0) angleRange += 2.0 * Math.PI;
        double angleStep = angleRange / segments;

        float[] xCoords = new float[segments + 1];
        float[] yCoords = new float[segments + 1];
        for (int i = 0; i <= segments; i++) {
            double angle = startAngle + i * angleStep;
            xCoords[i] = centerX + (float) (Math.cos(angle) * radius);
            yCoords[i] = centerY + (float) (Math.sin(angle) * radius);
        }

        addVertexWithColor(builder, m4, xCoords[0], yCoords[0], 0, color);
        addVertexWithColor(builder, m4, centerX, centerY, 0, color);

        for (int i = 1; i <= segments; i++) {
            addVertexWithColor(builder, m4, xCoords[i], yCoords[i], 0, color);
            if (i < segments) {
                addVertexWithColor(builder, m4, centerX, centerY, 0, color);
            }
        }

        setupBlendRender();
        finishBlendRender(builder);
    }

    /**
     * 绘制扇环
     *
     * @param startAngle 起始角度, 0为正右
     * @param endAngle   结束角度, start至end顺时针旋转
     */
    public static void drawSectorRing(GuiGraphics graphics, float centerX, float centerY, float radius, double startAngle, double endAngle, float lineWidth, int color) {
        drawSectorRingRad(graphics, centerX, centerY, radius, Math.toRadians(startAngle), Math.toRadians(endAngle), lineWidth, calculateCircleSegments(radius), color);
    }

    /**
     * 绘制扇环
     *
     * @param startAngle 起始角度, 0为正右
     * @param endAngle   结束角度, start至end顺时针旋转
     */
    public static void drawSectorRing(GuiGraphics graphics, float centerX, float centerY, float radius, double startAngle, double endAngle, float lineWidth, int segments, int color) {
        drawSectorRingRad(graphics, centerX, centerY, radius, Math.toRadians(startAngle), Math.toRadians(endAngle), lineWidth, segments, color);
    }

    /**
     * 绘制扇环
     *
     * @param startAngle 起始弧度, 0为正右
     * @param endAngle   结束弧度, start至end顺时针旋转
     */
    public static void drawSectorRingRad(GuiGraphics graphics, float centerX, float centerY, float radius, double startAngle, double endAngle, float lineWidth, int color) {
        drawSectorRingRad(graphics, centerX, centerY, radius, startAngle, endAngle, lineWidth, calculateCircleSegments(radius), color);
    }

    /**
     * 绘制扇环
     *
     * @param startAngle 起始弧度, 0为正右
     * @param endAngle   结束弧度, start至end顺时针旋转
     */
    public static void drawSectorRingRad(GuiGraphics graphics, float centerX, float centerY, float radius, double startAngle, double endAngle, float lineWidth, int segments, int color) {
        if (radius <= 0 || lineWidth <= 0 || segments < 2) return;

        Matrix4f m4 = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        float innerRadius = Math.max(0, radius - lineWidth);
        double angleRange = endAngle - startAngle;
        if (angleRange < 0) angleRange += 2.0 * Math.PI;
        double angleStep = angleRange / segments;

        for (int i = 0; i <= segments; i++) {
            double angle = startAngle + i * angleStep;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            addVertexWithColor(builder, m4, centerX + cos * radius, centerY + sin * radius, 0, color);
            addVertexWithColor(builder, m4, centerX + cos * innerRadius, centerY + sin * innerRadius, 0, color);
        }

        setupBlendRender();
        finishBlendRender(builder);
    }

    /**
     * 绘制多边形
     *
     * @param centerX  中心X坐标
     * @param centerY  中心Y坐标
     * @param radius   外接圆半径
     * @param sides    边数（n边形，n >= 3）
     * @param rotation 旋转角度
     */
    public static void drawPolygon(GuiGraphics graphics, float centerX, float centerY, float radius, int sides, double rotation, int color) {
        if (radius <= 0 || sides < 3) return;

        Matrix4f m4 = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        double rotationRad = Math.toRadians(rotation);
        double angleStep = 2.0 * Math.PI / sides;

        // 计算所有顶点坐标
        float[] xCoords = new float[sides];
        float[] yCoords = new float[sides];
        for (int i = 0; i < sides; i++) {
            double angle = i * angleStep + rotationRad;
            xCoords[i] = centerX + (float) (Math.cos(angle) * radius);
            yCoords[i] = centerY + (float) (Math.sin(angle) * radius);
        }

        // 使用三角形条带模式绘制（从中心点开始）
        addVertexWithColor(builder, m4, xCoords[0], yCoords[0], 0, color);
        addVertexWithColor(builder, m4, centerX, centerY, 0, color);

        for (int i = 1; i < sides; i++) {
            addVertexWithColor(builder, m4, xCoords[i], yCoords[i], 0, color);
            addVertexWithColor(builder, m4, centerX, centerY, 0, color);
        }

        // 闭合多边形
        addVertexWithColor(builder, m4, xCoords[0], yCoords[0], 0, color);
        addVertexWithColor(builder, m4, centerX, centerY, 0, color);

        setupBlendRender();
        finishBlendRender(builder);
    }

    /**
     * 绘制多边形边框
     */
    private static void drawPolygonBorder(GuiGraphics graphics, ShapeDrawArgs.PolygonParams polygon, int color) {
        float centerX = polygon.centerX();
        float centerY = polygon.centerY();
        float radius = polygon.radius();
        int sides = polygon.sides();
        double rotation = polygon.rotation();
        float borderWidth = polygon.border();

        if (radius <= 0 || sides < 3 || borderWidth <= 0) return;

        double rotationRad = Math.toRadians(rotation);
        double angleStep = 2.0 * Math.PI / sides;
        float innerRadius = Math.max(0, radius - borderWidth);

        Matrix4f m4 = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        // 绘制每条边的边框
        for (int i = 0; i <= sides; i++) {
            double angle = (i % sides) * angleStep + rotationRad;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            addVertexWithColor(builder, m4, centerX + cos * radius, centerY + sin * radius, 0, color);
            addVertexWithColor(builder, m4, centerX + cos * innerRadius, centerY + sin * innerRadius, 0, color);
        }

        setupBlendRender();
        finishBlendRender(builder);
    }

    // endregion 绘制圆


    //  region 绘制弹出层提示

    /**
     * 绘制九宫格纹理背景
     *
     * @param texture    纹理对象，包含资源位置和范围信息
     * @param x          绘制起始X坐标
     * @param y          绘制起始Y坐标
     * @param destWidth  目标宽度（缩放后的最终宽度）
     * @param destHeight 目标高度（缩放后的最终高度）
     * @param scale      缩放比例（用于根据右参考线高度缩放）
     */
    public static void drawNinePatch(GuiGraphics graphics, Texture texture, int x, int y, int destWidth, int destHeight, double scale) {
        if (texture == null) {
            return;
        }

        TextureUtils.NinePatchInfo info = TextureUtils.parseNinePatch(texture);
        if (info == null) {
            blit(graphics, texture.location(), x, y, destWidth, destHeight,
                    texture.u0(), texture.v0(), texture.uWidth(), texture.vHeight(),
                    texture.uvWidth(), texture.uvHeight());
            return;
        }
        if (scale <= 0) {
            // 根据右参考线的高度计算缩放比例
            if (info.rightGuideHeight > 0) {
                scale = (double) AbstractGuiUtils.getFont().lineHeight / info.rightGuideHeight;
            }
        }

        int contentStartX = texture.u0() + 1;
        int contentStartY = texture.v0() + 1;

        // 原始尺寸
        double originalDestWidth = destWidth / scale;
        double originalDestHeight = destHeight / scale;

        // 原始尺寸的固定和可拉伸区域
        int totalFixedWidth = 0;
        int totalFixedHeight = 0;
        int totalStretchableWidth = 0;
        int totalStretchableHeight = 0;

        // 原始尺寸水平方向的固定和可拉伸区域
        for (int i = 0; i < info.horizontalDivisions.length - 1; i++) {
            int regionWidth = info.horizontalDivisions[i + 1] - info.horizontalDivisions[i];
            if (info.horizontalStretchable[i]) {
                totalStretchableWidth += regionWidth;
            } else {
                totalFixedWidth += regionWidth;
            }
        }

        // 原始尺寸垂直方向的固定和可拉伸区域
        for (int i = 0; i < info.verticalDivisions.length - 1; i++) {
            int regionHeight = info.verticalDivisions[i + 1] - info.verticalDivisions[i];
            if (info.verticalStretchable[i]) {
                totalStretchableHeight += regionHeight;
            } else {
                totalFixedHeight += regionHeight;
            }
        }

        // 原始尺寸需要拉伸的尺寸
        double stretchWidth = Math.max(0, originalDestWidth - totalFixedWidth);
        double stretchHeight = Math.max(0, originalDestHeight - totalFixedHeight);

        // 拉伸比例
        double stretchWidthRatio = totalStretchableWidth > 0 ? stretchWidth / totalStretchableWidth : 1.0f;
        double stretchHeightRatio = totalStretchableHeight > 0 ? stretchHeight / totalStretchableHeight : 1.0f;

        // 应用缩放变换
        boolean needsScale = Math.abs(scale - 1.0f) > 0.001f;
        if (needsScale) {
            graphics.pose().pushPose();
            // 移动到绘制起始位置
            graphics.pose().translate(x, y, 0);
            // 应用缩放
            graphics.pose().scale((float) scale, (float) scale, 1.0f);
            x = 0;
            y = 0;
        }

        // 绘制各个区域
        double currentY = y;
        for (int v = 0; v < info.verticalDivisions.length - 1; v++) {
            int srcVStart = contentStartY + info.verticalDivisions[v];
            int srcVEnd = contentStartY + info.verticalDivisions[v + 1] - 1;
            int srcVHeight = srcVEnd - srcVStart + 1;

            double destVHeight;
            if (info.verticalStretchable[v]) {
                destVHeight = srcVHeight * stretchHeightRatio;
            } else {
                destVHeight = srcVHeight;
            }

            double currentX = x;
            for (int h = 0; h < info.horizontalDivisions.length - 1; h++) {
                int srcHStart = contentStartX + info.horizontalDivisions[h];
                int srcHEnd = contentStartX + info.horizontalDivisions[h + 1] - 1;
                int srcHWidth = srcHEnd - srcHStart + 1;

                double destHWidth;
                if (info.horizontalStretchable[h]) {
                    destHWidth = srcHWidth * stretchWidthRatio;
                } else {
                    destHWidth = srcHWidth;
                }

                // 绘制当前区域，使用纹理的完整尺寸作为纹理坐标的参考
                blit(graphics, texture.location(), (int) currentX, (int) currentY, (int) destHWidth, (int) destVHeight,
                        srcHStart, srcVStart, srcHWidth, srcVHeight,
                        texture.uvWidth(), texture.uvHeight());

                currentX += destHWidth;
            }

            currentY += destVHeight;
        }

        // 恢复矩阵状态
        if (needsScale) {
            graphics.pose().popPose();
        }
    }

    /**
     * 绘制弹出层消息
     */
    public static void drawPopupMessage(FontDrawArgs args) {
        // 计算文字最终绘制大小
        KeyValue<Integer, Integer> textSize = calculateLimitedTextSize(args);
        int textWidth = textSize.key();
        int textHeight = textSize.val();

        // 检查是否是.9.png格式纹理
        final TextureUtils.NinePatchInfo ninePatchInfo = args.texture() != null ? TextureUtils.parseNinePatch(args.texture()) : null;

        float calculatedTextureScale = 1.0f;
        int calculatedPaddingLeft = args.paddingLeft();
        int calculatedPaddingRight = args.paddingRight();
        int calculatedPaddingTop = args.paddingTop();
        int calculatedPaddingBottom = args.paddingBottom();

        if (ninePatchInfo != null) {
            Color color = Color.argb(ninePatchInfo.textColor);
            if (!color.isEmpty()) {
                args.text().color(color);
            }
            // 计算背景绘制大小与内外边距
            Font font = args.text().font();
            float targetFontSize = args.fontSize() > 0 ? args.fontSize() : font.lineHeight;

            // 根据右参考线的高度计算缩放比例
            if (ninePatchInfo.rightGuideHeight > 0) {
                calculatedTextureScale = targetFontSize / ninePatchInfo.rightGuideHeight;
            }

            // 根据下参考线计算内边距
            if (ninePatchInfo.bottomGuideLeftPadding > 0) {
                calculatedPaddingLeft += (int) (ninePatchInfo.bottomGuideLeftPadding * calculatedTextureScale);
            }
            if (ninePatchInfo.bottomGuideRightPadding > 0) {
                calculatedPaddingRight += (int) (ninePatchInfo.bottomGuideRightPadding * calculatedTextureScale);
            }

            // 计算上内边距
            if (ninePatchInfo.rightGuideTopPadding > 0) {
                calculatedPaddingTop += (int) (ninePatchInfo.rightGuideTopPadding * calculatedTextureScale);
            }

            // 计算下内边距
            if (ninePatchInfo.rightGuideBottomPadding > 0) {
                calculatedPaddingBottom += (int) (ninePatchInfo.rightGuideBottomPadding * calculatedTextureScale);
            }

            // 重新计算文本尺寸
            FontDrawArgs recalcArgs = args.clone()
                    .paddingLeft(calculatedPaddingLeft)
                    .paddingRight(calculatedPaddingRight)
                    .paddingTop(calculatedPaddingTop)
                    .paddingBottom(calculatedPaddingBottom);
            textSize = calculateLimitedTextSize(recalcArgs);
            textWidth = textSize.key();
            textHeight = textSize.val();
        }

        final float textureScale = calculatedTextureScale;
        final int finalCalculatedPaddingLeft = calculatedPaddingLeft;
        final int finalCalculatedPaddingRight = calculatedPaddingRight;
        final int finalCalculatedPaddingTop = calculatedPaddingTop;
        final int finalCalculatedPaddingBottom = calculatedPaddingBottom;

        // 计算消息框的总宽度和高度
        int msgWidth = textWidth;
        int msgHeight = textHeight;

        // 计算调整后的坐标
        double adjustedX = args.x();
        double adjustedY = args.y();
        int finalMaxWidth = args.maxWidth();

        if (args.inScreen()) {
            KeyValue<Integer, Integer> screenSize = getScreenSize();
            int screenWidth = screenSize.key();
            int screenHeight = screenSize.val();

            // 若启用了自动换行，根据 maxWidth 计算文本尺寸
            if (args.wrap() && finalMaxWidth > 0) {
                FontDrawArgs maxWidthRecalcArgs = args.clone()
                        .paddingLeft(finalCalculatedPaddingLeft)
                        .paddingRight(finalCalculatedPaddingRight)
                        .paddingTop(finalCalculatedPaddingTop)
                        .paddingBottom(finalCalculatedPaddingBottom)
                        .maxWidth(finalMaxWidth);
                KeyValue<Integer, Integer> maxWidthTextSize = calculateLimitedTextSize(maxWidthRecalcArgs);
                msgWidth = maxWidthTextSize.key();
                msgHeight = maxWidthTextSize.val();
            }

            // 初始化调整后的坐标
            // 横向居中
            adjustedX = args.x() - msgWidth / 2.0;
            // 放置于鼠标上方
            adjustedY = args.y() - msgHeight - 5;

            // 检查顶部空间是否充足
            boolean hasTopSpace = adjustedY >= args.marginTop();
            // 检查左右空间是否充足
            boolean hasLeftSpace = adjustedX >= args.marginLeft();
            boolean hasRightSpace = adjustedX + msgWidth <= screenWidth - args.marginRight();

            // 若顶部空间不足，调整到鼠标下方
            if (!hasTopSpace) {
                adjustedY = args.y() + 1 + 5;
            }
            //
            else {
                // 若左侧空间不足，靠右
                if (!hasLeftSpace) {
                    adjustedX = args.marginLeft();
                }
                // 若右侧空间不足，靠左
                else if (!hasRightSpace) {
                    adjustedX = screenWidth - msgWidth - args.marginRight();
                }
            }

            // 若调整后仍然超出屏幕范围，强制限制在屏幕内
            adjustedX = Math.max(args.marginLeft(), Math.min(adjustedX, screenWidth - msgWidth - args.marginRight()));
            adjustedY = Math.max(args.marginTop(), Math.min(adjustedY, screenHeight - msgHeight - args.marginBottom()));

            // 若启用了自动换行，计算实际的可用宽度用于文本绘制时的换行限制
            if (args.wrap()) {
                int actualAvailableWidth = screenWidth - (int) adjustedX - args.marginRight();
                // 如果设置了 maxWidth，取两者中的较小值
                if (finalMaxWidth > 0) {
                    actualAvailableWidth = Math.min(actualAvailableWidth, finalMaxWidth);
                }
                // 确保可用宽度不小于内边距
                actualAvailableWidth = Math.max(actualAvailableWidth, finalCalculatedPaddingLeft + finalCalculatedPaddingRight);
                // 更新 finalMaxWidth 为实际使用的可用宽度
                finalMaxWidth = actualAvailableWidth;
            }
        }

        final int finalMaxWidthForText = finalMaxWidth;

        double finalAdjustedX = adjustedX;
        double finalAdjustedY = adjustedY;
        int finalMsgWidth = msgWidth;
        int finalMsgHeight = msgHeight;
        AbstractGuiUtils.renderByDepth(args.text().graphics(), EnumRenderDepth.POPUP_TIPS, (graphics) -> {

            // 绘制背景
            FontDrawArgs bgArgs = args.clone().x(finalAdjustedX).y(finalAdjustedY);
            if (bgArgs.texture() != null && ninePatchInfo != null) {
                // 九宫格纹理绘制
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                drawNinePatch(graphics, bgArgs.texture(), (int) bgArgs.x(), (int) bgArgs.y(), finalMsgWidth, finalMsgHeight, textureScale);
                RenderSystem.disableBlend();
            } else if (bgArgs.texture() != null) {
                // 普通纹理绘制，使用 Texture 中指定的范围
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                Texture tex = bgArgs.texture();
                blit(graphics, tex.location(), (int) bgArgs.x(), (int) bgArgs.y(), finalMsgWidth, finalMsgHeight,
                        tex.u0(), tex.v0(), tex.uWidth(), tex.vHeight(),
                        tex.uvWidth(), tex.uvHeight());
                RenderSystem.disableBlend();
            } else {
                int borderRadius = bgArgs.bgBorderRadius();
                int borderThickness = bgArgs.bgBorderThickness();
                // 绘制圆角矩形背景
                AbstractGuiUtils.drawRoundedRect(bgArgs.text().graphics(), (int) bgArgs.x(), (int) bgArgs.y(), finalMsgWidth, finalMsgHeight, bgArgs.bgArgb(), borderRadius);

                // 计算边框颜色
                int borderArgb = ColorUtils.softenArgb(bgArgs.bgArgb());

                // 绘制圆角矩形边框
                AbstractGuiUtils.drawRoundedRectOutLineRough(bgArgs.text().graphics(), (int) bgArgs.x(), (int) bgArgs.y(), finalMsgWidth, finalMsgHeight, borderThickness, borderArgb, borderRadius);
            }

            // 绘制文本
            FontDrawArgs clone = args.clone()
                    .x(finalAdjustedX)
                    .y(finalAdjustedY)
                    .bgArgb(0x00000000)
                    .position(EnumEllipsisPosition.MIDDLE)
                    .paddingLeft(finalCalculatedPaddingLeft)
                    .paddingRight(finalCalculatedPaddingRight)
                    .paddingTop(finalCalculatedPaddingTop)
                    .paddingBottom(finalCalculatedPaddingBottom);
            if (args.wrap() && finalMaxWidthForText > 0) {
                clone.maxWidth(finalMaxWidthForText);
            } else if (args.maxWidth() > 0) {
                clone.maxWidth(args.maxWidth());
            }
            AbstractGuiUtils.drawLimitedText(clone);
        });
    }

    public static void drawItemTooltip(GuiGraphics graphics, ItemStack itemgraphics, double x, double y) {
        boolean advanced = Screen.hasShiftDown();
        List<Component> tooltipList = ItemUtils.getItemTooltip(itemgraphics, Minecraft.getInstance().player, advanced);

        Component tooltipComponent = Component.empty();
        for (int idx = 0; idx < tooltipList.size(); idx++) {
            Component component = tooltipList.get(idx);
            if (idx > 0) {
                tooltipComponent = tooltipComponent.append("\n");
            }
            tooltipComponent = tooltipComponent.append(component);
        }
        Text tooltipText = new Text(tooltipComponent);

        FontDrawArgs drawArgs = FontDrawArgs.ofPopo(tooltipText.graphics(graphics))
                .x(x).y(y);
        AbstractGuiUtils.drawPopupMessage(drawArgs);
    }

    //  endregion 绘制弹出层提示


    // region 杂项

    /**
     * 获取指定坐标点像素颜色
     */
    public static int getPixelArgb(double guiX, double guiY) {
        Minecraft mc = Minecraft.getInstance();
        Window window = mc.getWindow();

        // 将 GUI 坐标（左上为原点）转换为物理屏幕坐标（左下为原点）
        int pixelX = (int) (guiX * window.getGuiScale());
        int pixelY = (int) (guiY * window.getGuiScale());
        int glY = window.getHeight() - pixelY - 1;

        // 创建 ByteBuffer 存储像素数据（RGBA）
        ByteBuffer buffer = BufferUtils.createByteBuffer(4);
        GL11.glReadPixels(pixelX, glY, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        int r = buffer.get(0) & 0xFF;
        int g = buffer.get(1) & 0xFF;
        int b = buffer.get(2) & 0xFF;
        int a = buffer.get(3) & 0xFF;

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static Font getFont() {
        return Minecraft.getInstance().font;
    }

    public static KeyValue<Integer, Integer> getScreenSize() {
        if (Minecraft.getInstance().screen != null) {
            return new KeyValue<>(Minecraft.getInstance().screen.width, Minecraft.getInstance().screen.height);
        } else {
            return getGuiScaledSize();
        }
    }

    public static KeyValue<Integer, Integer> getGuiScaledSize() {
        return new KeyValue<>(Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight());
    }

    public static KeyValue<Integer, Integer> getGuiSize() {
        return new KeyValue<>(Minecraft.getInstance().getWindow().getWidth(), Minecraft.getInstance().getWindow().getHeight());
    }

    // endregion 杂项
}
