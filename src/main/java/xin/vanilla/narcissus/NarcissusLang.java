package xin.vanilla.narcissus;

import lombok.NonNull;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.ScopedComponent;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.common.util.Translator;
import xin.vanilla.narcissus.config.CommonConfig;

/**
 * 水仙辞语言入口
 */
public final class NarcissusLang extends Translator {

    public static final NarcissusLang INSTANCE = new NarcissusLang();

    private NarcissusLang() {
        super(NarcissusFarewell.class);
        registerInCache();
    }

    public static NarcissusLang get() {
        return INSTANCE;
    }

    public static boolean hasTranslation(@NonNull EnumI18nType type, @NonNull String key) {
        return INSTANCE.hasTranslation(type, key, Translator.getClientLanguage());
    }

    public static String getClientLanguage() {
        return Translator.getClientLanguage();
    }

    public static String getServerLanguage() {
        return CommonConfig.get().general().defaultLanguage();
    }

    public static String getServerPlayerLanguage(ServerPlayer player) {
        return Translator.getServerPlayerLanguage(player);
    }

    public static Component transLangAuto(String languageCode, String key, Object... args) {
        return transLangAuto(NarcissusFarewell.MODID, languageCode, key, args);
    }

    public static Component transLangAuto(String modId, String languageCode, String key, Object... args) {
        if (args == null || args.length == 0) {
            return new ScopedComponent(modId).transLang(modId, languageCode, EnumI18nType.WORD, key);
        }
        return new ScopedComponent(modId).transLang(modId, languageCode, EnumI18nType.FORMAT, key, args);
    }
}
