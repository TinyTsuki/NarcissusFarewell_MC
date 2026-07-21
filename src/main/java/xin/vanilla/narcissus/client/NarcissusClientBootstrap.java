package xin.vanilla.narcissus.client;

import net.minecraft.resources.ResourceLocation;
import xin.vanilla.banira.client.gui.ConfigEditorScreen;
import xin.vanilla.banira.client.gui.quickaction.QuickActionContext;
import xin.vanilla.banira.client.gui.quickaction.QuickActionContextMenuItem;
import xin.vanilla.banira.client.gui.quickaction.QuickActionRegistry;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.narcissus.Identifier;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.ClientConfig;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.event.ClientModEventHandler;
import xin.vanilla.narcissus.integration.ScreenHelper;

import java.util.function.Consumer;

/** 集中注册 Narcissus 的客户端事件、快捷入口和开发期 runner。 */
public final class NarcissusClientBootstrap {
    private static boolean initialized;

    private NarcissusClientBootstrap() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        ClientModEventHandler.register();
        registerQuickAction();
    }

    private static void registerQuickAction() {
        ResourceLocation texture = Identifier.id().create("gui/quick_icon.png");
        Component label = NarcissusComponent.get().transClient("key.narcissus_farewell.categories");
        Consumer<QuickActionContext> action = ctx -> ScreenHelper.openScreen();
        QuickActionContextMenuItem editClientConfig = new QuickActionContextMenuItem(
                NarcissusComponent.get().transClientAuto("edit_client_config"),
                ctx -> ConfigEditorScreen.open(ClientConfig.get().holder(), ctx.currentScreen()));
        QuickActionContextMenuItem editCommonConfig = new QuickActionContextMenuItem(
                NarcissusComponent.get().transClientAuto("edit_common_config"),
                ctx -> ConfigEditorScreen.open(CommonConfig.get().holder(), ctx.currentScreen()));
        QuickActionContextMenuItem editPlayerConfig = new QuickActionContextMenuItem(
                NarcissusComponent.get().transClientAuto("edit_player_config"),
                ctx -> ScreenHelper.openPlayerTeleportPrefsScreen());
        QuickActionContextMenuItem editAccessConfig = new QuickActionContextMenuItem(
                NarcissusComponent.get().transClientAuto("edit_access_config"),
                ctx -> ScreenHelper.openAccessListScreen());
        QuickActionRegistry.get().registerIcon(NarcissusFarewell.MODID + ":quick", texture, label, action,
                editAccessConfig, editPlayerConfig, editClientConfig, editCommonConfig);
    }
}
