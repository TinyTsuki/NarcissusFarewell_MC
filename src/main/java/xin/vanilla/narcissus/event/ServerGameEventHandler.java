package xin.vanilla.narcissus.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.util.TeleportCountdownTracker;

/**
 * 传送倒计时：受伤打断、登出清理。
 */
@Mod.EventBusSubscriber(modid = NarcissusFarewell.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ServerGameEventHandler {

    private ServerGameEventHandler() {
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
