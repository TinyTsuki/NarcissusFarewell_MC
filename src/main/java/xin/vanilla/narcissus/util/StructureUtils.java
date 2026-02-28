package xin.vanilla.narcissus.util;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.data.Coordinate;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 结构相关工具类
 */
public final class StructureUtils {
    private StructureUtils() {
    }


    public static Structure getStructure(String id) {
        ResourceLocation loc = Identifier.parse(id);
        return loc != null ? getStructure(loc) : null;
    }

    public static Structure getStructure(ResourceLocation id) {
        if (id == null) return null;
        MinecraftServer server = NarcissusFarewell.getServerInstance();
        if (server != null) {
            return server.registryAccess().registryOrThrow(Registry.STRUCTURE_REGISTRY).getOptional(id).orElse(null);
        }
        return null;
    }

    public static Structure getStructure(ServerLevel world, ResourceLocation id) {
        if (id == null) return null;
        if (world != null) {
            return world.registryAccess().registryOrThrow(Registry.STRUCTURE_REGISTRY).getOptional(id).orElse(null);
        }
        return getStructure(id);
    }

    public static ResourceKey<Structure> getKey(String id) {
        ResourceLocation loc = Identifier.parse(id);
        return loc != null ? getKey(loc) : null;
    }

    public static ResourceKey<Structure> getKey(ResourceLocation id) {
        return id != null ? ResourceKey.create(Registry.STRUCTURE_REGISTRY, id) : null;
    }

    public static Optional<ResourceKey<Structure>> getKey(Structure structure) {
        if (structure == null) return Optional.empty();
        MinecraftServer server = NarcissusFarewell.getServerInstance();
        if (server != null) {
            return server.registryAccess().registryOrThrow(Registry.STRUCTURE_REGISTRY).getResourceKey(structure);
        }
        return Optional.empty();
    }

    public static Optional<ResourceKey<Structure>> getKey(ServerLevel world, Structure structure) {
        if (structure == null || world == null) return Optional.empty();
        return world.registryAccess().registryOrThrow(Registry.STRUCTURE_REGISTRY).getResourceKey(structure);
    }

    public static Optional<Holder<Structure>> getHolder(ResourceLocation id) {
        if (id == null) return Optional.empty();
        MinecraftServer server = NarcissusFarewell.getServerInstance();
        if (server != null) {
            return server.registryAccess().registryOrThrow(Registry.STRUCTURE_REGISTRY).getHolder(getKey(id));
        }
        return Optional.empty();
    }

    public static Optional<Holder<Structure>> getHolder(ServerLevel world, ResourceLocation id) {
        if (id == null || world == null) return Optional.empty();
        return world.registryAccess().registryOrThrow(Registry.STRUCTURE_REGISTRY).getHolder(getKey(id));
    }

    public static ResourceLocation getResourceLocation(Structure structure) {
        return getKey(structure).map(ResourceKey::location).orElse(null);
    }

    public static TagKey<Structure> getStructureTag(ResourceLocation id) {
        return TagKey.create(Registry.STRUCTURE_REGISTRY, id);
    }

    public static TagKey<Structure> getStructureTag(String id) {
        return getStructureTag(Identifier.parse(id));
    }

    public static boolean hasStructure(ResourceLocation id) {
        if (id == null) return false;
        MinecraftServer server = NarcissusFarewell.getServerInstance();
        if (server != null) {
            var registry = server.registryAccess().registryOrThrow(Registry.STRUCTURE_REGISTRY);
            if (registry.containsKey(id)) return true;
            try {
                return registry.getTag(getStructureTag(id)).isPresent();
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    public static boolean hasStructure(String id) {
        return hasStructure(Identifier.parse(id));
    }

    public static Set<String> getAllIds() {
        MinecraftServer server = NarcissusFarewell.getServerInstance();
        if (server != null) {
            return server.registryAccess().registryOrThrow(Registry.STRUCTURE_REGISTRY).keySet().stream()
                    .map(ResourceLocation::toString)
                    .collect(Collectors.toSet());
        }
        return Set.of();
    }

    /**
     * 在指定范围内查找最近的结构位置
     */
    public static Coordinate findNearestStructure(ServerLevel world, Coordinate start, TagKey<Structure> structureTag, int radius) {
        if (world == null || start == null || structureTag == null) return null;
        var registry = world.registryAccess().registryOrThrow(Registry.STRUCTURE_REGISTRY);
        var holderSetOpt = registry.getTag(structureTag);
        return holderSetOpt.map(holders ->
                findNearestMapFeatureImpl(world, start, holders, radius)
        ).orElse(null);
    }

    /**
     * 在指定范围内查找最近的结构位置
     */
    public static Coordinate findNearestStructure(ServerLevel world, Coordinate start, ResourceLocation structureId, int radius) {
        if (world == null || start == null || structureId == null) return null;
        var holderOpt = getHolder(world, structureId);
        return holderOpt.map(configuredStructureFeatureHolder ->
                findNearestMapFeatureImpl(world, start, HolderSet.direct(configuredStructureFeatureHolder), radius)
        ).orElse(null);
    }

    private static Coordinate findNearestMapFeatureImpl(ServerLevel world, Coordinate start, HolderSet<Structure> holderSet, int radius) {
        Pair<BlockPos, Holder<Structure>> pair = world.getChunkSource().getGenerator()
                .findNearestMapStructure(world, holderSet, start.toBlockPos(), radius, true);
        if (pair != null) {
            BlockPos pos = pair.getFirst();
            if (pos != null) {
                return start.clone().x(pos.getX()).z(pos.getZ()).safe(true);
            }
        }
        return null;
    }

    public static Coordinate findNearestStructure(ServerLevel world, Coordinate start, String structureId, int radius) {
        ResourceLocation loc = Identifier.parse(structureId);
        return loc != null ? findNearestStructure(world, start, loc, radius) : null;
    }

}
