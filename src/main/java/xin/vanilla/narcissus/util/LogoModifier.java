package xin.vanilla.narcissus.util;

import net.neoforged.fml.loading.moddiscovery.ModInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.Optional;

public class LogoModifier {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void modifyLogo(IModInfo modInfo) {
        try {
            Field field = ModInfo.class.getDeclaredField("logoFile");
            field.setAccessible(true);
            // 解除 final 修饰符
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
            // 替换 logoFile
            boolean b = Math.random() > 0.5;
            Optional<String> newLogo = b ? Optional.of("logo_.png") : Optional.of("logo.png");
            field.set(modInfo, newLogo);
            LOGGER.debug(b ? "logo_" : "logo");
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }
}
