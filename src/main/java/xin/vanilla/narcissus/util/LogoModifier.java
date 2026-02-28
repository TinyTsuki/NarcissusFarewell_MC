package xin.vanilla.narcissus.util;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public final class LogoModifier {
    private LogoModifier() {
    }

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * modId -> Supplier
     */
    private static final Map<String, Supplier<String>> SUPPLIER_REGISTRY = new ConcurrentHashMap<>();

    /**
     * Function列表, 按注册顺序执行
     */
    private static final List<Function<String, String>> FUNCTION_REGISTRY = new ArrayList<>();
    private static String FIELD_NAME = null;


    /**
     * 注册Logo提供者
     *
     * @param logoFileSupplier Logo文件路径提供者
     */
    public static void register(String modId, Supplier<String> logoFileSupplier) {
        if (modId == null || logoFileSupplier == null) {
            throw new IllegalArgumentException("modId and logoFileSupplier cannot be null");
        }
        SUPPLIER_REGISTRY.put(modId, logoFileSupplier);
    }

    /**
     * 注册Logo提供者
     *
     * @param logoFileFunction Logo文件路径函数, 接收 modId, 返回 logoFile
     */
    public static void register(Function<String, String> logoFileFunction) {
        if (logoFileFunction == null) {
            throw new IllegalArgumentException("logoFileFunction cannot be null");
        }
        FUNCTION_REGISTRY.add(logoFileFunction);
    }

    /**
     * 获取指定Mod的Logo文件路径
     *
     * @return Logo文件路径
     */
    public static Optional<String> getLogoFile(String modId) {
        if (StringUtils.isNullOrEmptyEx(modId)) {
            return Optional.empty();
        }

        Supplier<String> supplier = SUPPLIER_REGISTRY.get(modId);
        if (supplier != null) {
            String logoFile = supplier.get();
            if (StringUtils.isNotNullOrEmpty(logoFile)) {
                return Optional.of(logoFile);
            }
        }

        for (Function<String, String> function : FUNCTION_REGISTRY) {
            String logoFile = function.apply(modId);
            if (StringUtils.isNotNullOrEmpty(logoFile)) {
                return Optional.of(logoFile);
            }
        }

        return Optional.empty();
    }

    public static void modifyLogo() {
        if (SUPPLIER_REGISTRY.isEmpty() && FUNCTION_REGISTRY.isEmpty()) {
            return;
        }

        try {
            if (StringUtils.isNullOrEmpty(FIELD_NAME)) {
                List<? extends IModInfo> mods = ModList.get().getMods();
                if (mods.isEmpty()) {
                    return;
                }
                IModInfo sample = mods.get(0);
                for (String name : FieldUtils.getPrivateFieldNames(ModInfo.class, Optional.class)) {
                    try {
                        @SuppressWarnings("unchecked")
                        Optional<String> logo = (Optional<String>) FieldUtils.getPrivateFieldValue(ModInfo.class, sample, name);
                        if (logo != null && logo.isPresent()
                                && StringUtils.isNotNullOrEmpty(logo.get())
                                && logo.get().matches(".*\\.png$")) {
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

            for (IModInfo info : ModList.get().getMods()) {
                if (!(info instanceof ModInfo)) {
                    continue;
                }

                Optional<String> customLogo = getLogoFile(info.getModId());
                if (customLogo.isEmpty()) {
                    continue;
                }

                FieldUtils.setPrivateFieldValue(ModInfo.class, info, FIELD_NAME, customLogo);
                // LOGGER.debug("Modify logo of {} to {}", info.getModId(), customLogo.get());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to modify mod logos", e);
        }
    }

    /**
     * 清除所有注册
     */
    public static void clear() {
        SUPPLIER_REGISTRY.clear();
        FUNCTION_REGISTRY.clear();
    }
}
