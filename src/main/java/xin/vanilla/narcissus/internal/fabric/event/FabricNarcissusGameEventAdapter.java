package xin.vanilla.narcissus.internal.fabric.event;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import xin.vanilla.narcissus.command.NarcissusCommand;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.event.EventHandlerProxy;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 将 Fabric 原生回调转换为 Narcissus 的稳定业务方法。
 */
public final class FabricNarcissusGameEventAdapter {
    private static final Map<UUID, SafeWorldCoordinate> PENDING_DEATH_POSITIONS = new ConcurrentHashMap<>();
    private static boolean registered;

    private FabricNarcissusGameEventAdapter() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                NarcissusCommand.register(dispatcher));
        // COPY_FROM 发生在新玩家移动到复活点之前，此时旧实体仍保留真实死亡坐标。
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            if (alive) {
                PENDING_DEATH_POSITIONS.remove(oldPlayer.getUUID());
            } else {
                PENDING_DEATH_POSITIONS.put(oldPlayer.getUUID(), new SafeWorldCoordinate(oldPlayer));
            }
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            SafeWorldCoordinate deathPosition = PENDING_DEATH_POSITIONS.remove(oldPlayer.getUUID());
            if (!alive && deathPosition != null) {
                EventHandlerProxy.onPlayerRespawned(newPlayer, deathPosition);
            }
        });
    }
}
