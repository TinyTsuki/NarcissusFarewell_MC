package xin.vanilla.narcissus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xin.vanilla.narcissus.integration.map.FtbWaypointList;

@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbchunks.client.gui.WaypointEditorScreen", remap = false)
public abstract class FtbWaypointListMixin {
    @Inject(method = "collectWaypoints", at = @At("RETURN"), cancellable = true, remap = false)
    private static void narcissus$list(CallbackInfoReturnable<java.util.Map<?, ?>> callback) {
        callback.setReturnValue(FtbWaypointList.append(callback.getReturnValue()));
    }
}
