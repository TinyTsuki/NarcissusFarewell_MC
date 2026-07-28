package xin.vanilla.narcissus.event;

import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.api.client.BaniraInput;
import xin.vanilla.banira.api.client.BaniraKeyHandle;
import xin.vanilla.banira.api.client.BaniraLogos;
import xin.vanilla.banira.api.client.event.BaniraClientEvents;
import xin.vanilla.banira.api.client.notification.BaniraClientNotificationTypes;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.integration.ScreenHelper;
import xin.vanilla.narcissus.internal.client.dev.NarcissusNetworkSmokeClientRunner;
import xin.vanilla.narcissus.internal.client.dev.NarcissusUiSmokeRunner;
import xin.vanilla.narcissus.network.packet.*;
import xin.vanilla.narcissus.notification.NarcissusNotificationTypes;

/**
 * 客户端：Banira 键位入队与稳定事件回调注册（不在此类上使用 Forge {@code @SubscribeEvent}）
 */
public final class ClientModEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    private static boolean registered;
    private static boolean keyDown;

    public static final BaniraKeyHandle TP_HOME_KEY = BaniraInput.registerKey(NarcissusFarewell.MODID, "tp_home", GLFWKey.GLFW_KEY_UNKNOWN);
    public static final BaniraKeyHandle TP_BACK_KEY = BaniraInput.registerKey(NarcissusFarewell.MODID, "tp_back", GLFWKey.GLFW_KEY_UNKNOWN);
    public static final BaniraKeyHandle TP_REQ_YES = BaniraInput.registerKey(NarcissusFarewell.MODID, "tp_req_yes", GLFWKey.GLFW_KEY_UNKNOWN);
    public static final BaniraKeyHandle TP_REQ_NO = BaniraInput.registerKey(NarcissusFarewell.MODID, "tp_req_no", GLFWKey.GLFW_KEY_UNKNOWN);
    public static final BaniraKeyHandle TP_GRAVE_KEY = BaniraInput.registerKey(NarcissusFarewell.MODID, "tp_grave", GLFWKey.GLFW_KEY_UNKNOWN);
    public static final BaniraKeyHandle OPEN_SCREEN_KEY = BaniraInput.registerKey(NarcissusFarewell.MODID, "open_screen", GLFWKey.GLFW_KEY_UNKNOWN);
    public static final BaniraKeyHandle OPEN_ACCESS_LIST_KEY = BaniraInput.registerKey(NarcissusFarewell.MODID, "open_access_list", GLFWKey.GLFW_KEY_UNKNOWN);

    private ClientModEventHandler() {
    }

    /**
     * 显式注册客户端生命周期、输入事件和 dev-only runner。
     */
    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;

        registerNotificationTypes();
        BaniraLogos.register(NarcissusFarewell.MODID, () -> Math.random() > 0.5 ? "logo_.png" : "logo.png");
        BaniraClientEvents.Client.onClientTick(ClientModEventHandler::onClientTick);
        NarcissusUiSmokeRunner.register();
        NarcissusNetworkSmokeClientRunner.register();
    }

    /**
     * 通知类型的名称和说明由子 Mod 登记，Banira 只负责展示。
     */
    private static void registerNotificationTypes() {
        BaniraClientNotificationTypes.registerModDisplayName(NarcissusFarewell.MODID, NarcissusComponent.get().transClientAuto("mod_name"));
        registerNotificationType(NarcissusNotificationTypes.TELEPORT_REQUEST, "notification_type_teleport_request");
        registerNotificationType(NarcissusNotificationTypes.TELEPORT_GUARD, "notification_type_teleport_guard");
        registerNotificationType(NarcissusNotificationTypes.TELEPORT_SEARCH, "notification_type_teleport_search");
        registerNotificationType(NarcissusNotificationTypes.TELEPORT_ERROR, "notification_type_teleport_error");
        registerNotificationType(NarcissusNotificationTypes.WAYPOINT, "notification_type_waypoint");
        registerNotificationType(NarcissusNotificationTypes.INTERACTIVE_TP_FLOW, "notification_type_interactive_tp_flow");
        registerNotificationType(NarcissusNotificationTypes.INTERACTIVE_SHARE, "notification_type_interactive_share");
        registerNotificationType(NarcissusNotificationTypes.INTERACTIVE_HELP, "notification_type_interactive_help");
        registerNotificationType(NarcissusNotificationTypes.INTERACTIVE_COORDINATE_LIST, "notification_type_interactive_coordinate_list");
        registerNotificationType(NarcissusNotificationTypes.INTERACTIVE_QUERY, "notification_type_interactive_query");
    }

    private static void registerNotificationType(String typeId, String descriptionKey) {
        BaniraClientNotificationTypes.register(typeId, NarcissusComponent.get().transClientAuto(descriptionKey));
    }

    private static void onClientTick(xin.vanilla.banira.api.client.event.BaniraClientTickEvent event) {
        NarcissusUiSmokeRunner.tick(Minecraft.getInstance());
        NarcissusNetworkSmokeClientRunner.tick(Minecraft.getInstance());
        if (Minecraft.getInstance().screen == null) {
            if (TP_HOME_KEY.consumeClick()) {
                if (!keyDown) {
                    PacketUtils.sendPacketToServer(new TpHomeToServer());
                    keyDown = true;
                }
            } else if (TP_GRAVE_KEY.consumeClick()) {
                if (!keyDown) {
                    PacketUtils.sendPacketToServer(new TpGraveToServer());
                    keyDown = true;
                }
            } else if (TP_BACK_KEY.consumeClick()) {
                if (!keyDown) {
                    PacketUtils.sendPacketToServer(new TpBackToServer());
                    keyDown = true;
                }
            } else if (TP_REQ_YES.consumeClick()) {
                if (!keyDown) {
                    PacketUtils.sendPacketToServer(new TpYesToServer());
                    keyDown = true;
                }
            } else if (TP_REQ_NO.consumeClick()) {
                if (!keyDown) {
                    PacketUtils.sendPacketToServer(new TpNoToServer());
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
