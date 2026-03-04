package xin.vanilla.narcissus.util;

import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import xin.vanilla.narcissus.NarcissusFarewell;

import java.util.HashSet;
import java.util.Set;

public final class DimensionUtils {
    private DimensionUtils() {
    }

    private static final Set<RegistryKey<World>> keySet = new HashSet<>();
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

    public static Set<RegistryKey<World>> keySet() {
        init();
        return keySet;
    }

    public static RegistryKey<World> parse(ResourceLocation dimension) {
        return RegistryKey.create(Registry.DIMENSION_REGISTRY, dimension);
    }

    public static RegistryKey<World> parse(String dimension) {
        return RegistryKey.create(Registry.DIMENSION_REGISTRY, NarcissusFarewell.parseResource(dimension));
    }

    public static ServerWorld getLevel(RegistryKey<World> dimension) {
        return NarcissusFarewell.getServerInstance().getLevel(dimension);
    }

    public static ServerWorld getLevel(ResourceLocation dimension) {
        return getLevel(RegistryKey.create(Registry.DIMENSION_REGISTRY, dimension));
    }

    public static ServerWorld getLevel(String dimension) {
        return getLevel(parse(dimension));
    }

}
