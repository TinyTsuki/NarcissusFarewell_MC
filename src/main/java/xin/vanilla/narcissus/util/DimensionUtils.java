package xin.vanilla.narcissus.util;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import net.minecraft.world.*;
import net.minecraftforge.common.DimensionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.CodeSource;
import java.util.*;

public class DimensionUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<Integer, String> idToStringMap = new HashMap<>();
    private static final Map<String, Integer> stringToIdMap = new HashMap<>();
    private static final Map<WorldProvider, Integer> typeToIdMap = new HashMap<>();

    public static void init() {
        // 初始化缓存
        idToStringMap.clear();
        stringToIdMap.clear();
        typeToIdMap.clear();

        // 遍历所有维度
        for (int dimId : DimensionManager.getStaticDimensionIDs()) {
            WorldProvider type = DimensionManager.getProvider(dimId);
            if (type != null) {
                String modid = getModIdForDimension(type).toLowerCase().replace("fml", "minecraft");
                String fullId = modid + ":" + type.getDimensionName().toLowerCase(Locale.ROOT).replace(" ", "_");

                idToStringMap.put(dimId, fullId);
                stringToIdMap.put(fullId, dimId);
                typeToIdMap.put(type, dimId);
            }
        }
    }

    /**
     * 通过dimensionID获取ResourceID
     */
    public static String getStringId(int dimensionID) {
        return idToStringMap.getOrDefault(dimensionID, "minecraft:overworld");
    }

    /**
     * 通过dimensionID获取ResourceID
     */
    public static String getStringId(String fullDimensionId) {
        return stringToIdMap.containsKey(fullDimensionId) ? fullDimensionId : null;
    }

    /**
     * 获取主世界的dimensionID
     */
    public static Integer getOverworldDimensionId() {
        return typeToIdMap.entrySet().stream()
                .filter(entry -> entry.getKey() instanceof WorldProviderSurface)
                .map(Map.Entry::getValue)
                .findFirst().orElse(0);
    }

    public static String getOverworldDimensionStringId() {
        return getStringId(getOverworldDimensionId());
    }

    /**
     * 获取所有维度的ResourceID
     */
    public static List<String> getStringIds() {
        return new ArrayList<>(idToStringMap.values());
    }

    /**
     * 通过WorldProvider获取ResourceID
     */
    public static String getStringId(WorldProvider provider) {
        return idToStringMap.get(typeToIdMap.get(provider));
    }

    /**
     * 通过ResourceID获取dimensionID
     */
    public static Integer getIntIdFromString(String fullDimensionId) {
        return stringToIdMap.getOrDefault(fullDimensionId, null);
    }

    /**
     * 通过WorldProvider获取DimensionId
     */
    public static Integer getDimensionType(WorldProvider provider) {
        return typeToIdMap.get(provider);
    }

    /**
     * 通过ResourceID获取DimensionId
     */
    public static Integer getDimensionType(String fullDimensionId) {
        return stringToIdMap.get(fullDimensionId);
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
     * 根据 WorldProvider 获取对应的 modid。
     */
    public static String getModIdForDimension(WorldProvider type) {
        // 原版维度直接返回
        if (type instanceof WorldProviderSurface || type instanceof WorldProviderHell || type instanceof WorldProviderEnd) {
            return "minecraft";
        }
        try {
            // 获取类的代码来源
            CodeSource codeSource = type.getClass().getProtectionDomain().getCodeSource();
            if (codeSource == null) {
                LOGGER.warn("No CodeSource found for {}", type.getDimensionName());
                return "minecraft";
            }

            // 3. 处理 URL 转换
            URL locationUrl = codeSource.getLocation();
            if (locationUrl == null) {
                LOGGER.warn("No location URL for {}", type.getDimensionName());
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
            LOGGER.error("Failed to get modid for {}", type.getDimensionName(), e);
        }

        return "minecraft";
    }

    /**
     * 将 URL 安全转换为 File（支持 JAR 和文件系统）
     */
    public static File convertUrlToFile(URL url) {
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
}
