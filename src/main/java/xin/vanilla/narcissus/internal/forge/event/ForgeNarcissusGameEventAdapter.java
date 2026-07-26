package xin.vanilla.narcissus.internal.forge.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import xin.vanilla.narcissus.command.NarcissusCommand;
import xin.vanilla.narcissus.event.EventHandlerProxy;
import xin.vanilla.narcissus.util.TeleportCountdownTracker;

/**
 * 将 Forge 高变动游戏事件转换到 Narcissus 业务处理层。
 */
public final class ForgeNarcissusGameEventAdapter {
    private static boolean registered;

    private ForgeNarcissusGameEventAdapter() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.Clone event) -> {
            if (event.getOriginal() instanceof ServerPlayer && event.getEntity() instanceof ServerPlayer) {
                EventHandlerProxy.onPlayerCloned((ServerPlayer) event.getOriginal(),
                        (ServerPlayer) event.getEntity(), event.isWasDeath());
            }
        });
        MinecraftForge.EVENT_BUS.addListener((EntityJoinLevelEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer) {
                EventHandlerProxy.onPlayerJoinWorld((ServerPlayer) event.getEntity());
            }
        });
        MinecraftForge.EVENT_BUS.addListener((EntityTeleportEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer) {
                EventHandlerProxy.onPlayerTeleport((ServerPlayer) event.getEntity(),
                        event.getPrev(), event.getTarget());
            }
        });
        MinecraftForge.EVENT_BUS.addListener((LivingHurtEvent event) -> {
            if (event.getAmount() > 0f && event.getEntity() instanceof ServerPlayer) {
                TeleportCountdownTracker.onPlayerHurt((ServerPlayer) event.getEntity());
            }
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer) {
                TeleportCountdownTracker.onPlayerLogout(event.getEntity().getUUID());
            }
        });
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
                NarcissusCommand.register(event.getDispatcher()));
    }
}
