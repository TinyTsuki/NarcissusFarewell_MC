package xin.vanilla.narcissus.event;

import net.minecraft.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.NarcissusFarewell;

/**
 * 服务端 Game事件处理器
 */
@Mod.EventBusSubscriber(modid = NarcissusFarewell.MODID, value = Dist.DEDICATED_SERVER, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerGameEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * 服务端Tick事件
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        EventHandlerProxy.onServerTick(event);
    }

    /**
     * 玩家Tick事件
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        EventHandlerProxy.onPlayerTick(event);
    }

    /**
     * 能力附加事件
     */
    @SubscribeEvent
    public static void onAttachCapabilityEvent(AttachCapabilitiesEvent<Entity> event) {
        EventHandlerProxy.onAttachCapability(event);
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
    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        EventHandlerProxy.onEntityJoinWorld(event);
    }

    /**
     * 玩家登录事件
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        EventHandlerProxy.onPlayerLoggedOut(event);
    }
}
