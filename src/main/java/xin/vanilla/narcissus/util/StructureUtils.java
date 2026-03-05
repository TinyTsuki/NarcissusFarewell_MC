package xin.vanilla.narcissus.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraftforge.registries.ForgeRegistries;
import xin.vanilla.narcissus.data.Coordinate;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 结构相关工具类
 */
public final class StructureUtils {
    private StructureUtils() {
    }

    public static StructureFeature<?> getStructure(String id) {
        return getStructure(Identifier.parse(id));
    }

    public static StructureFeature<?> getStructure(ResourceLocation id) {
        return id != null ? ForgeRegistries.STRUCTURE_FEATURES.getValue(id) : null;
    }

    public static Set<String> getAllIds() {
        return ForgeRegistries.STRUCTURE_FEATURES.getKeys().stream()
                .map(ResourceLocation::toString)
                .collect(Collectors.toSet());
    }

    /**
     * 在指定范围内查找最近的结构位置
     */
    public static Coordinate findNearestStructure(ServerLevel world, Coordinate start, StructureFeature<?> structure, int radius) {
        if (world == null || start == null || structure == null) return null;
        BlockPos pos = world.findNearestMapFeature(structure, start.toBlockPos(), radius, true);
        if (pos != null) {
            return start.clone().x(pos.getX()).z(pos.getZ()).safe(true);
        }
        return null;
    }

}
