package xin.vanilla.narcissus.internal.fabric;

import net.fabricmc.api.ModInitializer;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.internal.fabric.event.FabricNarcissusGameEventAdapter;

/**
 * Fabric 公共入口，只负责安装加载器适配并启动共享业务。
 */
public final class FabricNarcissusEntry implements ModInitializer {
    @Override
    public void onInitialize() {
        NarcissusFarewell.bootstrapCommon();
        FabricNarcissusGameEventAdapter.register();
    }
}
