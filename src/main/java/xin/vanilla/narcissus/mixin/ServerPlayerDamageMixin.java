package xin.vanilla.narcissus.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xin.vanilla.narcissus.util.TeleportCountdownTracker;

/**
 * 将 Fabric 缺失的非致命受伤事件转换为倒计时取消信号。
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDamageMixin {
    @Inject(method = "hurt", at = @At("HEAD"))
    private void narcissus$onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (amount > 0f) {
            TeleportCountdownTracker.onPlayerHurt((ServerPlayer) (Object) this);
        }
    }
}
