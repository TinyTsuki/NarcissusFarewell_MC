package xin.vanilla.narcissus.util;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.registries.ForgeRegistries;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.data.Coordinate;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 生物群系相关工具类
 */
public final class BiomeUtils {
    private BiomeUtils() {
    }

    @SuppressWarnings("unchecked")
    private static Registry<Biome> getBiomeRegistry(RegistryAccess access) {
        return (Registry<Biome>) access.registries()
                .filter(e -> e.key().equals(Registries.BIOME))
                .findFirst()
                .map(RegistryAccess.RegistryEntry::value)
                .orElseThrow();
    }

    public static Biome getBiome(String id) {
        ResourceLocation loc = Identifier.parse(id);
        return loc != null ? getBiome(loc) : null;
    }

    public static Biome getBiome(ResourceLocation id) {
        if (id == null) return null;
        MinecraftServer server = NarcissusFarewell.getServerInstance();
        if (server != null) {
            return getBiomeRegistry(server.registryAccess()).getOptional(id).orElse(null);
        }
        return ForgeRegistries.BIOMES.getValue(id);
    }

    public static Biome getBiome(ServerLevel world, ResourceLocation id) {
        if (id == null) return null;
        if (world != null) {
            return getBiomeRegistry(world.registryAccess()).getOptional(id).orElse(null);
        }
        return getBiome(id);
    }

    public static ResourceKey<Biome> getKey(String id) {
        ResourceLocation loc = Identifier.parse(id);
        return loc != null ? getKey(loc) : null;
    }

    public static ResourceKey<Biome> getKey(ResourceLocation id) {
        return id != null ? ResourceKey.create(Registries.BIOME, id) : null;
    }

    public static Optional<ResourceKey<Biome>> getKey(Biome biome) {
        if (biome == null) return Optional.empty();
        MinecraftServer server = NarcissusFarewell.getServerInstance();
        if (server != null) {
            return getBiomeRegistry(server.registryAccess()).getResourceKey(biome);
        }
        return ForgeRegistries.BIOMES.getResourceKey(biome);
    }

    public static Optional<ResourceKey<Biome>> getKey(ServerLevel world, Biome biome) {
        if (biome == null || world == null) return Optional.empty();
        return getBiomeRegistry(world.registryAccess()).getResourceKey(biome);
    }

    @SuppressWarnings("unchecked")
    public static Optional<Holder.Reference<Biome>> getHolder(ResourceLocation id) {
        if (id == null) return Optional.empty();
        MinecraftServer server = NarcissusFarewell.getServerInstance();
        if (server != null) {
            return (Optional<Holder.Reference<Biome>>) (Optional<?>) server.registryAccess().lookupOrThrow(Registries.BIOME).get(getKey(id));
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public static Optional<Holder.Reference<Biome>> getHolder(ServerLevel world, ResourceLocation id) {
        if (id == null || world == null) return Optional.empty();
        return (Optional<Holder.Reference<Biome>>) (Optional<?>) world.registryAccess().lookupOrThrow(Registries.BIOME).get(getKey(id));
    }

    public static ResourceLocation getResourceLocation(Biome biome) {
        return getKey(biome).map(ResourceKey::location).orElse(null);
    }

    public static boolean hasBiome(ResourceLocation id) {
        return getBiome(id) != null;
    }

    public static boolean hasBiome(String id) {
        return getBiome(id) != null;
    }

    public static Set<String> getAllIds() {
        MinecraftServer server = NarcissusFarewell.getServerInstance();
        if (server != null) {
            return getBiomeRegistry(server.registryAccess()).keySet().stream()
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
        var registry = getBiomeRegistry(world.registryAccess());
        var biomeKey = registry.getResourceKey(biome).orElse(null);
        if (biomeKey == null) return null;
        Pair<BlockPos, Holder<Biome>> nearestBiome = world.findClosestBiome3d(holder -> holder.is(biomeKey), start.toBlockPos(), radius, minDistance, 64);
        if (nearestBiome != null) {
            BlockPos pos = nearestBiome.getFirst();
            if (pos != null) {
                return start.clone().x(pos.getX()).z(pos.getZ()).safe(true);
            }
        }
        return null;
    }

}
