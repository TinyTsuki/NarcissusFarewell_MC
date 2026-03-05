package xin.vanilla.narcissus.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import xin.vanilla.narcissus.NarcissusFarewell;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 维度相关工具类
 */
public final class DimensionUtils {
    private DimensionUtils() {
    }

    public static RegistryKey<World> parse(String dimension) {
        return RegistryKey.create(Registry.DIMENSION_REGISTRY, Identifier.parse(dimension));
    }

    public static RegistryKey<World> parse(ResourceLocation dimension) {
        return RegistryKey.create(Registry.DIMENSION_REGISTRY, dimension);
    }

    public static ServerWorld getLevel(RegistryKey<World> dimension) {
        MinecraftServer server = NarcissusFarewell.getServerInstance();
        return server != null ? server.getLevel(dimension) : null;
    }

    public static ServerWorld getLevel(ResourceLocation dimension) {
        return getLevel(parse(dimension));
    }

    public static ServerWorld getLevel(String dimension) {
        return getLevel(parse(dimension));
    }

    public static Set<String> getAllIds() {
        MinecraftServer server = NarcissusFarewell.getServerInstance();
        if (server == null) return Collections.emptySet();
        Set<String> ids = new HashSet<>();
        server.levelKeys().forEach(key -> ids.add(key.location().toString()));
        return ids;
    }

}
