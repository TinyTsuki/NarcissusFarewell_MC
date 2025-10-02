package xin.vanilla.narcissus.event;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.network.packet.*;
import xin.vanilla.narcissus.util.NarcissusUtils;

/**
 * 客户端 Game事件处理器
 */
@Mod.EventBusSubscriber(modid = NarcissusFarewell.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientGameEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    private static boolean keyDown = false;

    /**
     * 客户端Tick事件
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        if (Minecraft.getInstance().screen == null) {
            // 快捷回家
            if (ClientModEventHandler.TP_HOME_KEY.consumeClick()) {
                if (!keyDown) {
                    NarcissusUtils.sendPacketToServer(new TpHomeNotice());
                    keyDown = true;
                }
            }
            // 快捷返回
            else if (ClientModEventHandler.TP_BACK_KEY.consumeClick()) {
                if (!keyDown) {
                    NarcissusUtils.sendPacketToServer(new TpBackNotice());
                    keyDown = true;
                }
            }
            // 快捷同意最近一条传送请求
            else if (ClientModEventHandler.TP_REQ_YES.consumeClick()) {
                if (!keyDown) {
                    NarcissusUtils.sendPacketToServer(new TpYesNotice());
                    keyDown = true;
                }
            }
            // 快捷拒绝最近一条传送请求
            else if (ClientModEventHandler.TP_REQ_NO.consumeClick()) {
                if (!keyDown) {
                    NarcissusUtils.sendPacketToServer(new TpNoNotice());
                    keyDown = true;
                }
            } else {
                keyDown = false;
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        LOGGER.debug("Client: Player logged in.");
        // 同步客户端配置到服务器
        NarcissusUtils.sendPacketToServer(new ClientModLoadedNotice());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        LOGGER.debug("Client: Player logged out.");
    }

    /**
     * 服务端Tick事件
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        EventHandlerProxy.onServerTick(event);
    }

    /**
     * 玩家死亡后重生或者从末地回主世界
     */
    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        EventHandlerProxy.onPlayerCloned(event);
    }

    /**
     * 实体进入世界事件
     */
    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        EventHandlerProxy.onEntityJoinWorld(event);
    }

    /**
     * 实体传送事件
     */
    @SubscribeEvent
    public static void onEntityTeleport(EntityTeleportEvent event) {
        EventHandlerProxy.onEntityTeleport(event);
    }

}
