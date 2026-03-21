package xin.vanilla.narcissus.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingHurtEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.util.TeleportCountdownTracker;

/**
 * 传送倒计时：受伤打断、登出清理。
 */
@EventBusSubscriber(modid = NarcissusFarewell.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class ServerGameEventHandler {
    private ServerGameEventHandler() {
    }

    /**
     * 服务端Tick事件
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        EventHandlerProxy.onServerTick(event);
    }


    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getAmount() <= 0f) {
            return;
        }
        TeleportCountdownTracker.onPlayerHurt(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            TeleportCountdownTracker.onPlayerLogout(event.getEntity().getUUID());
        }
    }
}
