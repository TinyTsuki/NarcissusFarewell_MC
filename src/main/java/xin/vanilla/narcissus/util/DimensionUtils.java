package xin.vanilla.narcissus.util;


import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import xin.vanilla.narcissus.NarcissusFarewell;

import java.util.HashSet;
import java.util.Set;

public final class DimensionUtils {
    private DimensionUtils() {
    }

    private static final Set<ResourceKey<Level>> keySet = new HashSet<>();
    private static final Set<ResourceLocation> locationSet = new HashSet<>();
    private static final Set<String> stringSet = new HashSet<>();

    private static void init() {
        if (keySet.isEmpty() || locationSet.isEmpty() || stringSet.isEmpty()) {
            NarcissusFarewell.getServerInstance().levelKeys().forEach(key -> {
                keySet.add(key);
                locationSet.add(key.location());
                stringSet.add(key.location().toString());
            });
        }
    }

    public static Set<String> stringSet() {
        init();
        return stringSet;
    }

    public static Set<ResourceLocation> locationSet() {
        init();
        return locationSet;
    }

    public static Set<ResourceKey<Level>> keySet() {
        init();
        return keySet;
    }

    public static ResourceKey<Level> parse(ResourceLocation dimension) {
        return ResourceKey.create(Registry.DIMENSION_REGISTRY, dimension);
    }

    public static ResourceKey<Level> parse(String dimension) {
        return ResourceKey.create(Registry.DIMENSION_REGISTRY, NarcissusFarewell.parseResource(dimension));
    }

    public static ServerLevel getLevel(ResourceKey<Level> dimension) {
        return NarcissusFarewell.getServerInstance().getLevel(dimension);
    }

    public static ServerLevel getLevel(ResourceLocation dimension) {
        return getLevel(ResourceKey.create(Registry.DIMENSION_REGISTRY, dimension));
    }

    public static ServerLevel getLevel(String dimension) {
        return getLevel(parse(dimension));
    }

}
