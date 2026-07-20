package xin.vanilla.narcissus.event;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.api.client.BaniraInput;
import xin.vanilla.banira.api.client.BaniraKeyHandle;
import xin.vanilla.banira.api.client.BaniraLogos;
import xin.vanilla.banira.api.client.event.BaniraClientEvents;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.notification.NotificationTypeRegistry;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.integration.ScreenHelper;
import xin.vanilla.narcissus.internal.client.dev.NarcissusUiSmokeRunner;
import xin.vanilla.narcissus.internal.client.dev.NarcissusNetworkSmokeClientRunner;
import xin.vanilla.narcissus.network.packet.*;
import xin.vanilla.narcissus.notification.NarcissusNotificationTypes;

/**
 * 客户端：Banira 键位入队与稳定事件回调注册（不在此类上使用 Forge {@code @SubscribeEvent}）。
 */
@OnlyIn(Dist.CLIENT)
public final class ClientModEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    private static boolean keyDown;

    public static final BaniraKeyHandle TP_HOME_KEY = BaniraInput.registerKey(NarcissusFarewell.MODID, "tp_home", GLFWKey.GLFW_KEY_UNKNOWN);
    public static final BaniraKeyHandle TP_BACK_KEY = BaniraInput.registerKey(NarcissusFarewell.MODID, "tp_back", GLFWKey.GLFW_KEY_UNKNOWN);
    public static final BaniraKeyHandle TP_REQ_YES = BaniraInput.registerKey(NarcissusFarewell.MODID, "tp_req_yes", GLFWKey.GLFW_KEY_UNKNOWN);
    public static final BaniraKeyHandle TP_REQ_NO = BaniraInput.registerKey(NarcissusFarewell.MODID, "tp_req_no", GLFWKey.GLFW_KEY_UNKNOWN);
    public static final BaniraKeyHandle TP_GRAVE_KEY = BaniraInput.registerKey(NarcissusFarewell.MODID, "tp_grave", GLFWKey.GLFW_KEY_UNKNOWN);
    public static final BaniraKeyHandle OPEN_SCREEN_KEY = BaniraInput.registerKey(NarcissusFarewell.MODID, "open_screen", GLFWKey.GLFW_KEY_UNKNOWN);
    public static final BaniraKeyHandle OPEN_ACCESS_LIST_KEY = BaniraInput.registerKey(NarcissusFarewell.MODID, "open_access_list", GLFWKey.GLFW_KEY_UNKNOWN);

    static {
        BaniraClientEvents.ModLifecycle.onClientSetup(event -> {
            for (String id : NarcissusNotificationTypes.ALL_TYPE_IDS) {
                NotificationTypeRegistry.register(id);
            }
        });
        BaniraClientEvents.ModLifecycle.onClientSetup(event ->
                BaniraLogos.register(NarcissusFarewell.MODID, () -> Math.random() > 0.5 ? "logo_.png" : "logo.png"));
        BaniraClientEvents.Client.onClientTick(ClientModEventHandler::onClientTick);
    }

    private ClientModEventHandler() {
    }

    /**
     * 由主模组构造函数经 {@link net.minecraftforge.fml.DistExecutor} 在客户端触发类初始化
     */
    public static void bootstrap() {
        NarcissusUiSmokeRunner.register();
        NarcissusNetworkSmokeClientRunner.register();
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
