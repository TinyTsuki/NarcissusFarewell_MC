package xin.vanilla.narcissus.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import xin.vanilla.narcissus.BuildConfig;
import xin.vanilla.narcissus.network.*;

/**
 * 客户端事件处理器
 */
@Mod.EventBusSubscriber(modid = BuildConfig.MODID, value = Side.CLIENT)
public class ClientEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String CATEGORIES = "key.narcissus_farewell.categories";

    // 定义按键绑定
    public static KeyBinding TP_HOME_KEY = new KeyBinding("key.narcissus_farewell.tp_home",
            Keyboard.KEY_NONE, CATEGORIES);
    public static KeyBinding TP_BACK_KEY = new KeyBinding("key.narcissus_farewell.tp_back",
            Keyboard.KEY_NONE, CATEGORIES);
    public static KeyBinding TP_REQ_YES = new KeyBinding("key.narcissus_farewell.tp_req_yes",
            Keyboard.KEY_NONE, CATEGORIES);
    public static KeyBinding TP_REQ_NO = new KeyBinding("key.narcissus_farewell.tp_req_no",
            Keyboard.KEY_NONE, CATEGORIES);

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
        if (event.phase != TickEvent.Phase.END) return;
        if (Minecraft.getMinecraft() != null && Minecraft.getMinecraft().currentScreen != null) return;
        if (TP_HOME_KEY.isPressed()) {
            ModNetworkHandler.INSTANCE.sendToServer(new TpHomeNotice());
        } else if (TP_BACK_KEY.isPressed()) {
            ModNetworkHandler.INSTANCE.sendToServer(new TpBackNotice());
        } else if (TP_REQ_YES.isPressed()) {
            ModNetworkHandler.INSTANCE.sendToServer(new TpYesNotice());
        } else if (TP_REQ_NO.isPressed()) {
            ModNetworkHandler.INSTANCE.sendToServer(new TpNoNotice());
        }
    }
}
