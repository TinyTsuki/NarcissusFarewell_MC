package xin.vanilla.narcissus.util;

import lombok.NonNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.BuildConfig;
import xin.vanilla.narcissus.enums.EI18nType;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class I18nUtils {
    private static final Map<String, Map<String, String>> LANGUAGES = new HashMap<>();
    private static final String DEFAULT_LANGUAGE = "en_us";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String LANG_PATH = String.format("/assets/%s/lang/", BuildConfig.MODID);
    private static final String LANG_FILE_PATH = String.format("%s%%s.lang", LANG_PATH);
    private static final int[][] RULES = {
            {3, 4},
            {3},
            {0, 1},
            {0},
            {0, 3},
            {0, 1, 3, 4},
            {0, 1, 3},
    };

    static {
        loadLanguage(DEFAULT_LANGUAGE);
        getI18nFiles().forEach(I18nUtils::loadLanguage);
    }

    /**
     * 将字符串中指定位置的字符转为大写
     *
     * @param s   输入字符串
     * @param arr 需要转换的字符位置索引数组（基于0）
     * @return 转换后的字符串，若输入非法则返回原字符串
     */
    public static String toUpcase(String s, int[] arr) {
        // 处理空输入
        if (s == null || s.isEmpty() || arr == null || arr.length == 0) {
            return s;
        }

        char[] chars = s.toCharArray();
        for (int index : arr) {
            // 检查索引是否合法（0 <= index < length）
            if (index >= 0 && index < chars.length) {
                chars[index] = Character.toUpperCase(chars[index]);
            }
        }
        return new String(chars);
    }

    /**
     * 查找可用的语言文件名
     */
    private static String findAvailableFileName(String baseName) {
        // 尝试原始文件名
        if (resourceExists(baseName)) {
            return baseName;
        }
        // 尝试应用规则后的文件名
        for (int[] indexes : RULES) {
            String modifiedName = toUpcase(baseName, indexes);
            if (resourceExists(modifiedName)) {
                return modifiedName;
            }
        }
        return null;
    }

    /**
     * 检查资源是否存在
     */
    private static boolean resourceExists(String fileName) {
        return I18nUtils.class.getResource(String.format(LANG_FILE_PATH, fileName)) != null;
    }

    /**
     * 加载语言文件
     */
    public static void loadLanguage(@NonNull String languageCode) {
        languageCode = languageCode.toLowerCase(Locale.ROOT);
        LANGUAGES.computeIfAbsent(languageCode, code -> {
            Map<String, String> language = new HashMap<>();
            String fileName = findAvailableFileName(code);
            if (fileName == null) {
                LOGGER.warn("Language file not found for: {}", code);
                return language;
            }

            try (InputStream inputStream = I18nUtils.class.getResourceAsStream(String.format(LANG_FILE_PATH, fileName));
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                reader.lines()
                        .filter(StringUtils::isNotNullOrEmpty)
                        .map(line -> line.split("=", 2))
                        .filter(parts -> parts.length == 2)
                        .forEach(parts -> language.put(parts[0], StringUtils.replaceLine(parts[1])));
            } catch (Exception e) {
                LOGGER.error("Failed to load language file: {}", fileName, e);
            }
            return language;
        });
    }

    /**
     * 获取翻译文本
     */
    public static String getTranslationClient(@NonNull EI18nType type, @NonNull String key) {
        return getTranslation(getKey(type, key), NarcissusUtils.getClientLanguage());
    }

    /**
     * 获取翻译文本
     */
    public static String getTranslation(@NonNull EI18nType type, @NonNull String key, @NonNull String languageCode) {
        return getTranslation(getKey(type, key), languageCode);
    }

    /**
     * 获取翻译文本
     */
    public static String getTranslation(@NonNull String key, @NonNull String languageCode) {
        languageCode = languageCode.toLowerCase(Locale.ROOT);
        Map<String, String> language = LANGUAGES.getOrDefault(languageCode, LANGUAGES.get(DEFAULT_LANGUAGE));
        if (language != null && language.containsKey(key)) {
            return language.get(key);
        }
        return key;
    }

    public static String getKey(@NonNull EI18nType type, @NonNull String key) {
        String result;
        if (type == EI18nType.PLAIN || type == EI18nType.NONE) {
            result = key;
        } else {
            result = String.format("%s.%s.%s", type.name().toLowerCase(), BuildConfig.MODID, key);
        }
        return result;
    }

    public static Component enabled(@NonNull String languageCode, boolean enabled) {
        return Component.translatable(languageCode, EI18nType.WORD, enabled ? "enabled" : "disabled");
    }

    public static Component enabled(boolean enabled) {
        return Component.translatable(EI18nType.WORD, enabled ? "enabled" : "disabled");
    }

    /**
     * 获取I18n文件列表
     */
    public static List<String> getI18nFiles() {
        List<String> result = new ArrayList<>();
        try (InputStreamReader reader = new InputStreamReader(
                Objects.requireNonNull(I18nUtils.class.getResourceAsStream(LANG_PATH + "0_i18n_files.txt")),
                StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                // 将每一行添加到列表中
                if (StringUtils.isNotNullOrEmpty(line))
                    result.add(line);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to get I18n file name list", e);
        }
        return result;
    }
}
