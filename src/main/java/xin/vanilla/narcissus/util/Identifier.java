package xin.vanilla.narcissus.util;


import xin.vanilla.narcissus.NarcissusFarewell;

/**
 * 资源创建器接口
 */
public final class Identifier {

    public static String modId() {
        return NarcissusFarewell.MODID;
    }

    public static net.minecraft.resources.Identifier empty() {
        return create("", "");
    }

    public static net.minecraft.resources.Identifier create(String path) {
        return create(modId(), path);
    }

    public static net.minecraft.resources.Identifier create(String namespace, String path) {
        return net.minecraft.resources.Identifier.fromNamespaceAndPath(namespace, path);
    }

    public static net.minecraft.resources.Identifier parse(String location) {
        return net.minecraft.resources.Identifier.tryParse(location);
    }
}
