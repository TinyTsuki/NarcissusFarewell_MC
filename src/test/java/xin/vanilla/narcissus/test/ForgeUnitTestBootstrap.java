package xin.vanilla.narcissus.test;

/** Minimal Forge 52 loading context for real item and hover codecs in JUnit. */
public final class ForgeUnitTestBootstrap {
    private static boolean bootstrapped;

    private ForgeUnitTestBootstrap() { }

    public static synchronized void bootstrap() throws Exception {
        if (bootstrapped) return;
        var loading = net.minecraftforge.fml.loading.LoadingModList.of(
                java.util.List.of(), java.util.List.of(), null);
        loading.setBrokenFiles(java.util.List.of());
        setField(net.minecraftforge.fml.loading.FMLLoader.class, "loadingModList", loading);
        cpw.mods.modlauncher.api.IModuleLayerManager layers = layer -> java.util.Optional.of(ModuleLayer.boot());
        setField(net.minecraftforge.fml.loading.FMLLoader.class, "moduleLayerManager", layers);
        var mods = net.minecraftforge.fml.ModList.of(java.util.List.of(), java.util.List.of());
        var setLoadedMods = net.minecraftforge.fml.ModList.class.getDeclaredMethod("setLoadedMods", java.util.List.class);
        setLoadedMods.setAccessible(true);
        setLoadedMods.invoke(mods, java.util.List.of());
        net.minecraftforge.fml.ModLoader.get();
        setField(net.minecraftforge.fml.ModLoader.class, "loadingStateValid", true);
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        bootstrapped = true;
    }

    private static void setField(Class<?> type, String name, Object value) throws Exception {
        var field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }
}
