package xin.vanilla.narcissus.event;

import net.minecraft.entity.player.ServerPlayerEntity;
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
        if (!(event.getEntityLiving() instanceof ServerPlayerEntity)) {
            return;
        }
        if (event.getAmount() <= 0f) {
            return;
        }
        TeleportCountdownTracker.onPlayerHurt((ServerPlayerEntity) event.getEntityLiving());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            TeleportCountdownTracker.onPlayerLogout(event.getPlayer().getUUID());
        }
    }
}
