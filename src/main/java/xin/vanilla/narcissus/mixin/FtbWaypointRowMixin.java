package xin.vanilla.narcissus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xin.vanilla.narcissus.integration.map.FtbWaypointList;
import xin.vanilla.narcissus.integration.map.ReadOnlyFtbRow;

@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbchunks.client.gui.WaypointEditorScreen$RowPanel", remap = false)
public abstract class FtbWaypointRowMixin implements ReadOnlyFtbRow {
    @Inject(method = {"mousePressed", "keyPressed"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void narcissus$readOnly(CallbackInfoReturnable<Boolean> callback) {
        if (FtbWaypointList.isOwnedRow(this)) callback.setReturnValue(false);
    }
}
