package xin.vanilla.narcissus.util;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.server.ServerWorld;
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

    public static Structure<?> getStructure(String id) {
        return getStructure(Identifier.parse(id));
    }

    public static Structure<?> getStructure(ResourceLocation id) {
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
    public static Coordinate findNearestStructure(ServerWorld world, Coordinate start, Structure<?> structure, int radius) {
        if (world == null || start == null || structure == null) return null;
        BlockPos pos = world.findNearestMapFeature(structure, start.toBlockPos(), radius, true);
        if (pos != null) {
            return start.clone().x(pos.getX()).z(pos.getZ()).safe(true);
        }
        return null;
    }

}
