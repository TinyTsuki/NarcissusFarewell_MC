package xin.vanilla.narcissus.internal.forge.event;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.EntityTeleportEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraft.entity.player.ServerPlayerEntity;
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
            if (event.getOriginal() instanceof ServerPlayerEntity && event.getPlayer() instanceof ServerPlayerEntity) {
                EventHandlerProxy.onPlayerCloned((ServerPlayerEntity) event.getOriginal(),
                        (ServerPlayerEntity) event.getPlayer(), event.isWasDeath());
            }
        });
        MinecraftForge.EVENT_BUS.addListener((EntityJoinWorldEvent event) -> {
            if (event.getEntity() instanceof ServerPlayerEntity) {
                EventHandlerProxy.onPlayerJoinWorld((ServerPlayerEntity) event.getEntity());
            }
        });
        MinecraftForge.EVENT_BUS.addListener((EntityTeleportEvent event) -> {
            if (event.getEntity() instanceof ServerPlayerEntity) {
                EventHandlerProxy.onPlayerTeleport((ServerPlayerEntity) event.getEntity(),
                        event.getPrev(), event.getTarget());
            }
        });
        MinecraftForge.EVENT_BUS.addListener((LivingHurtEvent event) -> {
            if (event.getAmount() > 0f && event.getEntityLiving() instanceof ServerPlayerEntity) {
                TeleportCountdownTracker.onPlayerHurt((ServerPlayerEntity) event.getEntityLiving());
            }
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getPlayer() instanceof ServerPlayerEntity) {
                TeleportCountdownTracker.onPlayerLogout(event.getPlayer().getUUID());
            }
        });
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
                NarcissusCommand.register(event.getDispatcher()));
    }
}
