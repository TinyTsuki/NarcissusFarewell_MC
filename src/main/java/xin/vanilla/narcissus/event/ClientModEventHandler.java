package xin.vanilla.narcissus.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.event.BaniraClientEventHub;
import xin.vanilla.banira.client.notification.NotificationTypeRegistry;
import xin.vanilla.banira.client.util.BaniraKeyBindings;
import xin.vanilla.banira.client.util.LogoModifier;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.integration.ScreenHelper;
import xin.vanilla.narcissus.network.NetworkInit;
import xin.vanilla.narcissus.network.packet.*;
import xin.vanilla.narcissus.notification.NarcissusNotificationTypes;

/**
 * 客户端：Banira 键位入队 + {@link BaniraClientEventHub} 回调注册（不在此类上使用 Forge {@code @SubscribeEvent}）
 */
@OnlyIn(Dist.CLIENT)
public final class ClientModEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    private static boolean keyDown;

    public static final KeyBinding TP_HOME_KEY = BaniraKeyBindings.register(NarcissusFarewell.MODID, "tp_home", GLFWKey.GLFW_KEY_UNKNOWN);
    public static final KeyBinding TP_BACK_KEY = BaniraKeyBindings.register(NarcissusFarewell.MODID, "tp_back", GLFWKey.GLFW_KEY_UNKNOWN);
    public static final KeyBinding TP_REQ_YES = BaniraKeyBindings.register(NarcissusFarewell.MODID, "tp_req_yes", GLFWKey.GLFW_KEY_UNKNOWN);
    public static final KeyBinding TP_REQ_NO = BaniraKeyBindings.register(NarcissusFarewell.MODID, "tp_req_no", GLFWKey.GLFW_KEY_UNKNOWN);
    public static final KeyBinding TP_GRAVE_KEY = BaniraKeyBindings.register(NarcissusFarewell.MODID, "tp_grave", GLFWKey.GLFW_KEY_UNKNOWN);
    public static final KeyBinding OPEN_SCREEN_KEY = BaniraKeyBindings.register(NarcissusFarewell.MODID, "open_screen", GLFWKey.GLFW_KEY_UNKNOWN);
    public static final KeyBinding OPEN_ACCESS_LIST_KEY = BaniraKeyBindings.register(NarcissusFarewell.MODID, "open_access_list", GLFWKey.GLFW_KEY_UNKNOWN);

    static {
        BaniraClientEventHub.ModLifecycle.onClientSetup(event -> {
            for (String id : NarcissusNotificationTypes.ALL_TYPE_IDS) {
                NotificationTypeRegistry.register(id);
            }
        });
        BaniraClientEventHub.ModLifecycle.onClientSetup(event ->
                LogoModifier.register(NarcissusFarewell.MODID, () -> Math.random() > 0.5 ? "logo_.png" : "logo.png"));
        BaniraClientEventHub.Client.onClientTick(ClientModEventHandler::onClientTick);
    }

    private ClientModEventHandler() {
    }

    /**
     * 由主模组构造函数经 {@link net.minecraftforge.fml.DistExecutor} 在客户端触发类初始化
     */
    public static void bootstrap() {
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (Minecraft.getInstance().screen == null && event.phase == TickEvent.Phase.END) {
            if (TP_HOME_KEY.consumeClick()) {
                if (!keyDown) {
                    PacketUtils.sendPacketToServer(NetworkInit.INSTANCE, new TpHomeToServer());
                    keyDown = true;
                }
            } else if (TP_GRAVE_KEY.consumeClick()) {
                if (!keyDown) {
                    PacketUtils.sendPacketToServer(NetworkInit.INSTANCE, new TpGraveToServer());
                    keyDown = true;
                }
            } else if (TP_BACK_KEY.consumeClick()) {
                if (!keyDown) {
                    PacketUtils.sendPacketToServer(NetworkInit.INSTANCE, new TpBackToServer());
                    keyDown = true;
                }
            } else if (TP_REQ_YES.consumeClick()) {
                if (!keyDown) {
                    PacketUtils.sendPacketToServer(NetworkInit.INSTANCE, new TpYesToServer());
                    keyDown = true;
                }
            } else if (TP_REQ_NO.consumeClick()) {
                if (!keyDown) {
                    PacketUtils.sendPacketToServer(NetworkInit.INSTANCE, new TpNoToServer());
                    keyDown = true;
                }
            } else if (OPEN_SCREEN_KEY.consumeClick()) {
                if (!keyDown) {
                    ScreenHelper.openScreen();
                    keyDown = true;
                }
            } else if (OPEN_ACCESS_LIST_KEY.consumeClick()) {
                if (!keyDown) {
                    ScreenHelper.openAccessListScreen();
                    keyDown = true;
                }
            } else {
                keyDown = false;
            }
        }
    }

}
