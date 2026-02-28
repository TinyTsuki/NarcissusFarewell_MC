package xin.vanilla.narcissus.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
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

    public static ResourceKey<Level> parse(String dimension) {
        return ResourceKey.create(Registries.DIMENSION, Identifier.parse(dimension));
    }

    public static ResourceKey<Level> parse(net.minecraft.resources.Identifier dimension) {
        return ResourceKey.create(Registries.DIMENSION, dimension);
    }

    public static ServerLevel getLevel(ResourceKey<Level> dimension) {
        MinecraftServer server = NarcissusFarewell.getServerInstance();
        return server != null ? server.getLevel(dimension) : null;
    }

    public static ServerLevel getLevel(net.minecraft.resources.Identifier dimension) {
        return getLevel(parse(dimension));
    }

    public static ServerLevel getLevel(String dimension) {
        return getLevel(parse(dimension));
    }

    public static Set<String> getAllIds() {
        MinecraftServer server = NarcissusFarewell.getServerInstance();
        if (server == null) return Collections.emptySet();
        Set<String> ids = new HashSet<>();
        server.levelKeys().forEach(key -> ids.add(key.identifier().toString()));
        return ids;
    }

}
