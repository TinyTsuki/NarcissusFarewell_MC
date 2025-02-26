package xin.vanilla.narcissus.event;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.network.packet.TpBackNotice;
import xin.vanilla.narcissus.network.packet.TpHomeNotice;
import xin.vanilla.narcissus.network.packet.TpNoNotice;
import xin.vanilla.narcissus.network.packet.TpYesNotice;
import xin.vanilla.narcissus.util.LogoModifier;

/**
 * 客户端事件处理器
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = NarcissusFarewell.MODID, value = Dist.CLIENT)
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
     * 在客户端设置阶段触发的事件处理方法
     * 此方法主要用于接收 FML 客户端设置事件，并执行相应的初始化操作
     */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 修改logo为随机logo
        ModList.get().getMods().stream()
                .filter(info -> info.getModId().equals(NarcissusFarewell.MODID))
                .findFirst()
                .ifPresent(LogoModifier::modifyLogo);
    }

    /**
     * 注册键绑定
     */
    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TP_HOME_KEY);
        event.register(TP_BACK_KEY);
        event.register(TP_REQ_YES);
        event.register(TP_REQ_NO);
    }

    private static boolean keyDown = false;

    /**
     * 在客户端Tick事件触发时执行
     *
     * @param event 客户端Tick事件
     */
    // @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (Minecraft.getInstance().screen == null && event.phase == TickEvent.Phase.END) {
            // 快捷回家
            if (TP_HOME_KEY.consumeClick()) {
                if (!keyDown) {
                    PacketDistributor.SERVER.noArg().send(new TpHomeNotice());
                    keyDown = true;
                }
            }
            // 快捷返回
            else if (TP_BACK_KEY.consumeClick()) {
                if (!keyDown) {
                    PacketDistributor.SERVER.noArg().send(new TpBackNotice());
                    keyDown = true;
                }
            }
            // 快捷同意最近一条传送请求
            else if (TP_REQ_YES.consumeClick()) {
                if (!keyDown) {
                    PacketDistributor.SERVER.noArg().send(new TpYesNotice());
                    keyDown = true;
                }
            }
            // 快捷拒绝最近一条传送请求
            else if (TP_REQ_NO.consumeClick()) {
                if (!keyDown) {
                    PacketDistributor.SERVER.noArg().send(new TpNoNotice());
                    keyDown = true;
                }
            } else {
                keyDown = false;
            }
        }
    }
}
