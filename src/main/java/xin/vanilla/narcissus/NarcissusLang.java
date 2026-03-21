package xin.vanilla.narcissus;

import lombok.NonNull;
import net.minecraft.entity.player.ServerPlayerEntity;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.common.util.Translator;
import xin.vanilla.narcissus.config.CommonConfig;

/**
 * 水仙辞语言入口
 */
public final class NarcissusLang extends Translator {

    public static final NarcissusLang INSTANCE = new NarcissusLang();

    private NarcissusLang() {
        super(NarcissusFarewell.MODID);
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
        return CommonConfig.get().server().general().defaultLanguage();
    }

    public static String getServerPlayerLanguage(ServerPlayerEntity player) {
        return Translator.getServerPlayerLanguage(player);
    }

    /**
     * 与 {@link Component#transAuto(String, String, Object...)} 一致：无参走 WORD，有参走 FORMAT。
     */
    public static Component transLangAuto(String languageCode, String key, Object... args) {
        return transLangAuto(NarcissusFarewell.MODID, languageCode, key, args);
    }

    public static Component transLangAuto(String modId, String languageCode, String key, Object... args) {
        if (args == null || args.length == 0) {
            return Component.transLang(modId, languageCode, EnumI18nType.WORD, key);
        }
        return Component.transLang(modId, languageCode, EnumI18nType.FORMAT, key, args);
    }
}
