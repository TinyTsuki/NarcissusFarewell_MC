package xin.vanilla.narcissus.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.narcissus.event.EventHandlerProxy;

/**
 * Fabric 1.16 没有通用玩家传送事件，在原版传送入口补齐业务回调。
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerTeleportMixin {
    @Inject(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDFF)V", at = @At("HEAD"))
    private void narcissus$beforeTeleport(ServerLevel targetLevel, double x, double y, double z,
                                          float yaw, float pitch, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        EventHandlerProxy.onPlayerTeleport(player, player.position(), new Vec3(x, y, z));
    }
}
