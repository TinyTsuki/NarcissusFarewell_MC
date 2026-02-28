package xin.vanilla.narcissus.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.NonNull;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.enums.EnumI18nType;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class I18nUtils {
    private static final Map<String, JsonObject> LANGUAGES = new HashMap<>();
    private static final String DEFAULT_LANGUAGE = "en_us";
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String LANG_PATH = String.format("/assets/%s/lang/", NarcissusFarewell.MODID);
    private static final String LANG_FILE_PATH = String.format("%s%%s.json", LANG_PATH);

    static {
        loadLanguage(DEFAULT_LANGUAGE);
        getI18nFiles().forEach(I18nUtils::loadLanguage);
    }

    /**
     * 加载语言文件
     */
    public static void loadLanguage(@NonNull String languageCode) {
        languageCode = languageCode.toLowerCase(Locale.ROOT);
        if (!LANGUAGES.containsKey(languageCode)) {
            try {
                try (InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(I18nUtils.class.getResourceAsStream(String.format(LANG_FILE_PATH, languageCode))), StandardCharsets.UTF_8)) {
                    JsonObject jsonObject = GSON.fromJson(reader, JsonObject.class);
                    LANGUAGES.put(languageCode, jsonObject);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load language file: {}", languageCode, e);
            }
        }
    }

    public static boolean hasTranslation(@NonNull EnumI18nType type, @NonNull String key) {
        String languageCode = NarcissusUtils.getClientLanguage();
        JsonObject language = LANGUAGES.getOrDefault(languageCode, LANGUAGES.get(DEFAULT_LANGUAGE));
        return language != null && language.has(getKey(type, key));
    }

    /**
     * 获取翻译文本
     */
    public static String getTranslationClient(@NonNull EnumI18nType type, @NonNull String key) {
        return getTranslation(getKey(type, key), NarcissusUtils.getClientLanguage());
    }

    /**
     * 获取翻译文本
     */
    public static String getTranslation(@NonNull EnumI18nType type, @NonNull String key, @NonNull String languageCode) {
        return getTranslation(getKey(type, key), languageCode);
    }

    /**
     * 获取翻译文本
     */
    public static String getTranslation(@NonNull String key, @NonNull String languageCode) {
        languageCode = languageCode.toLowerCase(Locale.ROOT);
        JsonObject language = LANGUAGES.getOrDefault(languageCode, LANGUAGES.get(DEFAULT_LANGUAGE));
        if (language != null && language.has(key)) {
            return language.get(key).getAsString();
        }
        return key;
    }

    public static String getKey(@NonNull EnumI18nType type, @NonNull String key) {
        String result;
        if (type == EnumI18nType.PLAIN || type == EnumI18nType.NONE) {
            result = key;
        } else {
            result = String.format("%s.%s.%s", type.name().toLowerCase(), NarcissusFarewell.MODID, key);
        }
        return result;
    }

    public static Component enabled(@NonNull String languageCode, boolean enabled) {
        return Component.trans(languageCode, EnumI18nType.WORD, enabled ? "enabled" : "disabled");
    }

    public static Component enabled(boolean enabled) {
        return Component.trans(EnumI18nType.WORD, enabled ? "enabled" : "disabled");
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


    public static String getClientLanguage() {
        return Minecraft.getInstance().getLanguageManager().getSelected().getCode();
    }

    public static String getServerLanguage() {
        return ServerConfig.DEFAULT_LANGUAGE.get();
    }

    public static String getServerPlayerLanguage(ServerPlayerEntity player) {
        return player.getLanguage();
    }

    public static String getValidLanguage(@Nullable PlayerEntity player, @Nullable String language) {
        String result;
        if (StringUtils.isNullOrEmptyEx(language) || "client".equalsIgnoreCase(language)) {
            if (player instanceof ServerPlayerEntity) {
                result = I18nUtils.getServerPlayerLanguage((ServerPlayerEntity) player);
            } else {
                result = I18nUtils.getClientLanguage();
            }
        } else if ("server".equalsIgnoreCase(language)) {
            return ServerConfig.DEFAULT_LANGUAGE.get();
        } else {
            result = language;
        }
        return result;
    }

    public static String getPlayerLanguage(@NonNull PlayerEntity player) {
        try {
            String language = ServerConfig.DEFAULT_LANGUAGE.get();
            return I18nUtils.getValidLanguage(player, language);
        } catch (IllegalArgumentException i) {
            return ServerConfig.DEFAULT_LANGUAGE.get();
        }
    }
}
