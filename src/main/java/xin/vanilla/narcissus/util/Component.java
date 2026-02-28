package xin.vanilla.narcissus.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.enums.EnumI18nType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor
@Accessors(chain = true, fluent = true)
public class Component implements Cloneable, Serializable {

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
    private EnumI18nType i18nType = EnumI18nType.PLAIN;

    /**
     * 子组件
     */
    private List<Component> children = new ArrayList<>();

    /**
     * 翻译组件参数
     */
    private List<Component> args = new ArrayList<>();

    /**
     * 原始组件
     */
    @Getter
    @Setter
    private Object original = null;

    // region 样式属性

    /**
     * 语言代码
     */
    @Getter
    @Setter
    private String languageCode;
    /**
     * 文本颜色
     */
    @Getter
    private xin.vanilla.narcissus.data.Color color = xin.vanilla.narcissus.data.Color.white();
    /**
     * 文本背景色
     */
    @Getter
    private xin.vanilla.narcissus.data.Color bgColor = xin.vanilla.narcissus.data.Color.argb(0x00000000);
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

    public Component(String text, EnumI18nType i18nType) {
        this.text = text;
        this.i18nType = i18nType;
    }

    public Component color(xin.vanilla.narcissus.data.Color color) {
        this.color = color;
        return this;
    }

    public Component color(int rgb) {
        this.color = xin.vanilla.narcissus.data.Color.rgb(rgb);
        return this;
    }

    public Component bgColor(xin.vanilla.narcissus.data.Color color) {
        this.bgColor = color;
        return this;
    }

    public Component bgColor(int rgb) {
        this.bgColor = xin.vanilla.narcissus.data.Color.rgb(rgb);
        return this;
    }

    public Component languageCodeIfEmpty(String languageCode) {
        if (this.isLanguageCodeEmpty()) {
            this.languageCode(languageCode);
        }
        return this;
    }

    // region NonNull Getter

    /**
     * 内容是否为空
     */
    public boolean isEmpty() {
        return StringUtils.isNullOrEmptyEx(this.text())
                && this.original() == null
                && this.getChildren().isEmpty()
                && this.getArgs().isEmpty();
    }

    /**
     * 获取语言代码
     */
    public @NonNull String languageCodeOrDefault(String defaultLanguage) {
        return this.languageCode == null ? defaultLanguage : this.languageCode;
    }

    /**
     * 获取语言代码
     */
    public @NonNull String languageCodeOrDefault() {
        return this.languageCode == null ? ServerConfig.DEFAULT_LANGUAGE.get() : this.languageCode;
    }

    /**
     * 是否有阴影
     */
    public boolean shadow() {
        return this.shadow != null && this.shadow;
    }

    /**
     * 是否粗体
     */
    public boolean bold() {
        return this.bold != null && this.bold;
    }

    /**
     * 是否斜体
     */
    public boolean italic() {
        return this.italic != null && this.italic;
    }

    /**
     * 是否下划线
     */
    public boolean underlined() {
        return this.underlined != null && this.underlined;
    }

    /**
     * 是否中划线
     */
    public boolean strikethrough() {
        return this.strikethrough != null && this.strikethrough;
    }

    /**
     * 是否混淆
     */
    public boolean obfuscated() {
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

    private Component children(List<Component> children) {
        this.children = children;
        return this;
    }

    private Component args(List<Component> args) {
        this.args = args;
        return this;
    }

    public Component clone() {
        try {
            Component component = (Component) super.clone();
            component.text(this.text)
                    .i18nType(this.i18nType)
                    .languageCode(this.languageCode)
                    .color(this.color)
                    .bgColor(this.bgColor)
                    .shadow(this.shadow)
                    .bold(this.bold)
                    .italic(this.italic)
                    .underlined(this.underlined)
                    .strikethrough(this.strikethrough)
                    .obfuscated(this.obfuscated)
                    .clickEvent(this.clickEvent)
                    .hoverEvent(this.hoverEvent);

            if (CollectionUtils.isNotNullOrEmpty(this.getChildren())) {
                List<Component> clonedChildren = new ArrayList<>(this.getChildren().size());
                for (Component child : this.getChildren()) {
                    clonedChildren.add(child != null ? child.clone() : null);
                }
                component.children(clonedChildren);
            } else {
                component.children(null);
            }

            if (CollectionUtils.isNotNullOrEmpty(this.getArgs())) {
                List<Component> clonedArgs = new ArrayList<>(this.getArgs().size());
                for (Component arg : this.getArgs()) {
                    clonedArgs.add(arg != null ? arg.clone() : null);
                }
                component.args(clonedArgs);
            } else {
                component.args(null);
            }

            return component;
        } catch (CloneNotSupportedException e) {
            return empty();
        }
    }

    public Component append(Object... objs) {
        return this.appendIndex(this.getChildren().size(), objs);
    }

    public Component appendIndex(int index, Object... objs) {
        for (int i = 0; i < objs.length; i++) {
            Object obj = objs[i];
            if (obj instanceof Component) {
                this.getChildren().add(index + i, ((Component) obj).withStyle(this));
            } else {
                this.getChildren().add(index + i, new Component(obj.toString()).withStyle(this));
            }
        }
        return this;
    }

    public Component appendArg(Object... objs) {
        return this.appendArgIndex(this.getArgs().size(), objs);
    }

    public Component appendArgIndex(int index, Object... objs) {
        for (int i = 0; i < objs.length; i++) {
            Object obj = objs[i];
            if (obj instanceof Component) {
                this.getArgs().add(index + i, ((Component) obj).withStyle(this));
            } else {
                this.getArgs().add(index + i, new Component(obj.toString()).withStyle(this));
            }
        }
        return this;
    }

    public List<Component> getChildren() {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        return this.children;
    }

    public List<Component> getArgs() {
        if (this.args == null) {
            this.args = new ArrayList<>();
        }
        return this.args;
    }

    public Component clearChildren() {
        if (CollectionUtils.isNotNullOrEmpty(this.children)) {
            this.children = new ArrayList<>();
        }
        return this;
    }

    public Component clearArgs() {
        if (CollectionUtils.isNotNullOrEmpty(this.args)) {
            this.args = new ArrayList<>();
        }
        return this;
    }

    /**
     * 将另一个组件的样式应用到当前组件
     */
    public Component withStyle(Component component) {
        if (this.isLanguageCodeEmpty() && !component.isLanguageCodeEmpty()) {
            this.languageCode(component.languageCode());
        }
        if ((this.color().isEmpty()) && !component.color().isEmpty()) {
            this.color(component.color());
        }
        if ((this.bgColor().isEmpty()) && !component.bgColor().isEmpty()) {
            this.bgColor(component.bgColor());
        }
        if (this.isShadowEmpty() && !component.isShadowEmpty()) {
            this.shadow(component.shadow());
        }
        if (this.isBoldEmpty() && !component.isBoldEmpty()) {
            this.bold(component.bold());
        }
        if (this.isItalicEmpty() && !component.isItalicEmpty()) {
            this.italic(component.italic());
        }
        if (this.isUnderlinedEmpty() && !component.isUnderlinedEmpty()) {
            this.underlined(component.underlined());
        }
        if (this.isStrikethroughEmpty() && !component.isStrikethroughEmpty()) {
            this.strikethrough(component.strikethrough());
        }
        if (this.isObfuscatedEmpty() && !component.isObfuscatedEmpty()) {
            this.obfuscated(component.obfuscated());
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
        if (!this.color().isEmpty() && this.color().rgb() != 0xFFFFFF)
            style = style.withColor(TextColor.fromRgb(color().rgb()));
        style = style.setUnderlined(this.underlined())
                .setStrikethrough(this.strikethrough())
                .setObfuscated(this.obfuscated())
                .withBold(this.bold())
                .withItalic(this.italic())
                .withClickEvent(this.clickEvent)
                .withHoverEvent(this.hoverEvent);
        return style;
    }

    /**
     * 获取文本
     */
    public String toString() {
        return this.getString(this.languageCodeOrDefault(), false, true);
    }

    /**
     * 获取文本
     *
     * @param igStyle 是否忽略样式
     */
    public String toString(boolean igStyle) {
        return this.getString(this.languageCodeOrDefault(), igStyle, true);
    }

    /**
     * 获取指定语言文本
     *
     * @param languageCode 语言代码
     */
    public String getString(String languageCode) {
        return this.getString(languageCode, false, true);
    }

    /**
     * 获取指定语言文本
     *
     * @param languageCode 语言代码
     * @param igStyle      是否忽略样式
     * @param igColor      是否忽略颜色
     */
    public String getString(String languageCode, boolean igStyle, boolean igColor) {
        StringBuilder result = new StringBuilder();
        String colorStr = this.color().isEmpty() ? "§f" : ColorUtils.argbToMinecraftColorString(color().rgb());
        igColor = igColor && colorStr.equalsIgnoreCase("§f");
        // 如果颜色值为透明，则不显示内容，所以返回空文本
        if (!this.color().isEmpty()) {
            if (!igStyle) {
                if (!igColor) {
                    result.append(colorStr);
                }
                // 添加样式：粗体
                if (bold()) {
                    result.append("§l");
                }
                // 添加样式：斜体
                if (italic()) {
                    result.append("§o");
                }
                // 添加样式：下划线
                if (underlined()) {
                    result.append("§n");
                }
                // 添加样式：中划线
                if (strikethrough()) {
                    result.append("§m");
                }
                // 添加样式：混淆
                if (obfuscated()) {
                    result.append("§k");
                }
            }
            if (this.i18nType == EnumI18nType.PLAIN) {
                result.append(this.text);
            } else if (i18nType == EnumI18nType.ORIGINAL) {
                result.append(((net.minecraft.network.chat.Component) this.original).getString());
            } else {
                String fullKey = I18nUtils.getKey(this.i18nType, this.text);
                result.append(I18nUtils.getTranslation(fullKey, this.languageCodeOrDefault(languageCode)));
            }
        }
        boolean finalIgColor = igColor;
        this.getChildren().forEach(component -> result.append(component.getString(languageCode, igStyle, finalIgColor)));
        return StringUtils.format(result.toString(), this.getArgs().stream().map(component -> component.getString(languageCode, igStyle, finalIgColor)).toArray());
    }

    /**
     * 获取文本组件
     */
    public net.minecraft.network.chat.Component toTextComponent() {
        return this.toTextComponent(this.languageCodeOrDefault());
    }

    /**
     * 获取文本组件
     *
     * @param languageCode 语言代码
     */
    public net.minecraft.network.chat.Component toTextComponent(String languageCode) {
        List<MutableComponent> components = new ArrayList<>();
        if (this.i18nType == EnumI18nType.ORIGINAL) {
            components.add((MutableComponent) this.original);
        } else {
            // 如果颜色值为null则说明为透明，则不显示内容，所以返回空文本组件
            if (!this.color().isEmpty()) {
                if (this.i18nType != EnumI18nType.PLAIN) {
                    String text = I18nUtils.getTranslation(this.i18nType, this.text, this.languageCodeOrDefault(languageCode));
                    String[] split = text.split(StringUtils.FORMAT_REGEX, -1);
                    for (String s : split) {
                        components.add(new TextComponent(s).withStyle(this.getStyle()));
                    }
                    Pattern pattern = Pattern.compile(StringUtils.FORMAT_REGEX);
                    Matcher matcher = pattern.matcher(text);
                    int i = 0;
                    while (matcher.find()) {
                        String placeholder = matcher.group();
                        int index = placeholder.contains("$") ? NumberUtils.toInt(placeholder.split("\\$")[0].substring(1)) - 1 : -1;
                        if (index == -1) {
                            index = i;
                        }
                        Component formattedArg = new Component(placeholder).withStyle(this);
                        if (index < this.getArgs().size()) {
                            if (this.getArgs().get(index) == null) {
                                formattedArg = new Component();
                            } else {
                                Component argComponent = this.getArgs().get(index);
                                if (argComponent.i18nType() != EnumI18nType.PLAIN) {
                                    // 语言代码传递
                                    if (argComponent.isLanguageCodeEmpty()) {
                                        argComponent.languageCode(languageCode);
                                    }
                                    try {
                                        // 颜色代码传递
                                        String colorCode = split[i].replaceAll("^.*?((?:§[\\da-fA-FKLMNORklmnor])*)$", "$1");
                                        formattedArg = new Component(String.format(placeholder.replaceAll("^%\\d+\\$", "%"), colorCode + argComponent)).withStyle(argComponent);
                                    } catch (Exception e) {
                                        // 颜色传递
                                        if (argComponent.color().isEmpty()) {
                                            argComponent.color(this.color);
                                        }
                                        formattedArg = argComponent;
                                    }
                                } else {
                                    // 颜色传递
                                    if (argComponent.color().isEmpty()) {
                                        argComponent.color(this.color);
                                    }
                                    formattedArg = argComponent;
                                }
                            }
                        }
                        if (components.size() > i) {
                            components.get(i).append(formattedArg.toTextComponent());
                        }
                        i++;
                    }
                } else {
                    this.args.forEach(arg -> arg.languageCodeIfEmpty(languageCode));
                    components.add(new TextComponent(StringUtils.format(this.text, this.args.toArray())).withStyle(this.getStyle()));
                }
            }
        }
        components.addAll(this.getChildren().stream().map(component -> (MutableComponent) component.toTextComponent(languageCode)).toList());
        if (components.isEmpty()) {
            components.add(new TextComponent(""));
        }
        MutableComponent result = components.get(0);
        for (int j = 1; j < components.size(); j++) {
            result.append(components.get(j));
        }
        return result.withStyle(this.getStyle());
    }

    /**
     * 获取翻译文本组件
     */
    public net.minecraft.network.chat.Component toTranslatedTextComponent() {
        MutableComponent result = new TranslatableComponent("");
        if (!this.color().isEmpty() || !this.bgColor().isEmpty()) {
            if (this.i18nType != EnumI18nType.PLAIN) {
                Object[] objects = this.getArgs().stream().map(component -> {
                    if (component.i18nType == EnumI18nType.PLAIN) {
                        return component.toTextComponent();
                    } else {
                        return component.toTranslatedTextComponent();
                    }
                }).toArray();
                String fullKey = I18nUtils.getKey(this.i18nType, this.text);
                if (CollectionUtils.isNotNullOrEmpty(objects)) {
                    result = new TranslatableComponent(fullKey, objects);
                } else {
                    result = new TranslatableComponent(fullKey);
                }
            } else {
                result = new TextComponent(this.text).withStyle(this.getStyle());
            }
        }
        for (Component child : this.getChildren()) {
            result.append(child.toTranslatedTextComponent());
        }
        return result;
    }

    /**
     * 获取聊天文本组件
     *
     * @return 格式化颜色后的文本组件
     */
    public net.minecraft.network.chat.Component toChatComponent() {
        return this.toChatComponent(this.languageCodeOrDefault());
    }

    /**
     * 获取聊天文本组件
     *
     * @return 格式化颜色后的文本组件
     */
    public net.minecraft.network.chat.Component toChatComponent(String languageCode) {
        return rewriteColor(this.toTextComponent(languageCode));
    }

    public static net.minecraft.network.chat.Component rewriteColor(net.minecraft.network.chat.Component component) {
        if (component instanceof MutableComponent) {
            TextColor color = component.getStyle().getColor();
            if (color != null && color.serialize().startsWith("#")) {
                Style style = component.getStyle().withColor(TextColor.parseColor(ColorUtils.argbToMinecraftColor(ColorUtils.parseArgb(color.serialize())).name().toLowerCase()));
                ((MutableComponent) component).setStyle(style);
            }
        }
        for (net.minecraft.network.chat.Component sibling : component.getSiblings()) {
            rewriteColor(sibling);
        }
        return component;
    }

    /**
     * 获取空文本组件
     */
    public static Component empty() {
        return new Component();
    }

    /**
     * 获取原始组件
     */
    public static Component object(Object original) {
        return empty().original(original).i18nType(EnumI18nType.ORIGINAL);
    }

    /**
     * 获取文本组件
     *
     * @param text 文本
     */
    public static Component literal(String text) {
        return new Component().text(text);
    }

    /**
     * 获取翻译文本组件
     *
     * @param key  翻译键
     * @param args 参数
     */
    public static Component trans(String key, Object... args) {
        return new Component(key, EnumI18nType.NONE).appendArg(args);
    }

    /**
     * 获取翻译文本组件
     *
     * @param key 翻译键
     */
    public static Component trans(String key) {
        return new Component(key, EnumI18nType.NONE);
    }

    /**
     * 获取翻译文本组件
     */
    public static Component trans(EnumI18nType type, String key, Object... args) {
        return new Component(key, type).appendArg(args);
    }

    /**
     * 获取翻译文本组件
     */
    public static Component trans(String languageCode, EnumI18nType type, String key, Object... args) {
        return new Component(key, type).languageCode(languageCode).appendArg(args);
    }

    /**
     * 获取翻译文本组件
     *
     * @param key  翻译键
     * @param args 参数
     */
    public static Component transClient(String key, Object... args) {
        return new Component(key, EnumI18nType.NONE).languageCode(I18nUtils.getClientLanguage()).appendArg(args);
    }

    /**
     * 获取翻译文本组件
     *
     * @param key 翻译键
     */
    public static Component transClient(String key) {
        return new Component(key, EnumI18nType.NONE).languageCode(I18nUtils.getClientLanguage());
    }

    /**
     * 获取翻译文本组件
     *
     * @param type 翻译类型
     * @param key  翻译键
     * @param args 参数
     */
    public static Component transClient(EnumI18nType type, String key, Object... args) {
        return new Component(key, type).languageCode(I18nUtils.getClientLanguage()).appendArg(args);
    }

    /**
     * 获取翻译文本组件
     *
     * @param player 玩家
     * @param type   翻译类型
     * @param key    翻译键
     * @param args   参数
     */
    public static Component trans(ServerPlayer player, EnumI18nType type, String key, Object... args) {
        return new Component(key, type).languageCode(I18nUtils.getPlayerLanguage(player)).appendArg(args);
    }

    public static Component deserialize(JsonObject jsonObject) {
        Component result = new Component();
        result.text(JsonUtils.getString(jsonObject, "text"));
        result.i18nType(EnumI18nType.valueOf(JsonUtils.getString(jsonObject, "i18nType")));
        result.languageCode(JsonUtils.getString(jsonObject, "languageCode", null));
        result.color(xin.vanilla.narcissus.data.Color.argb(JsonUtils.getInt(jsonObject, "color", 0)));
        result.bgColor(xin.vanilla.narcissus.data.Color.argb(JsonUtils.getInt(jsonObject, "bgColor", 0)));
        result.shadow(JsonUtils.getBoolean(jsonObject, "shadow", false));
        result.bold(JsonUtils.getBoolean(jsonObject, "bold", false));
        result.italic(JsonUtils.getBoolean(jsonObject, "italic", false));
        result.underlined(JsonUtils.getBoolean(jsonObject, "underlined", false));
        result.strikethrough(JsonUtils.getBoolean(jsonObject, "strikethrough", false));
        result.obfuscated(JsonUtils.getBoolean(jsonObject, "obfuscated", false));
        String clickAction = JsonUtils.getString(jsonObject, "clickEvent.action", "");
        String clickValue = JsonUtils.getString(jsonObject, "clickEvent.value", "");
        if (StringUtils.isNotNullOrEmpty(clickAction) && StringUtils.isNotNullOrEmpty(clickValue)) {
            result.clickEvent(new ClickEvent(ClickEvent.Action.valueOf(clickAction), clickValue));
        }
        JsonObject hover = JsonUtils.getJsonObject(jsonObject, "hoverEvent", null);
        if (hover != null) {
            result.hoverEvent(HoverEvent.deserialize(hover));
        }
        for (JsonElement childJson : JsonUtils.getJsonArray(jsonObject, "children", new JsonArray())) {
            result.getChildren().add(deserialize((JsonObject) childJson));
        }
        for (JsonElement argJson : JsonUtils.getJsonArray(jsonObject, "args", new JsonArray())) {
            result.getArgs().add(deserialize((JsonObject) argJson));
        }
        return result;
    }

    public static JsonObject serialize(Component component) {
        JsonObject result = new JsonObject();
        JsonUtils.set(result, "text", component.text());
        JsonUtils.set(result, "i18nType", component.i18nType().name());
        if (!component.isLanguageCodeEmpty()) {
            JsonUtils.set(result, "languageCode", component.languageCode());
        }
        if (!component.color().isEmpty()) {
            JsonUtils.set(result, "color", component.color().argb());
        }
        if (!component.bgColor().isEmpty()) {
            JsonUtils.set(result, "bgColor", component.bgColor().argb());
        }
        if (component.shadow()) {
            JsonUtils.set(result, "shadow", component.shadow());
        }
        if (component.bold()) {
            JsonUtils.set(result, "bold", component.bold());
        }
        if (component.italic()) {
            JsonUtils.set(result, "italic", component.italic());
        }
        if (component.underlined()) {
            JsonUtils.set(result, "underlined", component.underlined());
        }
        if (component.strikethrough()) {
            JsonUtils.set(result, "strikethrough", component.strikethrough());
        }
        if (component.obfuscated()) {
            JsonUtils.set(result, "obfuscated", component.obfuscated());
        }
        if (component.clickEvent() != null) {
            JsonUtils.set(result, "clickEvent.action", component.clickEvent().getAction().getName());
            JsonUtils.set(result, "clickEvent.value", component.clickEvent().getValue());
        }
        if (component.hoverEvent() != null) {
            JsonUtils.set(result, "hoverEvent", component.hoverEvent().serialize());
        }
        JsonArray children = new JsonArray();
        for (Component child : component.getChildren()) {
            children.add(serialize(child));
        }
        JsonUtils.set(result, "children", children);
        JsonArray args = new JsonArray();
        for (Component arg : component.getArgs()) {
            args.add(serialize(arg));
        }
        JsonUtils.set(result, "args", args);
        return result;
    }

    public JsonObject toJson() {
        return serialize(this);
    }

}
