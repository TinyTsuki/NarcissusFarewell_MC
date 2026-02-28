package xin.vanilla.narcissus.event;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.NarcissusFarewell;

/**
 * 客户端 Mod事件处理器
 */
@Mod.EventBusSubscriber(modid = NarcissusFarewell.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientModEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String CATEGORIES = "word.narcissus_farewell.categories";

    // 定义按键绑定
    public static KeyMapping TP_HOME_KEY = new KeyMapping("word.narcissus_farewell.tp_home",
            -1, CATEGORIES);
    public static KeyMapping TP_BACK_KEY = new KeyMapping("word.narcissus_farewell.tp_back",
            -1, CATEGORIES);
    public static KeyMapping TP_REQ_YES = new KeyMapping("word.narcissus_farewell.tp_req_yes",
            -1, CATEGORIES);
    public static KeyMapping TP_REQ_NO = new KeyMapping("word.narcissus_farewell.tp_req_no",
            -1, CATEGORIES);
    public static KeyMapping TP_GRAVE_KEY = new KeyMapping("word.narcissus_farewell.tp_grave",
            -1, CATEGORIES);
    public static KeyMapping OPEN_SCREEN_KEY = new KeyMapping("word.narcissus_farewell.open_screen",
            -1, CATEGORIES);

    /**
     * 注册键绑定
     */
    public static void registerKeyBindings() {
        ClientRegistry.registerKeyBinding(TP_HOME_KEY);
        ClientRegistry.registerKeyBinding(TP_BACK_KEY);
        ClientRegistry.registerKeyBinding(TP_REQ_YES);
        ClientRegistry.registerKeyBinding(TP_REQ_NO);
        ClientRegistry.registerKeyBinding(TP_GRAVE_KEY);
        ClientRegistry.registerKeyBinding(OPEN_SCREEN_KEY);
    }

}
