package xin.vanilla.narcissus.event;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
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
    public static KeyBinding TP_HOME_KEY = new KeyBinding("key.narcissus_farewell.tp_home",
            -1, CATEGORIES);
    public static KeyBinding TP_BACK_KEY = new KeyBinding("key.narcissus_farewell.tp_back",
            -1, CATEGORIES);
    public static KeyBinding TP_REQ_YES = new KeyBinding("key.narcissus_farewell.tp_req_yes",
            -1, CATEGORIES);
    public static KeyBinding TP_REQ_NO = new KeyBinding("key.narcissus_farewell.tp_req_no",
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

    /**
     * 在客户端Tick事件触发时执行
     *
     * @param event 客户端Tick事件
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        // 检测并消费点击事件
        if (TP_HOME_KEY.consumeClick()) {
            // 快捷回家
            ModNetworkHandler.INSTANCE.sendToServer(new TpHomeNotice());
        } else if (TP_BACK_KEY.consumeClick()) {
            // 快捷返回
            ModNetworkHandler.INSTANCE.sendToServer(new TpBackNotice());
        } else if (TP_REQ_YES.consumeClick()) {
            // 快捷同意最近一条传送请求
            ModNetworkHandler.INSTANCE.sendToServer(new TpYesNotice());
        } else if (TP_REQ_NO.consumeClick()) {
            // 快捷拒绝最近一条传送请求
            ModNetworkHandler.INSTANCE.sendToServer(new TpNoNotice());
        }
    }
}
