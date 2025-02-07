package xin.vanilla.narcissus.util;

import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.CodeSource;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DimensionUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<Integer, String> idToStringMap = new HashMap<>();
    private static final Map<String, Integer> stringToIdMap = new HashMap<>();
    private static final Map<Integer, DimensionType> idToTypeMap = new HashMap<>();

    static {
        // 初始化缓存
        for (int dimId : DimensionManager.getStaticDimensionIDs()) {
            DimensionType type = DimensionManager.getProviderType(dimId);
            if (type != null) {
                String modid = getModIdForDimension(type);
                String fullId = modid + ":" + type.getName().toLowerCase(Locale.ROOT).replace(" ", "_");

                idToStringMap.put(dimId, fullId);
                stringToIdMap.put(fullId, dimId);
                idToTypeMap.put(dimId, type);
            }
        }
    }

    /**
     * 通过ID获取ResourceID
     */
    public static String getStringIdFromInt(int dimensionID) {
        return idToStringMap.get(dimensionID);
    }

    /**
     * 通过ResourceID获取ID
     */
    public static Integer getIntIdFromString(String fullDimensionId) {
        return stringToIdMap.getOrDefault(fullDimensionId, null);
    }

    /**
     * 通过ID获取DimensionType
     */
    public static DimensionType getDimensionType(int dimensionId) {
        return idToTypeMap.get(dimensionId);
    }

    /**
     * 通过ResourceID获取DimensionType
     */
    public static DimensionType getDimensionType(String fullDimensionId) {
        Integer dimId = getIntIdFromString(fullDimensionId);
        if (dimId == null) return null;
        return getDimensionType(dimId);
    }

    /**
     * 通过ID获取维度的World
     */
    public static World getWorldByDimensionId(int dimensionID) {
        return DimensionManager.getWorld(dimensionID);
    }

    /**
     * 通过ResourceID获取维度的World
     */
    public static World getWorldByStringId(String fullDimensionId) {
        Integer dimId = getIntIdFromString(fullDimensionId);
        if (dimId == null) return null;
        return getWorldByDimensionId(dimId);
    }

    /**
     * 根据 DimensionType 获取对应的 modid。
     */
    public static String getModIdForDimension(DimensionType type) {
        // 原版维度直接返回
        if (type == DimensionType.OVERWORLD || type == DimensionType.NETHER || type == DimensionType.THE_END) {
            return "minecraft";
        }
        try {
            Class<? extends WorldProvider> providerClass = ObfuscationReflectionHelper.getPrivateValue(
                    DimensionType.class, type, getWorldProviderClassFieldName());

            // 获取类的代码来源
            CodeSource codeSource = providerClass.getProtectionDomain().getCodeSource();
            if (codeSource == null) {
                LOGGER.warn("No CodeSource found for {}", type.getName());
                return "minecraft";
            }

            // 3. 处理 URL 转换
            URL locationUrl = codeSource.getLocation();
            if (locationUrl == null) {
                LOGGER.warn("No location URL for {}", type.getName());
                return "minecraft";
            }

            // 4. 安全转换为 File（处理 JAR 和目录）
            File sourceFile = convertUrlToFile(locationUrl);
            if (sourceFile == null) {
                LOGGER.warn("Unsupported URL format: {}", locationUrl);
                return "minecraft";
            }

            // 5. 匹配 Mod 文件
            for (ModContainer mod : Loader.instance().getActiveModList()) {
                if (mod.getSource() != null && mod.getSource().equals(sourceFile)) {
                    return mod.getModId();
                }
            }

        } catch (Exception e) {
            LOGGER.error("Failed to get modid for {}", type.getName(), e);
        }

        return "minecraft";
    }

    /**
     * 将 URL 安全转换为 File（支持 JAR 和文件系统）
     */
    private static File convertUrlToFile(URL url) {
        try {
            String protocol = url.getProtocol();
            String path = URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8.name());

            // 处理 JAR 协议（格式：jar:file:/path/to/mod.jar!/）
            if ("jar".equals(protocol)) {
                int separatorIndex = path.indexOf('!');
                if (separatorIndex != -1) {
                    path = path.substring(0, separatorIndex);
                }
                if (path.startsWith("file:")) {
                    path = path.substring(5);
                }
                return new File(path);
            }

            // 处理文件协议
            if ("file".equals(protocol)) {
                return new File(url.toURI());
            }

            // 其他协议（如 jrt:/）
            return null;

        } catch (URISyntaxException | java.io.UnsupportedEncodingException e) {
            LOGGER.warn("Failed to convert URL to File: {}", url, e);
            return null;
        }
    }

    /**
     * 获取 DimensionType 中类型为 Class<? extends WorldProvider> 的字段名称
     */
    public static String getWorldProviderClassFieldName() {
        // 遍历 DimensionType 的所有字段
        for (Field field : DimensionType.class.getDeclaredFields()) {
            try {
                field.setAccessible(true);
                // 检查字段类型是否为 Class
                if (field.getType() == Class.class) {
                    Object value = field.get(DimensionType.OVERWORLD);

                    // 验证值是否为 WorldProvider 的子类
                    if (value instanceof Class<?>) {
                        Class<?> clazz = (Class<?>) value;
                        if (WorldProvider.class.isAssignableFrom(clazz)) {
                            return field.getName();
                        }
                    }
                }
            } catch (IllegalAccessException e) {
                LOGGER.error("Failed to get WorldProvider class field name", e);
            }
        }
        return "field_186077_g";
    }
}
