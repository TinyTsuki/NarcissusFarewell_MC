package xin.vanilla.narcissus.util;


import xin.vanilla.narcissus.data.Color;
import xin.vanilla.narcissus.enums.EnumMCColor;

import javax.annotation.Nullable;

public final class ColorUtils {
    private ColorUtils() {
    }

    /**
     * 解析ARGB颜色字符串
     * 支持格式：#AARRGGBB、#RRGGBB、0xAARRGGBB、0xRRGGBB
     * 若为6位十六进制（RGB格式），默认alpha为0xFF
     */
    public static int parseArgb(String argb) {
        if (argb == null || argb.isEmpty()) {
            return 0;
        }
        try {
            String cleanHex;
            if (argb.startsWith("#")) {
                cleanHex = argb.substring(1);
            } else if (argb.startsWith("0x") || argb.startsWith("0X")) {
                cleanHex = argb.substring(2);
            } else {
                cleanHex = argb;
            }

            long value = Long.parseLong(cleanHex, 16);

            // 如果是6位十六进制（RGB格式），添加默认alpha值0xFF
            if (cleanHex.length() == 6) {
                return (int) ((0xFFL << 24) | value);
            }
            // 如果是8位十六进制（ARGB格式），直接返回
            return (int) value;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * RGB颜色转换为Minecraft颜色代码
     */
    public static String argbToMinecraftColorString(int color) {
        return "§" + argbToMinecraftColor(color).getCode();
    }

    /**
     * 将ARGB颜色转换为最接近的Minecraft颜色
     */
    public static EnumMCColor argbToMinecraftColor(int color) {
        // 获取 RGB 分量（忽略alpha通道）
        // 无论输入是ARGB还是RGB格式，RGB部分都在低24位
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;

        // 颜色匹配
        double closestDistance = Double.MAX_VALUE;
        // 默认为白色
        EnumMCColor result = EnumMCColor.WHITE;
        for (EnumMCColor mcColor : EnumMCColor.values()) {
            int colorRGB = mcColor.getColor();
            int r = (colorRGB >> 16) & 0xFF;
            int g = (colorRGB >> 8) & 0xFF;
            int b = colorRGB & 0xFF;
            // 加权欧几里得距离计算（人眼对绿色最敏感，蓝色最不敏感）
            double distance = Math.sqrt(2 * Math.pow(red - r, 2) + 4 * Math.pow(green - g, 2) + 3 * Math.pow(blue - b, 2));
            if (distance < closestDistance) {
                closestDistance = distance;
                result = mcColor;
            }
        }
        return result;
    }

    public static int getProbabilityArgb(double probability) {
        int argb = 0xFF000000;
        // 默认不渲染
        if (probability == 1) {
            argb = 0x00FFFFFF;
        }
        // 深灰色，最低级
        else if (probability >= 0.9) {
            argb = 0xEFA9A9A9;
        }
        // 灰色，低级
        else if (probability >= 0.8) {
            argb = 0xEFC0C0C0;
        }
        // 白色，普通
        else if (probability >= 0.7) {
            argb = 0xEFFFFFFF;
        }
        // 亮绿色，良好
        else if (probability >= 0.6) {
            argb = 0xEF32CD32;
        }
        // 深绿色，优秀
        else if (probability >= 0.5) {
            argb = 0xEF228B22;
        }
        // 蓝色，稀有
        else if (probability >= 0.4) {
            argb = 0xEF1E90FF;
        }
        // 深蓝色，稀有
        else if (probability >= 0.3) {
            argb = 0xEF4682B4;
        }
        // 紫色，史诗
        else if (probability >= 0.2) {
            argb = 0xEFA020F0;
        }
        // 金色，传说
        else if (probability >= 0.1) {
            argb = 0xEFFFD700;
        }
        // 橙红色，终极
        else if (probability > 0) {
            argb = 0xEFFF4500;
        }
        return argb;
    }

    /**
     * 获取颜色的亮度（感知亮度）
     * 使用ITU-R BT.709标准权重计算
     *
     * @return 亮度值，范围0.0-1.0
     */
    public static float getBrightness(int rgba) {
        // RGBA格式：R在24-31位，G在16-23位，B在8-15位，A在0-7位
        int r = (rgba >> 24) & 0xFF;
        int g = (rgba >> 16) & 0xFF;
        int b = (rgba >> 8) & 0xFF;
        // 使用ITU-R BT.709标准权重（人眼对绿色最敏感）
        return (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f;
    }

    /**
     * 获取ARGB格式颜色的亮度
     *
     * @return 亮度值，范围0.0-1.0
     */
    public static float getBrightnessFromArgb(int argb) {
        // ARGB格式：A在24-31位，R在16-23位，G在8-15位，B在0-7位
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        // 使用ITU-R BT.709标准权重
        return (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f;
    }

    public static boolean isArgbEmpty(int argb) {
        int alpha = (argb >> 24) & 0xFF;
        return alpha == 0x00;
    }

    public static boolean isRgbaEmpty(int rgba) {
        int alpha = rgba & 0xFF;
        return alpha == 0x00;
    }

    /**
     * 柔化颜色
     * 通过调整RGB分量向中间值（128）靠近来实现柔化效果
     */
    public static int softenArgb(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;

        // 向中间值（128）调整，使颜色更柔和
        int r1 = Math.max(0, Math.min(255, r + (r > 128 ? -30 : 30)));
        int g1 = Math.max(0, Math.min(255, g + (g > 128 ? -30 : 30)));
        int b1 = Math.max(0, Math.min(255, b + (b > 128 ? -30 : 30)));
        return (a << 24) | (r1 << 16) | (g1 << 8) | b1;
    }

    // region Color 类集成方法

    /**
     * 使用Color.parse方法解析并返回Color对象
     */
    public static @Nullable Color parseColor(String colorStr) {
        if (colorStr == null || colorStr.isEmpty()) {
            return null;
        }
        try {
            return Color.parse(colorStr);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将ARGB颜色值转换为Color对象
     */
    public static Color argbToColor(int argb) {
        return Color.argb(argb);
    }

    /**
     * 将Color对象转换为Minecraft颜色代码字符串
     *
     */
    public static String colorToMinecraftColorString(Color color) {
        if (color == null || color.isEmpty()) {
            return "";
        }
        return argbToMinecraftColorString(color.argb());
    }

    /**
     * 柔化Color对象的颜色
     *
     */
    public static Color softenColor(Color color) {
        if (color == null) {
            return null;
        }
        return Color.argb(softenArgb(color.argb()));
    }

    // endregion

}
