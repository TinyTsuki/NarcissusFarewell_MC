package xin.vanilla.narcissus.util;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.enums.EI18nType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Accessors(chain = true)
@NoArgsConstructor
public class Component {

    // region 属性定义
    /**
     * 文本
     */
    @Getter
    @Setter
    private String text = "";
    /**
     * i18n类型
     */
    @Getter
    @Setter
    private EI18nType i18nType = EI18nType.PLAIN;

    /**
     * 子组件
     */
    @Getter
    private final List<Component> children = new ArrayList<>();

    /**
     * 翻译组件参数
     */
    @Getter
    private final List<Component> args = new ArrayList<>();

    // region 样式属性

    /**
     * 语言代码
     */
    @Setter
    private String languageCode;
    /**
     * 文本颜色
     */
    @Setter
    private Integer color;
    /**
     * 文本背景色
     */
    @Setter
    private Integer bgColor;
    /**
     * 是否有阴影
     */
    @Setter
    private Boolean shadow;
    /**
     * 是否粗体
     */
    @Setter
    private Boolean bold;
    /**
     * 是否斜体
     */
    @Setter
    private Boolean italic;
    /**
     * 是否下划线
     */
    @Setter
    private Boolean underlined;
    /**
     * 是否中划线
     */
    @Setter
    private Boolean strikethrough;
    /**
     * 是否混淆
     */
    @Setter
    private Boolean obfuscated;
    /**
     * 点击事件
     */
    @Setter
    @Getter
    private ClickEvent clickEvent;
    /**
     * 悬停事件
     */
    @Setter
    @Getter
    private HoverEvent hoverEvent;

    // endregion 样式属性

    // endregion 属性定义

    public Component(String text) {
        this.text = text;
    }

    public Component(String text, EI18nType i18nType) {
        this.text = text;
        this.i18nType = i18nType;
    }

    // region NonNull Getter

    /**
     * 获取语言代码
     */
    public @NonNull String getLanguageCode() {
        return this.languageCode == null ? NarcissusFarewell.getDefaultLanguage() : this.languageCode;
    }

    /**
     * 获取文本颜色
     */
    public int getColor() {
        return this.color == null ? 0xFFFFFFFF : this.color;
    }

    /**
     * 获取文本背景色
     */
    public int getBgColor() {
        return this.bgColor == null ? 0x00000000 : this.bgColor;
    }

    /**
     * 是否有阴影
     */
    public boolean isShadow() {
        return this.shadow != null && this.shadow;
    }

    /**
     * 是否粗体
     */
    public boolean isBold() {
        return this.bold != null && this.bold;
    }

    /**
     * 是否斜体
     */
    public boolean isItalic() {
        return this.italic != null && this.italic;
    }

    /**
     * 是否下划线
     */
    public boolean isUnderlined() {
        return this.underlined != null && this.underlined;
    }

    /**
     * 是否中划线
     */
    public boolean isStrikethrough() {
        return this.strikethrough != null && this.strikethrough;
    }

    /**
     * 是否混淆
     */
    public boolean isObfuscated() {
        return this.obfuscated != null && this.obfuscated;
    }

    // endregion NonNull Getter

    // region 样式元素是否为空(用于父组件样式传递)

    /**
     * 语言代码是否为空
     */
    public boolean isLanguageCodeEmpty() {
        return this.languageCode == null;
    }

    /**
     * 文本颜色是否为空
     */
    public boolean isColorEmpty() {
        return this.color == null;
    }

    /**
     * 文本背景色是否为空
     */
    public boolean isBgColorEmpty() {
        return this.bgColor == null;
    }

    /**
     * 阴影状态是否为空
     */
    public boolean isShadowEmpty() {
        return this.shadow == null;
    }

    /**
     * 粗体状态是否为空
     */
    public boolean isBoldEmpty() {
        return this.bold == null;
    }

    /**
     * 斜体状态是否为空
     */
    public boolean isItalicEmpty() {
        return this.italic == null;
    }

    /**
     * 下划线状态是否为空
     */
    public boolean isUnderlinedEmpty() {
        return this.underlined == null;
    }

    /**
     * 中划线状态是否为空
     */
    public boolean isStrikethroughEmpty() {
        return this.strikethrough == null;
    }

    /**
     * 混淆状态是否为空
     */
    public boolean isObfuscatedEmpty() {
        return this.obfuscated == null;
    }

    // endregion 样式元素是否为空(用于父组件样式传递)

    public Component copy() {
        Component component = new Component()
                .setText(this.text)
                .setI18nType(this.i18nType)
                .setLanguageCode(this.languageCode)
                .setColor(this.color)
                .setBgColor(this.bgColor)
                .setShadow(this.shadow)
                .setBold(this.bold)
                .setItalic(this.italic)
                .setUnderlined(this.underlined)
                .setStrikethrough(this.strikethrough)
                .setObfuscated(this.obfuscated)
                .setClickEvent(this.clickEvent)
                .setHoverEvent(this.hoverEvent);
        for (Component child : this.children) {
            component.getChildren().add(child.copy());
        }
        return component;
    }

    public Component append(Object... objs) {
        return this.append(this.children.size(), objs);
    }

    public Component append(int index, Object... objs) {
        for (int i = 0; i < objs.length; i++) {
            Object obj = objs[i];
            if (obj instanceof Component) {
                this.children.add(index + i, ((Component) obj).withStyle(this));
            } else {
                this.children.add(index + i, new Component(obj.toString()).withStyle(this));
            }
        }
        return this;
    }

    public Component appendArg(Object... objs) {
        return this.appendArg(this.args.size(), objs);
    }

    public Component appendArg(int index, Object... objs) {
        for (int i = 0; i < objs.length; i++) {
            Object obj = objs[i];
            if (obj instanceof Component) {
                this.args.add(index + i, ((Component) obj).withStyle(this));
            } else {
                this.args.add(index + i, new Component(obj.toString()).withStyle(this));
            }
        }
        return this;
    }

    /**
     * 将另一个组件的样式应用到当前组件
     */
    public Component withStyle(Component component) {
        if (this.isLanguageCodeEmpty() && !component.isLanguageCodeEmpty()) {
            this.setLanguageCode(component.getLanguageCode());
        }
        if (this.isColorEmpty() && !component.isColorEmpty()) {
            this.setColor(component.getColor());
        }
        if (this.isBgColorEmpty() && !component.isBgColorEmpty()) {
            this.setBgColor(component.getBgColor());
        }
        if (this.isShadowEmpty() && !component.isShadowEmpty()) {
            this.setShadow(component.isShadow());
        }
        if (this.isBoldEmpty() && !component.isBoldEmpty()) {
            this.setBold(component.isBold());
        }
        if (this.isItalicEmpty() && !component.isItalicEmpty()) {
            this.setItalic(component.isItalic());
        }
        if (this.isUnderlinedEmpty() && !component.isUnderlinedEmpty()) {
            this.setUnderlined(component.isUnderlined());
        }
        if (this.isStrikethroughEmpty() && !component.isStrikethroughEmpty()) {
            this.setStrikethrough(component.isStrikethrough());
        }
        if (this.isObfuscatedEmpty() && !component.isObfuscatedEmpty()) {
            this.setObfuscated(component.isObfuscated());
        }
        if (this.clickEvent == null && component.clickEvent != null) {
            this.clickEvent = component.clickEvent;
        }
        if (this.hoverEvent == null && component.hoverEvent != null) {
            this.hoverEvent = component.hoverEvent;
        }
        return this;
    }

    public Style getStyle() {
        Style style = Style.EMPTY;
        if (!isColorEmpty())
            style = style.withColor(Color.fromRgb(getColor()));
        style = style.setUnderlined(this.isUnderlined())
                .setStrikethrough(this.isStrikethrough())
                .setObfuscated(this.isObfuscated())
                .withBold(this.isBold())
                .withItalic(this.isItalic())
                .withClickEvent(this.clickEvent)
                .withHoverEvent(this.hoverEvent);
        return style;
    }

    /**
     * 获取文本
     */
    public String toString() {
        return this.getString(this.getLanguageCode());
    }

    /**
     * 获取指定语言文本
     *
     * @param languageCode 语言代码
     */
    public String getString(String languageCode) {
        StringBuilder result = new StringBuilder();
        if (!isColorEmpty()) {
            result.append(StringUtils.rgbToMinecraftColor(getColor()));
        }
        // 添加样式：粗体
        if (isBold()) {
            result.append("§l");
        }
        // 添加样式：斜体
        if (isItalic()) {
            result.append("§o");
        }
        // 添加样式：下划线
        if (isUnderlined()) {
            result.append("§n");
        }
        // 添加样式：中划线
        if (isStrikethrough()) {
            result.append("§m");
        }
        // 添加样式：混淆
        if (isObfuscated()) {
            result.append("§k");
        }
        if (this.i18nType == EI18nType.PLAIN) {
            result.append(this.text);
        } else {
            result.append(I18nUtils.getTranslation(I18nUtils.getKey(this.i18nType, this.text), languageCode));
        }
        this.children.forEach(component -> result.append(component.getString(languageCode)));
        return StringUtils.format(result.toString(), this.args.stream().map(component -> component.getString(languageCode)).toArray());
    }

    /**
     * 获取文本组件
     */
    public ITextComponent toTextComponent() {
        return this.toTextComponent(this.getLanguageCode());
    }

    /**
     * 获取文本组件
     *
     * @param languageCode 语言代码
     */
    public ITextComponent toTextComponent(String languageCode) {
        List<IFormattableTextComponent> components = new ArrayList<>();
        String text;
        if (this.i18nType != EI18nType.PLAIN) {
            text = I18nUtils.getTranslation(I18nUtils.getKey(this.i18nType, this.text), languageCode);
            String[] split = text.split(StringUtils.FORMAT_REGEX);
            for (String s : split) {
                components.add(new StringTextComponent(s).withStyle(this.getStyle()));
            }
            Pattern pattern = Pattern.compile(StringUtils.FORMAT_REGEX);
            Matcher matcher = pattern.matcher(text);
            int i = 0;
            while (matcher.find()) {
                String placeholder = matcher.group();
                int index = placeholder.contains("$") ? StringUtils.toInt(placeholder.split("\\$")[0].substring(1)) - 1 : -1;
                if (index == -1) {
                    index = i;
                }
                Component formattedArg = new Component(placeholder).withStyle(this);
                if (index < this.args.size()) {
                    if (this.args.get(index) == null) {
                        formattedArg = new Component();
                    } else {
                        Component argComponent = this.args.get(index);
                        try {
                            // 颜色代码传递
                            String colorCode = split[i].replaceAll("^.*?((?:§[\\da-fA-FKLMNORklmnor])*)$", "$1");
                            formattedArg = new Component(String.format(placeholder.replaceAll("^%\\d+\\$", "%"), colorCode + argComponent.toString())).withStyle(argComponent);
                        } catch (Exception e) {
                            formattedArg = argComponent;
                        }
                    }
                }
                components.get(i).append(formattedArg.toTextComponent());
                i++;
            }
        } else {
            components.add(new StringTextComponent(this.text).withStyle(this.getStyle()));
        }
        components.addAll(this.children.stream().map(component -> (IFormattableTextComponent) component.toTextComponent(languageCode)).collect(Collectors.toList()));
        IFormattableTextComponent result = components.get(0);
        for (int j = 1; j < components.size(); j++) {
            result.append(components.get(j));
        }
        return result.withStyle(this.getStyle());
    }

    /**
     * 获取翻译文本组件
     */
    public ITextComponent toTranslatedTextComponent() {
        IFormattableTextComponent result;
        if (this.i18nType != EI18nType.PLAIN) {
            Object[] args = this.args.stream().map(component -> {
                if (component.i18nType == EI18nType.PLAIN) {
                    return component.toTextComponent();
                } else {
                    return component.toTranslatedTextComponent();
                }
            }).toArray();
            if (CollectionUtils.isNotNullOrEmpty(args)) {
                result = new TranslationTextComponent(I18nUtils.getKey(this.i18nType, this.text), args);
            } else {
                result = new TranslationTextComponent(I18nUtils.getKey(this.i18nType, this.text));
            }
        } else {
            result = new StringTextComponent(this.text).withStyle(this.getStyle());
        }
        for (Component child : this.children) {
            result.append(child.toTranslatedTextComponent());
        }
        return result;
    }

    /**
     * 获取文本组件
     *
     * @param text 文本
     */
    public static Component literal(String text) {
        return new Component().setText(text);
    }

    /**
     * 获取翻译文本组件
     *
     * @param key  翻译键
     * @param args 参数
     */
    public static Component translatable(String key, Object... args) {
        return new Component(key, EI18nType.NONE).appendArg(args);
    }

    /**
     * 获取翻译文本组件
     *
     * @param type 翻译类型
     * @param key  翻译键
     * @param args 参数
     */
    public static Component translatable(EI18nType type, String key, Object... args) {
        return new Component(key, type).appendArg(args);
    }

    /**
     * 获取翻译文本组件
     *
     * @param languageCode 语言代码
     * @param type         翻译类型
     * @param key          翻译键
     * @param args         参数
     */
    public static Component translatable(String languageCode, EI18nType type, String key, Object... args) {
        return new Component(key, type).setLanguageCode(languageCode).appendArg(args);
    }

}
