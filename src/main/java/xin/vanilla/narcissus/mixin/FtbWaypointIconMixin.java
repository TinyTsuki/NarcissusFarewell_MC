package xin.vanilla.narcissus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.narcissus.integration.map.ReadOnlyFtbIcon;
import xin.vanilla.narcissus.integration.map.FtbEventWaypointAdapter;

@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbchunks.client.mapicon.WaypointMapIcon", remap = false)
public abstract class FtbWaypointIconMixin implements ReadOnlyFtbIcon {
    @Inject(method = "draw", at = @At("HEAD"), cancellable = true, remap = false)
    private void narcissus$draw(CallbackInfo callback) {
        if (FtbEventWaypointAdapter.isOwnedIcon(this) && !FtbEventWaypointAdapter.isActiveIcon(this))
            callback.cancel();
    }

    @Inject(method = {"onMousePressed", "onKeyPressed"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void narcissus$readOnly(CallbackInfoReturnable<Boolean> callback) {
        if (FtbEventWaypointAdapter.isOwnedIcon(this)) callback.setReturnValue(false);
    }

    @Inject(method = "isVisible", at = @At("HEAD"), cancellable = true, remap = false)
    private void narcissus$visibility(CallbackInfoReturnable<Boolean> callback) {
        if (FtbEventWaypointAdapter.isOwnedIcon(this) && !FtbEventWaypointAdapter.isActiveIcon(this))
            callback.setReturnValue(false);
    }
}

