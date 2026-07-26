package xin.vanilla.narcissus.internal.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import xin.vanilla.narcissus.client.NarcissusClientBootstrap;

/**
 * Fabric 客户端入口，避免独立服务端类加载客户端类型。
 */
public final class FabricNarcissusClientEntry implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        NarcissusClientBootstrap.init();
    }
}
