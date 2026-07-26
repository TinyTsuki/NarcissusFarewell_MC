package xin.vanilla.narcissus.internal.neoforge.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import xin.vanilla.narcissus.command.NarcissusCommand;
import xin.vanilla.narcissus.event.EventHandlerProxy;
import xin.vanilla.narcissus.util.TeleportCountdownTracker;

/**
 * 将 NeoForge 游戏事件转换为不依赖加载器类型的业务参数。
 */
public final class NeoForgeNarcissusGameEventAdapter {
    private static boolean registered;

    private NeoForgeNarcissusGameEventAdapter() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        NeoForge.EVENT_BUS.addListener((PlayerEvent.Clone event) -> {
            if (event.getOriginal() instanceof ServerPlayer && event.getEntity() instanceof ServerPlayer) {
                EventHandlerProxy.onPlayerCloned((ServerPlayer) event.getOriginal(),
                        (ServerPlayer) event.getEntity(), event.isWasDeath());
            }
        });
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer) {
                EventHandlerProxy.onPlayerJoinWorld((ServerPlayer) event.getEntity());
            }
        });
        NeoForge.EVENT_BUS.addListener((EntityTeleportEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer) {
                EventHandlerProxy.onPlayerTeleport((ServerPlayer) event.getEntity(),
                        event.getPrev(), event.getTarget());
            }
        });
        NeoForge.EVENT_BUS.addListener((LivingDamageEvent.Pre event) -> {
            if (event.getNewDamage() > 0f && event.getEntity() instanceof ServerPlayer) {
                TeleportCountdownTracker.onPlayerHurt((ServerPlayer) event.getEntity());
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer) {
                TeleportCountdownTracker.onPlayerLogout(event.getEntity().getUUID());
            }
        });
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
                NarcissusCommand.register(event.getDispatcher()));
    }
}
