package xin.vanilla.narcissus.event;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fmlclient.registry.ClientRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.network.*;

/**
 * 客户端事件处理器
 */
@Mod.EventBusSubscriber(modid = NarcissusFarewell.MODID, value = Dist.CLIENT)
public class ClientEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String CATEGORIES = "key.narcissus_farewell.categories";

    // 定义按键绑定
    public static KeyMapping TP_HOME_KEY = new KeyMapping("key.narcissus_farewell.tp_home",
            -1, CATEGORIES);
    public static KeyMapping TP_BACK_KEY = new KeyMapping("key.narcissus_farewell.tp_back",
            -1, CATEGORIES);
    public static KeyMapping TP_REQ_YES = new KeyMapping("key.narcissus_farewell.tp_req_yes",
            -1, CATEGORIES);
    public static KeyMapping TP_REQ_NO = new KeyMapping("key.narcissus_farewell.tp_req_no",
            -1, CATEGORIES);

    /**
     * 注册键绑定
     */
    public static void registerKeyBindings() {
        ClientRegistry.registerKeyBinding(TP_HOME_KEY);
        ClientRegistry.registerKeyBinding(TP_BACK_KEY);
        ClientRegistry.registerKeyBinding(TP_REQ_YES);
        ClientRegistry.registerKeyBinding(TP_REQ_NO);
    }

    private static boolean keyDown = false;

    /**
     * 在客户端Tick事件触发时执行
     *
     * @param event 客户端Tick事件
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (Minecraft.getInstance().screen == null && event.phase == TickEvent.Phase.END) {
            // 快捷回家
            if (TP_HOME_KEY.consumeClick()) {
                if (!keyDown) {
                    ModNetworkHandler.INSTANCE.sendToServer(new TpHomeNotice());
                    keyDown = true;
                }
            }
            // 快捷返回
            else if (TP_BACK_KEY.consumeClick()) {
                if (!keyDown) {
                    ModNetworkHandler.INSTANCE.sendToServer(new TpBackNotice());
                    keyDown = true;
                }
            }
            // 快捷同意最近一条传送请求
            else if (TP_REQ_YES.consumeClick()) {
                if (!keyDown) {
                    ModNetworkHandler.INSTANCE.sendToServer(new TpYesNotice());
                    keyDown = true;
                }
            }
            // 快捷拒绝最近一条传送请求
            else if (TP_REQ_NO.consumeClick()) {
                if (!keyDown) {
                    ModNetworkHandler.INSTANCE.sendToServer(new TpNoNotice());
                    keyDown = true;
                }
            } else {
                keyDown = false;
            }
        }
    }
}
