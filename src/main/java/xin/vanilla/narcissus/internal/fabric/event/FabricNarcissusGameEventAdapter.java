package xin.vanilla.narcissus.internal.fabric.event;

import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import xin.vanilla.narcissus.command.NarcissusCommand;
import xin.vanilla.narcissus.event.EventHandlerProxy;

/** 将 Fabric 原生回调转换为 Narcissus 的稳定业务方法。 */
public final class FabricNarcissusGameEventAdapter {
    private static boolean registered;

    private FabricNarcissusGameEventAdapter() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) ->
                NarcissusCommand.register(dispatcher));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                EventHandlerProxy.onPlayerCloned(oldPlayer, newPlayer, !alive));
    }
}
