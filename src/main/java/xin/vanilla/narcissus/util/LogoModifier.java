package xin.vanilla.narcissus.util;

import net.minecraftforge.fml.common.ModContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LogoModifier {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void modifyLogo(ModContainer modInfo) {
        try {
            boolean b = Math.random() > 0.5;
            modInfo.getMetadata().logoFile = b ? "logo_.png" : "logo.png";
            LOGGER.debug(b ? "logo_" : "logo");
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }
}
