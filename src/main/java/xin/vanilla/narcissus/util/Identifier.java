package xin.vanilla.narcissus.util;


import net.minecraft.util.ResourceLocation;
import xin.vanilla.narcissus.NarcissusFarewell;

/**
 * 资源创建器接口
 */
public final class Identifier {

    public static String modId() {
        return NarcissusFarewell.MODID;
    }

    public static ResourceLocation empty() {
        return create("", "");
    }

    public static ResourceLocation create(String path) {
        return create(modId(), path);
    }

    public static ResourceLocation create(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }

    public static ResourceLocation parse(String location) {
        return ResourceLocation.tryParse(location);
    }
}
