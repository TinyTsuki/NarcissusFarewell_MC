package xin.vanilla.narcissus.client.component;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xin.vanilla.narcissus.client.enums.EnumAlignment;
import xin.vanilla.narcissus.data.Color;
import xin.vanilla.narcissus.enums.EnumI18nType;
import xin.vanilla.narcissus.util.AbstractGuiUtils;
import xin.vanilla.narcissus.util.Component;
import xin.vanilla.narcissus.util.I18nUtils;

@OnlyIn(Dist.CLIENT)
@Setter
@Accessors(chain = true, fluent = true)
public class Text implements Cloneable {
    /**
     * 矩阵栈
     */
    private MatrixStack stack;
    /**
     * 字体渲染器
     */
    private FontRenderer font;
    /**
     * 是否悬浮(需手动设置状态)
     */
    private boolean hovered;
    /**
     * 文本
     */
    private Component text = Component.empty().clone();
    /**
     * 文本对齐方式(仅多行绘制时)
     */
    private EnumAlignment align = EnumAlignment.START;
    /**
     * 鼠标悬浮时文本
     */
    private Component hoverText = Component.empty().clone();
    /**
     * 鼠标悬浮时对齐方式(仅多行绘制时)
     */
    private EnumAlignment hoverAlign = EnumAlignment.START;

    private Text() {
    }

    private Text(String text) {
        this.text = Component.literal(text);
        this.hoverText = Component.literal(text);
    }

    public Text(Component text) {
        this.text = text;
        this.hoverText = text.clone();
    }

    public static Text empty() {
        return new Text().text(Component.empty()).hoverText(Component.empty());
    }

    public static Text literal(String text) {
        return new Text(text);
    }

    public static Text trans(String key, Object... args) {
        return new Text(Component.transClient(key, args));
    }

    public static Text trans(String key) {
        return new Text(Component.transClient(key));
    }

    public static Text translatable(EnumI18nType type, String key, Object... args) {
        return new Text(Component.transClient(type, key, args));
    }

    public Text clone() {
        try {
            Text cloned = (Text) super.clone();
            cloned.stack = this.stack;
            cloned.text = this.text.clone();
            cloned.hoverText = this.hoverText.clone();
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public Text copyWithoutChildren() {
        return new Text()
                .text(this.text.clone().clearChildren().clearArgs())
                .hoverText(this.hoverText.clone().clearChildren().clearArgs())
                .hovered(this.hovered)
                .align(this.align)
                .hoverAlign(this.hoverAlign)
                .stack(this.stack)
                .font(this.font);
    }

    public boolean isEmpty() {
        return (this.text == null || this.text.isEmpty()) && (this.hoverText == null || this.hoverText.isEmpty());
    }

    public MatrixStack stack() {
        return stack == null ? new MatrixStack() : this.stack;
    }

    public FontRenderer font() {
        return font == null ? AbstractGuiUtils.getFont() : this.font;
    }

    public boolean colorEmpty() {
        return this.hovered ? this.hoverText.color().isEmpty() : this.text.color().isEmpty();
    }

    public int color() {
        return this.hovered ? this.hoverText.color().rgb() : this.text.color().rgb();
    }

    public int colorArgb() {
        return this.hovered ? this.hoverText.color().argb() : this.text.color().argb();
    }

    public int colorRgba() {
        return this.hovered ? this.hoverText.color().rgba() : this.text.color().rgba();
    }

    public boolean bgColorEmpty() {
        return this.hovered ? this.hoverText.bgColor().isEmpty() : this.text.bgColor().isEmpty();
    }

    public int bgColor() {
        return this.hovered ? this.hoverText.bgColor().rgb() : this.text.bgColor().rgb();
    }

    public int bgColorArgb() {
        return this.hovered ? this.hoverText.bgColor().argb() : this.text.bgColor().argb();
    }

    public int bgColorRgba() {
        return this.hovered ? this.hoverText.bgColor().rgba() : this.text.bgColor().rgba();
    }

    public Component original() {
        return this.hovered ? this.hoverText : this.text;
    }

    public String content() {
        return content(true);
    }

    /**
     * 获取文本内容, 忽略样式
     *
     * @param ignoreStyle 是否忽略样式
     */
    public String content(boolean ignoreStyle) {
        return this.hovered ? this.hoverText.getString(I18nUtils.getClientLanguage(), ignoreStyle, true) : this.text.getString(I18nUtils.getClientLanguage(), ignoreStyle, true);
    }

    public boolean shadow() {
        return this.hovered ? this.hoverText.shadow() : this.text.shadow();
    }

    public boolean bold() {
        return this.hovered ? this.hoverText.bold() : this.text.bold();
    }

    public boolean italic() {
        return this.hovered ? this.hoverText.italic() : this.text.italic();
    }

    public boolean underlined() {
        return this.hovered ? this.hoverText.underlined() : this.text.underlined();
    }

    public boolean strikethrough() {
        return this.hovered ? this.hoverText.strikethrough() : this.text.strikethrough();
    }

    public boolean obfuscated() {
        return this.hovered ? this.hoverText.obfuscated() : this.text.obfuscated();
    }

    public EnumAlignment align() {
        return this.hovered ? this.hoverAlign : this.align;
    }

    public Text color(Color color) {
        this.text.color(color);
        this.hoverText.color(color);
        return this;
    }

    public Text color(int rgb) {
        Color color = Color.rgb(rgb);
        this.text.color(color);
        this.hoverText.color(color);
        return this;
    }

    public Text bgColor(Color bgColor) {
        this.text.bgColor(bgColor);
        this.hoverText.bgColor(bgColor);
        return this;
    }

    public Text bgColor(int rgb) {
        Color color = Color.rgb(rgb);
        this.text.bgColor(color);
        this.hoverText.bgColor(color);
        return this;
    }

    public Text text(String text) {
        this.text.i18nType(EnumI18nType.PLAIN).text(text);
        this.hoverText.i18nType(EnumI18nType.PLAIN).text(text);
        return this;
    }

    public Text text(Component text) {
        this.text = text;
        this.hoverText = text.clone();
        return this;
    }

    public Text hoverText(String text) {
        this.hoverText.i18nType(EnumI18nType.PLAIN).text(text);
        return this;
    }

    public Text hoverText(Component text) {
        this.hoverText = text;
        return this;
    }

    public Text shadow(boolean shadow) {
        this.text.shadow(shadow);
        this.hoverText.shadow(shadow);
        return this;
    }

    public Text bold(boolean bold) {
        this.text.bold(bold);
        this.hoverText.bold(bold);
        return this;
    }

    public Text italic(boolean italic) {
        this.text.italic(italic);
        this.hoverText.italic(italic);
        return this;
    }

    public Text underlined(boolean underlined) {
        this.text.underlined(underlined);
        this.hoverText.underlined(underlined);
        return this;
    }

    public Text strikethrough(boolean strikethrough) {
        this.text.strikethrough(strikethrough);
        this.hoverText.strikethrough(strikethrough);
        return this;
    }

    public Text obfuscated(boolean obfuscated) {
        this.text.obfuscated(obfuscated);
        this.hoverText.obfuscated(obfuscated);
        return this;
    }

    public Text align(EnumAlignment align) {
        this.align = align;
        this.hoverAlign = align;
        return this;
    }

    public Text withStyle(Text text) {
        this.text.withStyle(text.text);
        this.hoverText.withStyle(text.hoverText);
        return this;
    }

    public Component toComponent() {
        return this.hovered ? this.hoverText : this.text;
    }

    public static Color getTextComponentColor(IFormattableTextComponent textComponent) {
        return getTextComponentColor(textComponent, Color.white());
    }

    public static Color getTextComponentColor(IFormattableTextComponent textComponent, Color defaultColor) {
        return textComponent.getStyle().getColor() == null ? defaultColor : Color.rgb(textComponent.getStyle().getColor().getValue());
    }

    public static Text from(IFormattableTextComponent component) {
        return Text.literal(component.getString())
                .color(getTextComponentColor(component))
                .bold(component.getStyle().isBold())
                .italic(component.getStyle().isItalic())
                .underlined(component.getStyle().isUnderlined())
                .strikethrough(component.getStyle().isStrikethrough())
                .obfuscated(component.getStyle().isObfuscated());
    }

    public static Text from(Component component) {
        return new Text(component);
    }
}
