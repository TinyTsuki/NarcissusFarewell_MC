package xin.vanilla.narcissus.util;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
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

    @SuppressWarnings("unchecked")
    private static Registry<Structure> getStructureRegistry(RegistryAccess access) {
        return (Registry<Structure>) access.registries()
                .filter(e -> e.key().equals(Registries.STRUCTURE))
                .findFirst()
                .map(RegistryAccess.RegistryEntry::value)
                .orElseThrow();
    }

    public static Structure getStructure(String id) {
        net.minecraft.resources.Identifier loc = Identifier.parse(id);
        return loc != null ? getStructure(loc) : null;
    }

    public static Structure getStructure(net.minecraft.resources.Identifier id) {
        if (id == null) return null;
        MinecraftServer server = NarcissusFarewell.getServerInstance();
        if (server != null) {
            return getStructureRegistry(server.registryAccess()).getOptional(id).orElse(null);
        }
        return null;
    }

    public static Structure getStructure(ServerLevel world, net.minecraft.resources.Identifier id) {
        if (id == null) return null;
        if (world != null) {
            return getStructureRegistry(world.registryAccess()).getOptional(id).orElse(null);
        }
        return getStructure(id);
    }

    public static ResourceKey<Structure> getKey(String id) {
        net.minecraft.resources.Identifier loc = Identifier.parse(id);
        return loc != null ? getKey(loc) : null;
    }

    public static ResourceKey<Structure> getKey(net.minecraft.resources.Identifier id) {
        return id != null ? ResourceKey.create(Registries.STRUCTURE, id) : null;
    }

    public static Optional<ResourceKey<Structure>> getKey(Structure structure) {
        if (structure == null) return Optional.empty();
        MinecraftServer server = NarcissusFarewell.getServerInstance();
        if (server != null) {
            return getStructureRegistry(server.registryAccess()).getResourceKey(structure);
        }
        return Optional.empty();
    }

    public static Optional<ResourceKey<Structure>> getKey(ServerLevel world, Structure structure) {
        if (structure == null || world == null) return Optional.empty();
        return getStructureRegistry(world.registryAccess()).getResourceKey(structure);
    }

    public static Optional<Holder.Reference<Structure>> getHolder(net.minecraft.resources.Identifier id) {
        if (id == null) return Optional.empty();
        MinecraftServer server = NarcissusFarewell.getServerInstance();
        if (server != null) {
            Registry<Structure> registry = getStructureRegistry(server.registryAccess());
            ResourceKey<Structure> key = getKey(id);
            if (key == null) return Optional.empty();
            return registry.get(key);
        }
        return Optional.empty();
    }

    public static Optional<Holder.Reference<Structure>> getHolder(ServerLevel world, net.minecraft.resources.Identifier id) {
        if (id == null || world == null) return Optional.empty();
        Registry<Structure> registry = getStructureRegistry(world.registryAccess());
        ResourceKey<Structure> key = getKey(id);
        if (key == null) return Optional.empty();
        return registry.get(key);
    }

    public static net.minecraft.resources.Identifier getResourceLocation(Structure structure) {
        return getKey(structure).map(ResourceKey::identifier).orElse(null);
    }

    public static TagKey<Structure> getStructureTag(net.minecraft.resources.Identifier id) {
        return TagKey.create(Registries.STRUCTURE, id);
    }

    public static TagKey<Structure> getStructureTag(String id) {
        return getStructureTag(Identifier.parse(id));
    }

    public static boolean hasStructure(net.minecraft.resources.Identifier id) {
        if (id == null) return false;
        MinecraftServer server = NarcissusFarewell.getServerInstance();
        if (server != null) {
            Registry<Structure> registry = getStructureRegistry(server.registryAccess());
            return registry.containsKey(id);
        }
        return false;
    }

    public static boolean hasStructure(String id) {
        return hasStructure(Identifier.parse(id));
    }

    public static Set<String> getAllIds() {
        MinecraftServer server = NarcissusFarewell.getServerInstance();
        if (server != null) {
            return getStructureRegistry(server.registryAccess()).keySet().stream()
                    .map(net.minecraft.resources.Identifier::toString)
                    .collect(Collectors.toSet());
        }
        return Set.of();
    }

    /**
     * 在指定范围内查找最近的结构位置
     */
    public static Coordinate findNearestStructure(ServerLevel world, Coordinate start, TagKey<Structure> structureTag, int radius) {
        if (world == null || start == null || structureTag == null) return null;
        HolderGetter<Structure> getter = world.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Optional<HolderSet.Named<Structure>> holderSetOpt = getter.get(structureTag);
        return holderSetOpt.map(holders ->
                findNearestMapFeatureImpl(world, start, holders, radius)
        ).orElse(null);
    }

    /**
     * 在指定范围内查找最近的结构位置
     */
    public static Coordinate findNearestStructure(ServerLevel world, Coordinate start, net.minecraft.resources.Identifier structureId, int radius) {
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
        net.minecraft.resources.Identifier loc = Identifier.parse(structureId);
        return loc != null ? findNearestStructure(world, start, loc, radius) : null;
    }

}
