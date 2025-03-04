package xin.vanilla.narcissus.event;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import xin.vanilla.narcissus.network.*;

/**
 * 客户端事件处理器
 */
public class ClientEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String CATEGORIES = "key.narcissus_farewell.categories";

    public ClientEventHandler() {
        // 注册事件到事件总线
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

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
    public void onClientTick(TickEvent.ClientTickEvent event) {
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

    /**
     * 玩家进入世界
     */
    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.entity instanceof EntityPlayerSP) {
            if (event.entity.isEntityAlive()) {
                LOGGER.debug("Client: Player join world.");
                // 同步客户端配置到服务器
                try {
                    ModNetworkHandler.INSTANCE.sendToServer(new ClientModLoadedNotice());
                } catch (Exception e) {
                    LOGGER.error("Failed to send ClientModLoadedNotice", e);
                }
            }
        }
    }
}
