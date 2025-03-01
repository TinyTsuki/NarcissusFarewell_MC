package xin.vanilla.narcissus.util;

import net.neoforged.fml.loading.moddiscovery.ModInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

public class LogoModifier {

    private static final Logger LOGGER = LogManager.getLogger();

    private static String FIELD_NAME = null;

    public static void modifyLogo(IModInfo modInfo) {
        try {
            if (StringUtils.isNullOrEmpty(FIELD_NAME)) {
                for (String name : FieldUtils.getPrivateFieldNames(ModInfo.class, Optional.class)) {
                    try {
                        Optional<String> logo = ((Optional<String>) FieldUtils.getPrivateFieldValue(ModInfo.class, modInfo, name));
                        if (logo.isPresent() && StringUtils.isNotNullOrEmpty(logo.get()) && logo.get().matches(".*logo.*.png$")) {
                            FIELD_NAME = name;
                            break;
                        }
                    } catch (Exception ignored) {
                    }
                }
                if (StringUtils.isNullOrEmpty(FIELD_NAME)) {
                    FIELD_NAME = "logoFile";
                }
            }
            // 替换 logoFile
            FieldUtils.setPrivateFieldValue(ModInfo.class, modInfo, FIELD_NAME, Optional.of(LogoModifier.getLogoName()));
            LOGGER.debug("Modify logo to {}", modInfo.getLogoFile().get());
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    public static String getLogoName() {
        return Math.random() > 0.5 ? "logo_.png" : "logo.png";
    }
}
