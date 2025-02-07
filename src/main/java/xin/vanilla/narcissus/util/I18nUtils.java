package xin.vanilla.narcissus.util;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.BuildConfig;
import xin.vanilla.narcissus.enums.EI18nType;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class I18nUtils {
    private static final Map<String, Map<String, String>> LANGUAGES = new HashMap<>();
    private static final String DEFAULT_LANGUAGE = "en_us";
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String LANG_PATH = String.format("/assets/%s/lang/", BuildConfig.MODID);
    private static final String LANG_FILE_PATH = String.format("%s%%s.lang", LANG_PATH);

    static {
        loadLanguage(DEFAULT_LANGUAGE);
        getI18nFiles().forEach(I18nUtils::loadLanguage);
    }

    /**
     * 加载语言文件
     */
    public static void loadLanguage(String languageCode) {
        if (!LANGUAGES.containsKey(languageCode)) {
            try {
                try (InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(I18nUtils.class.getResourceAsStream(String.format(LANG_FILE_PATH, languageCode))), StandardCharsets.UTF_8)) {
                    Map<String, String> language = new HashMap<>();
                    BufferedReader bufferedReader = new BufferedReader(reader);
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        if (StringUtils.isNotNullOrEmpty(line)) {
                            String[] keyValue = line.split("=", 2);
                            if (keyValue.length == 2) {
                                language.put(keyValue[0], keyValue[1]);
                            }
                        }
                    }
                    LANGUAGES.put(languageCode, language);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load language file: {}", languageCode, e);
            }
        }
    }

    /**
     * 获取翻译文本
     */
    public static String getTranslation(String key, String languageCode) {
        Map<String, String> language = LANGUAGES.getOrDefault(languageCode, LANGUAGES.get(DEFAULT_LANGUAGE));
        if (language != null && language.containsKey(key)) {
            return language.get(key);
        }
        return key;
    }

    public static String getKey(EI18nType type, String key) {
        String result;
        if (type == EI18nType.PLAIN || type == EI18nType.NONE) {
            result = key;
        } else {
            result = String.format("%s.%s.%s", type.name().toLowerCase(), BuildConfig.MODID, key);
        }
        return result;
    }

    public static Component enabled(String languageCode, boolean enabled) {
        return Component.translatable(languageCode, EI18nType.WORD, enabled ? "enabled" : "disabled");
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
