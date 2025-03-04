package xin.vanilla.narcissus.util;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import net.minecraft.world.biome.BiomeGenBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BiomeUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<String, BiomeGenBase> stringToType = new HashMap<>();
    private static final Map<BiomeGenBase, String> typeToString = new HashMap<>();

    static {
        for (BiomeGenBase biome : BiomeGenBase.getBiomeGenArray()) {
            if (biome != null && StringUtils.isNotNullOrEmpty(biome.biomeName)) {
                String modid = getModIdForBiome(biome).toLowerCase().replace("fml", "minecraft");
                String fullId = modid + ":" + biome.biomeName.toLowerCase().replace(" ", "_");
                stringToType.put(fullId, biome);
                typeToString.put(biome, fullId);
            }
        }
    }

    /**
     * 获取生物群系的ResourceID
     */
    public static String getStringId(BiomeGenBase biome) {
        if (biome == null) {
            return null;
        }
        return typeToString.get(biome);
    }

    /**
     * 获取所有生物群系的ResourceID
     */
    public static List<String> getStringIds() {
        return new ArrayList<>(typeToString.values());
    }

    /**
     * 获取生物群系
     */
    public static BiomeGenBase getBiome(String id) {
        return stringToType.get(id);
    }

    /**
     * 根据 BiomeGenBase 获取对应的 modid。
     */
    public static String getModIdForBiome(BiomeGenBase biome) {
        try {
            // 获取类的代码来源
            CodeSource codeSource = biome.getBiomeClass().getProtectionDomain().getCodeSource();
            if (codeSource == null) {
                LOGGER.warn("No CodeSource found for {}", biome.biomeName);
                return "minecraft";
            }

            // 3. 处理 URL 转换
            URL locationUrl = codeSource.getLocation();
            if (locationUrl == null) {
                LOGGER.warn("No location URL for {}", biome.biomeName);
                return "minecraft";
            }

            // 4. 安全转换为 File（处理 JAR 和目录）
            File sourceFile = DimensionUtils.convertUrlToFile(locationUrl);
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
            LOGGER.error("Failed to get modid for {}", biome.biomeName, e);
        }

        return "minecraft";
    }
}
