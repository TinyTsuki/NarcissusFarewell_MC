package xin.vanilla.narcissus.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xin.vanilla.narcissus.event.EventHandlerProxy;

/**
 * Fabric 没有通用玩家传送事件，在原版传送入口补齐业务回调。
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerTeleportMixin {
    @Inject(method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;", at = @At("HEAD"))
    private void narcissus$beforeTeleport(TeleportTransition transition, CallbackInfoReturnable<ServerPlayer> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.isRemoved()) return;
        var destination = PositionMoveRotation.calculateAbsolute(PositionMoveRotation.of(player),
                PositionMoveRotation.of(transition), transition.relatives()).position();
        EventHandlerProxy.onPlayerTeleport(player, player.position(), destination, transition.newLevel().dimension());
    }
}
