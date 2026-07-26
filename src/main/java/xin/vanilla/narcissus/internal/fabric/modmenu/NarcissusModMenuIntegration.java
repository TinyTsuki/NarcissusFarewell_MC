package xin.vanilla.narcissus.internal.fabric.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import xin.vanilla.banira.client.gui.ConfigEditorScreen;
import xin.vanilla.narcissus.config.ClientConfig;

/**
 * 将 Mod Menu 的设置按钮连接到水仙辞客户端配置。
 */
public final class NarcissusModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new ConfigEditorScreen(
                ClientConfig.get().holder(),
                new ConfigEditorScreen.Args().parentScreen(parent));
    }
}
