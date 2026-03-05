package xin.vanilla.narcissus.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.registries.ForgeRegistries;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.data.Coordinate;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 生物群系相关工具类
 */
public final class BiomeUtils {
    private BiomeUtils() {
    }

    public static Biome getBiome(String id) {
        ResourceLocation loc = Identifier.parse(id);
        return loc != null ? getBiome(loc) : null;
    }

    public static Biome getBiome(ResourceLocation id) {
        if (id == null) return null;
        MinecraftServer server = NarcissusFarewell.getServerInstance();
        if (server != null) {
            return server.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getOptional(id).orElse(null);
        }
        return ForgeRegistries.BIOMES.getValue(id);
    }

    public static Biome getBiome(ServerLevel world, ResourceLocation id) {
        if (id == null) return null;
        if (world != null) {
            return world.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getOptional(id).orElse(null);
        }
        return getBiome(id);
    }

    public static Set<String> getAllIds() {
        MinecraftServer server = NarcissusFarewell.getServerInstance();
        if (server != null) {
            return server.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).keySet().stream()
                    .map(ResourceLocation::toString)
                    .collect(Collectors.toSet());
        }
        return ForgeRegistries.BIOMES.getKeys().stream()
                .map(ResourceLocation::toString)
                .collect(Collectors.toSet());
    }

    /**
     * 在指定范围内查找最近的生物群系位置
     */
    public static Coordinate findNearestBiome(ServerLevel world, Coordinate start, Biome biome, int radius, int minDistance) {
        if (world == null || start == null || biome == null) return null;
        BlockPos pos = world.findNearestBiome(biome, start.toBlockPos(), radius, minDistance);
        if (pos != null) {
            return start.clone().x(pos.getX()).z(pos.getZ()).safe(true);
        }
        return null;
    }

}
