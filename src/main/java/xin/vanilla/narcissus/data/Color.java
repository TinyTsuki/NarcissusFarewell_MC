package xin.vanilla.narcissus.data;

import lombok.ToString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.util.StringUtils;

import javax.annotation.Nullable;

@ToString
@SuppressWarnings("unused")
public class Color {
    private static final Logger LOGGER = LogManager.getLogger();

    private int red = 0xFF;
    private int green = 0xFF;
    private int blue = 0xFF;

    /**
     * 颜色
     */
    private int rgb = 0xFFFFFF;
    /**
     * 透明度
     */
    private int alpha = 0xFF;

    private Color() {
    }


    // region fluent

    @Nullable
    public Integer color() {
        return getColor();
    }

    public int rgb() {
        return this.rgb;
    }

    public int alpha() {
        return this.alpha;
    }

    public int red() {
        return this.red;
    }

    public int green() {
        return this.green;
    }

    public int blue() {
        return this.blue;
    }

    public int argb() {
        return this.getArgb();
    }

    public int rgba() {
        return this.getRgba();
    }

    //  endregion fluent


    //  region getter

    /**
     * 获取颜色
     */
    @Nullable
    public Integer getColor() {
        return this.isEmpty() ? null : this.rgb();
    }

    /**
     * 获取RGB
     */
    public int getRgb() {
        return this.rgb();
    }

    /**
     * 获取透明度
     */
    public int getAlpha() {
        return this.alpha();
    }

    /**
     * 获取红色分量
     */
    public int getRed() {
        return this.red & 0xFF;
    }

    /**
     * 获取绿色分量
     */
    public int getGreen() {
        return this.green & 0xFF;
    }

    /**
     * 获取蓝色分量
     */
    public int getBlue() {
        return this.blue & 0xFF;
    }

    /**
     * 获取ARGB格式颜色
     */
    public int getArgb() {
        int a = this.alpha & 0xFF;
        int r = this.red & 0xFF;
        int g = this.green & 0xFF;
        int b = this.blue & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * 获取RGBA格式颜色
     */
    public int getRgba() {
        int a = this.alpha & 0xFF;
        int r = this.red & 0xFF;
        int g = this.green & 0xFF;
        int b = this.blue & 0xFF;
        return (r << 24) | (g << 16) | (b << 8) | a;
    }

    /**
     * 颜色是否为空
     */
    public boolean isEmpty() {
        return this.alpha == 0x00;
    }

    //  endregion getter


    //  region setter

    /**
     * 设置颜色
     */
    public Color setColor(Integer rgb) {
        if (rgb == null) {
            this.alpha = 0x00;
            this.rgb = 0x000000;
            this.red = 0x00;
            this.green = 0x00;
            this.blue = 0x00;
        } else {
            int cleanRgb = rgb & 0xFFFFFF;
            this.red = (cleanRgb >> 16) & 0xFF;
            this.green = (cleanRgb >> 8) & 0xFF;
            this.blue = cleanRgb & 0xFF;
            this.rgb = cleanRgb;
        }
        return this;
    }

    /**
     * 设置RGB
     */
    public Color setRgb(int rgb) {
        int cleanRgb = rgb & 0xFFFFFF;
        this.red = (cleanRgb >> 16) & 0xFF;
        this.green = (cleanRgb >> 8) & 0xFF;
        this.blue = cleanRgb & 0xFF;
        this.rgb = cleanRgb;
        return this;
    }

    public Color setR(int r) {
        this.red = r & 0xFF;
        this.rgb = (this.red << 16) | (this.green << 8) | this.blue;
        return this;
    }

    public Color setG(int g) {
        this.green = g & 0xFF;
        this.rgb = (this.red << 16) | (this.green << 8) | this.blue;
        return this;
    }

    public Color setB(int b) {
        this.blue = b & 0xFF;
        this.rgb = (this.red << 16) | (this.green << 8) | this.blue;
        return this;
    }

    /**
     * 设置透明度
     */
    public Color setAlpha(int alpha) {
        this.alpha = alpha & 0xFF;
        return this;
    }

    /**
     * 设置ARGB颜色
     */
    public Color setArgb(Integer argb) {
        if (argb == null) {
            this.alpha = 0x00;
            this.rgb = 0x000000;
            this.red = 0x00;
            this.green = 0x00;
            this.blue = 0x00;
        } else {
            this.alpha = (argb >> 24) & 0xFF;
            this.red = (argb >> 16) & 0xFF;
            this.green = (argb >> 8) & 0xFF;
            this.blue = argb & 0xFF;
            this.rgb = (this.red << 16) | (this.green << 8) | this.blue;
        }
        return this;
    }

    /**
     * 设置RGBA颜色
     */
    public Color setRgba(Integer rgba) {
        if (rgba == null) {
            this.alpha = 0x00;
            this.rgb = 0x000000;
            this.red = 0x00;
            this.green = 0x00;
            this.blue = 0x00;
        } else {
            this.alpha = rgba & 0xFF;
            this.red = (rgba >> 24) & 0xFF;
            this.green = (rgba >> 16) & 0xFF;
            this.blue = (rgba >> 8) & 0xFF;
            this.rgb = (this.red << 16) | (this.green << 8) | this.blue;
        }
        return this;
    }

    //  endregion setter

    //  region toString

    public String toArgbHexString() {
        return String.format("#%08X", this.getArgb());
    }

    public String toRgbaHexString() {
        return String.format("#%08X", this.getRgba());
    }

    public String toRgbDecimalString() {
        return String.format("%s,%s,%s", this.red, this.green, this.blue);
    }

    public String toArgbDecimalString() {
        return String.format("%s,%s,%s,%s", this.alpha, this.red, this.green, this.blue);
    }

    public String toRgbHexString() {
        return String.format("#%06X", this.rgb);
    }

    //  endregion toString

    //  region static

    public static Color white() {
        return new Color();
    }

    public static Color black() {
        return new Color().setColor(0x000000);
    }

    public static Color empty() {
        return new Color().setAlpha(0x00);
    }


    public static Color fromRgb(Integer rgb) {
        return rgb(rgb);
    }

    public static Color fromRgb(Integer r, Integer g, Integer b) {
        return rgb(r, g, b);
    }

    public static Color rgb(Integer rgb) {
        return new Color().setColor(rgb);
    }

    public static Color rgb(Integer r, Integer g, Integer b) {
        if (r == null || g == null || b == null) {
            return empty();
        }
        return rgb((r << 16) | (g << 8) | b);
    }


    public static Color fromArgb(Integer argb) {
        return argb(argb);
    }

    public static Color fromARgb(Integer a, Integer r, Integer g, Integer b) {
        return argb(a, r, g, b);
    }

    public static Color argb(Integer argb) {
        return new Color().setArgb(argb);
    }

    public static Color argb(Integer a, Integer r, Integer g, Integer b) {
        if (a == null || r == null || g == null || b == null) {
            return empty();
        }
        return rgb((r << 16) | (g << 8) | b).setAlpha(a);
    }


    public static Color fromRgba(Integer rgba) {
        return rgba(rgba);
    }

    public static Color fromRgba(Integer r, Integer g, Integer b, Integer a) {
        return argb(a, r, g, b);
    }

    public static Color rgba(Integer rgba) {
        return new Color().setRgba(rgba);
    }

    public static Color rgba(Integer r, Integer g, Integer b, Integer a) {
        return argb(a, r, g, b);
    }


    public static Color fromAbgr(Integer abgr) {
        return abgr(abgr);
    }

    public static Color fromAbgr(Integer a, Integer b, Integer g, Integer r) {
        return argb(a, r, g, b);
    }

    public static Color abgr(Integer abgr) {
        if (abgr == null) {
            return empty();
        } else {
            int alpha = (abgr >> 24) & 0xFF;
            int blue = (abgr >> 16) & 0xFF;
            int green = (abgr >> 8) & 0xFF;
            int red = abgr & 0xFF;
            return argb(alpha, red, green, blue);
        }
    }

    public static Color abgr(Integer a, Integer b, Integer g, Integer r) {
        return argb(a, r, g, b);
    }

    /**
     * 解析颜色格式字符串
     * <p>
     * 支持的格式：
     * <p>
     * 十六进制：0xAARRGGBB、0xRRGGBB、#RRGGBB、#AARRGGBB
     * <p>
     * 十进制：R,G,B、A,R,G,B
     */
    public static Color parse(String colorStr) {
        return parse(colorStr, empty());
    }

    /**
     * 解析颜色格式字符串
     * <p>
     * 支持的格式：
     * <p>
     * 十六进制：0xAARRGGBB、0xRRGGBB、#RRGGBB、#AARRGGBB
     * <p>
     * 十进制：R,G,B、A,R,G,B
     */
    public static Color parse(String colorStr, Color defaultColor) {
        if (StringUtils.isNullOrEmptyEx(colorStr)) {
            LOGGER.debug("Color parse error: colorStr is null or empty");
            return defaultColor;
        }
        Color color = defaultColor;

        colorStr = colorStr.trim().toUpperCase();

        try {
            if (colorStr.startsWith("0X") || colorStr.startsWith("#")) {
                color = parseHexColor(colorStr);
            } else if (colorStr.contains(",")) {
                color = parseDecimalColor(colorStr);
            } else {
                LOGGER.debug("Color parse error: unsupported color format: {}", colorStr);
            }
        } catch (Exception e) {
            LOGGER.debug("Color parse error: failed to parse color string: {} - {}", colorStr, e.getMessage());
        }
        return color;
    }

    /**
     * 解析十六进制颜色格式
     */
    private static Color parseHexColor(String hexStr) {
        // 移除前缀
        String cleanHex = hexStr.replace("0X", "").replace("#", "");

        int alpha = 0xFF;
        int red, green, blue;

        // RGB (6位)
        if (cleanHex.length() == 6) {
            red = Integer.parseInt(cleanHex.substring(0, 2), 16);
            green = Integer.parseInt(cleanHex.substring(2, 4), 16);
            blue = Integer.parseInt(cleanHex.substring(4, 6), 16);
        }
        // ARGB (8位)
        else if (cleanHex.length() == 8) {
            alpha = Integer.parseInt(cleanHex.substring(0, 2), 16);
            red = Integer.parseInt(cleanHex.substring(2, 4), 16);
            green = Integer.parseInt(cleanHex.substring(4, 6), 16);
            blue = Integer.parseInt(cleanHex.substring(6, 8), 16);
        } else {
            throw new IllegalArgumentException("Hex color length must be 6 (RRGGBB) or 8 (AARRGGBB), got: " + cleanHex.length());
        }
        return argb(alpha, red, green, blue);
    }

    /**
     * 解析十进制颜色格式
     */
    private static Color parseDecimalColor(String decimalStr) {
        String[] parts = decimalStr.split(",");

        int red, green, blue;
        int alpha = 0xFF;

        // RGB (R,G,B)
        if (parts.length == 3) {
            red = Integer.parseInt(parts[0].trim());
            green = Integer.parseInt(parts[1].trim());
            blue = Integer.parseInt(parts[2].trim());
        }
        // ARGB (A,R,G,B)
        else if (parts.length == 4) {
            alpha = Integer.parseInt(parts[0].trim());
            red = Integer.parseInt(parts[1].trim());
            green = Integer.parseInt(parts[2].trim());
            blue = Integer.parseInt(parts[3].trim());
        } else {
            throw new IllegalArgumentException("Decimal color must have 3 (R,G,B) or 4 (A,R,G,B) components, got: " + parts.length);
        }

        // 验证数值范围
        validateColorComponent(red, "red");
        validateColorComponent(green, "green");
        validateColorComponent(blue, "blue");
        validateColorComponent(alpha, "alpha");

        return argb(alpha, red, green, blue);
    }

    /**
     * 验证颜色分量范围 (0-255)
     */
    private static void validateColorComponent(int value, String name) {
        if (value < 0x00 || value > 0xFF) {
            throw new IllegalArgumentException(name + " must be in range 0x00 - 0xFF");
        }
    }

    //  endregion static

}
