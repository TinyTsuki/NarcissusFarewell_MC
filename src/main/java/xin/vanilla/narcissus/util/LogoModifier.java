package xin.vanilla.narcissus.util;

import cpw.mods.fml.common.ModContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LogoModifier {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void modifyLogo(ModContainer modInfo) {
        try {
            modInfo.getMetadata().logoFile = LogoModifier.getLogoName();
            LOGGER.debug("Modify logo to {}", modInfo.getMetadata().logoFile);
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    public static String getLogoName() {
        return Math.random() > 0.5 ? "logo_.png" : "logo.png";
    }
}
