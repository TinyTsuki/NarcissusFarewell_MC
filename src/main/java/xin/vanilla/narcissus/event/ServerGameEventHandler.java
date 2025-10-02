package xin.vanilla.narcissus.event;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.NarcissusFarewell;

/**
 * 服务端 Game事件处理器
 */
@EventBusSubscriber(modid = NarcissusFarewell.MODID, value = Dist.DEDICATED_SERVER)
public class ServerGameEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * 服务端Tick事件
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
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
