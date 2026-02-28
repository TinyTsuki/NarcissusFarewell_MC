package xin.vanilla.narcissus.util;


import lombok.NonNull;
import xin.vanilla.narcissus.data.KeyValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public final class StringUtils {
    private StringUtils() {
    }

    public static final String FORMAT_REGEX = "%(\\d+\\$)?([-#+ 0,(<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])";


    /**
     * 将字符串转为 boolean
     */
    public static boolean stringToBoolean(String s) {
        if (null == s) return false;
        switch (s.toLowerCase(Locale.ROOT).trim()) {
            case "1":
            case "真":
            case "是":
            case "true":
            case "y":
            case "t":
                return true;
            case "0":
            case "假":
            case "否":
            case "false":
            case "n":
            case "f":
            default:
                return false;
        }
    }

    public static boolean isNullOrEmpty(String s) {
        return null == s || s.isEmpty();
    }

    public static boolean isNullOrEmptyEx(String s) {
        return null == s || s.trim().isEmpty();
    }

    public static boolean isNotNullOrEmpty(String s) {
        return s != null && !s.isEmpty();
    }

    public static boolean isNotNull(Object s) {
        return s != null;
    }

    public static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    public static String firstChar(String s) {
        if (s == null || s.isEmpty()) return "";
        int end = s.offsetByCodePoints(0, 1);
        return s.substring(0, end);
    }

    public static String substring(String s, int start, int end) {
        if (isNullOrEmpty(s)) {
            return "";
        }
        int length = s.length();
        if (end < start) {
            return s;
        }
        if (length >= start && length >= end) {
            return s.substring(start, end);
        }
        return s;
    }

    public static String substring(String s, int start) {
        if (isNullOrEmpty(s)) {
            return "";
        }
        int length = s.length();
        if (start > length) {
            return s;
        }
        return s.substring(start);
    }

    public static String substringEnd(String s, int len) {
        if (isNullOrEmpty(s)) {
            return "";
        }
        int length = s.length();
        if (len > length) {
            return s;
        }
        return s.substring(0, length - len);
    }

    public static String toString(String s, String emptyDefault) {
        return StringUtils.isNullOrEmpty(s) ? emptyDefault : s;
    }

    /**
     * 替换换行符
     */
    @NonNull
    public static String replaceLineBreak(String s) {
        if (s == null) return "";
        return s.replaceAll("<br>", "\n")
                .replaceAll("\\\\n", "\n")
                .replaceAll("\\\\r", "\r")
                .replaceAll("\\n", "\n")
                .replaceAll("\\r", "\r")
                .replaceAll("\r\n", "\n");
    }

    public static int getLineCount(String s) {
        if (StringUtils.isNullOrEmpty(s)) return 0;
        return StringUtils.replaceLineBreak(s).split("\n").length;
    }

    /**
     * 获取指定数量的某个字符串
     */
    public static String getString(String s, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    /**
     * 自定义格式化方法，支持位置重排
     *
     * @param string 格式化字符串
     * @param args   参数
     * @return 格式化后的字符串
     */
    public static String format(String string, Object... args) {
        StringBuilder result = new StringBuilder();
        // 使用正则匹配格式化占位符
        Pattern pattern = Pattern.compile(FORMAT_REGEX);
        Matcher matcher = pattern.matcher(string);
        int i = 0;
        while (matcher.find()) {
            // 获取当前占位符
            String placeholder = matcher.group();

            // 获取位置标识符，如 %1$s 中的 1
            int index = placeholder.contains("$") ? NumberUtils.toInt(placeholder.split("\\$")[0].substring(1)) - 1 : -1;
            // 如果占位符中没有显式的数字索引，则默认按顺序处理
            if (index == -1) {
                index = i;
            }
            // 检查是否有足够的参数
            String formattedArg = placeholder;
            if (index < args.length) {
                formattedArg = formatArgument(placeholder, args[index]);
            }
            // 替换占位符为对应的参数
            string = string.replaceFirst(Pattern.quote(placeholder), formattedArg.replaceAll("\\$", "\\\\\\$"));
            i++;
        }
        return string;
    }

    /**
     * 根据占位符的类型格式化参数
     *
     * @param placeholder 占位符
     * @param arg         参数
     */
    private static String formatArgument(String placeholder, Object arg) {
        if (arg == null) return "null";
        try {
            return String.format(placeholder.replaceAll("^%\\d+\\$", "%"), arg);
        } catch (Exception e) {
            // 如果出现异常，直接转换为字符串
            return arg.toString();
        }
    }

    /**
     * 将字符串转换为驼峰命名
     */
    public static String toPascalCase(String input) {
        if (isNullOrEmptyEx(input)) return "";

        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : input.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            } else {
                capitalizeNext = true;
            }
        }
        return result.toString();
    }

    /**
     * 将字符串转换为小写蛇形命名
     */
    public static String toSnakeCase(String input) {
        if (isNullOrEmptyEx(input)) return "";

        StringBuilder result = new StringBuilder();
        boolean firstChar = true;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (Character.isLetterOrDigit(c)) {
                if (!firstChar && Character.isUpperCase(c)) {
                    char prevChar = input.charAt(i - 1);
                    if (Character.isLetterOrDigit(prevChar) && !Character.isUpperCase(prevChar)) {
                        result.append('_');
                    } else if (i < input.length() - 1) {
                        char nextChar = input.charAt(i + 1);
                        if (Character.isLowerCase(nextChar)) {
                            result.append('_');
                        }
                    }
                }
                result.append(Character.toLowerCase(c));
                firstChar = false;
            } else if (c != '_') {
                if (!firstChar && i < input.length() - 1) {
                    char nextChar = input.charAt(i + 1);
                    if (Character.isLetterOrDigit(nextChar)) {
                        result.append('_');
                    }
                }
            }
        }
        return result.toString();
    }

    /**
     * 将字符串转换为大写蛇形命名
     */
    public static String toSnakeCaseUpper(String input) {
        return toSnakeCase(input).toUpperCase(Locale.ROOT);
    }

    /**
     * 检查字符串是否被 left 和 right 包裹
     */
    public static boolean isWrappedBy(String str, KeyValue<String, String> keyValue) {
        return isWrappedBy(str, keyValue.left(), keyValue.right());
    }

    /**
     * 检查字符串是否被 left 和 right 包裹
     */
    public static boolean isWrappedBy(String str, String left, String right) {
        if (str == null || left == null || right == null) {
            return false;
        }
        if (str.length() < left.length() + right.length()) {
            return false;
        }
        return str.startsWith(left) && str.endsWith(right);
    }

    /**
     * 删除字符串的 left 和 right
     */
    public static String unwrap(String str, KeyValue<String, String> keyValue) {
        return unwrap(str, keyValue.left(), keyValue.right());
    }

    /**
     * 删除字符串的 left 和 right 包裹
     */
    public static String unwrap(String str, String left, String right) {
        if (str == null || left == null || right == null) {
            return str;
        }
        if (isWrappedBy(str, left, right)) {
            return str.substring(left.length(), str.length() - right.length());
        }
        return str;
    }

    public static String padOptimizedLeft(Object value, int length, String padChar) {
        return padOptimized(value, length, padChar, true);
    }

    public static String padOptimizedRight(Object value, int length, String padChar) {
        return padOptimized(value, length, padChar, false);
    }

    /**
     * 在字符串前或后补全字符
     */
    public static String padOptimized(Object value, int length, String padChar, boolean left) {
        String str = String.valueOf(value);
        int currentLength = str.length();

        if (length <= currentLength) return str;

        char paddingChar = padChar != null && !padChar.isEmpty() ? padChar.charAt(0) : ' ';
        char[] chars = new char[length - currentLength];
        Arrays.fill(chars, paddingChar);

        return left ? new String(chars) + str : str + new String(chars);
    }

    /**
     * 判断给定字符串是否仅包含\w字符
     */
    public static boolean isWordString(String str) {
        if (StringUtils.isNullOrEmptyEx(str)) return false;
        return str.matches("^\\w+$");
    }

    /**
     * 若input不为 仅包含\w字符 的字符串
     * 则将input格式化为 'input'，并将input中的'转义为\'
     */
    public static String formatString(String input) {
        if (isWordString(input)) return input;
        return "'" + input.replaceAll("'", "\\\\'") + "'";
    }

    /**
     * 计算字符串的 MD5 值
     *
     * @param input 输入字符串
     * @return 32位小写 MD5 值
     */
    public static String md5(String input) {
        if (input == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    /**
     * 计算文件的 MD5 值
     *
     * @param file 目标文件
     * @return 32位小写 MD5 值
     */
    public static String md5(File file) {
        if (file == null || !file.isFile()) return null;

        try (FileInputStream in = new FileInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int length;

            while ((length = in.read(buffer)) != -1) {
                md.update(buffer, 0, length);
            }
            return bytesToHex(md.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to calculate file MD5", e);
        }
    }

    /**
     * 将字节数组转换为十六进制字符串
     *
     * @param bytes 字节数组
     * @return 小写十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 根据分隔符翻转字符串
     */
    public static String reverseBySeparatorElegant(String str, String separator) {
        if (str == null) {
            return null;
        }
        if (isNullOrEmpty(separator)) {
            return new StringBuilder(str).reverse().toString();
        }

        int sepLen = separator.length();
        List<String> parts = new ArrayList<>();
        int from = 0;
        int idx;
        while ((idx = str.indexOf(separator, from)) >= 0) {
            parts.add(str.substring(from, idx));
            from = idx + sepLen;
        }
        parts.add(str.substring(from));

        Collections.reverse(parts);
        return String.join(separator, parts);
    }

    /**
     * 根据分隔符翻转字符串
     */
    public static String reverseBySeparator(String str) {
        return reverseBySeparatorElegant(str, "");
    }

    /**
     * 按正则表达式分割字符串
     *
     * @param text    待分割的文本
     * @param pattern 正则表达式
     * @return 分割后的字符串列表
     */
    public static List<String> splitStrings(String text, Pattern pattern) {
        List<String> segments = new ArrayList<>();
        int lastIndex = 0;
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            // 添加分隔符前的文本段
            if (matcher.start() > lastIndex) {
                String segment = text.substring(lastIndex, matcher.start());
                if (!segment.isEmpty()) {
                    segments.add(segment);
                }
            }
            // 添加分隔符本身
            segments.add(matcher.group());
            lastIndex = matcher.end();
        }
        // 添加最后一段文本
        if (lastIndex < text.length()) {
            String segment = text.substring(lastIndex);
            if (!segment.isEmpty()) {
                segments.add(segment);
            }
        }
        return segments;
    }

    /**
     * string是否匹配输入
     */
    public static boolean matches(String string, String input) {
        if (StringUtils.isNullOrEmpty(input)) return true;
        return string != null && (string.equals(input) || string.contains(input));
    }

    /**
     * 匹配度：0=精确 1=前缀 2=包含，数值越小匹配度越高
     */
    public static int matchDegree(String string, String input) {
        if (string == null || input == null) return 2;
        if (string.equals(input)) return 0;
        if (string.startsWith(input)) return 1;
        return 2;
    }

}
