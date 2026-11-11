package xin.vanilla.narcissus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xin.vanilla.narcissus.integration.map.LegacyJourneyMapFiles;

@Pseudo
@Mixin(targets = "journeymap.client.waypoint.Waypoint", remap = false)
public abstract class JourneyMapWaypointFileMixin {
    @Shadow(remap = false)
    public abstract String getOrigin();

    @Shadow(remap = false)
    public abstract String getDisplayId();

    @Inject(method = "getFileName()Ljava/lang/String;", at = @At("HEAD"), cancellable = true, remap = false)
    private void narcissus$waypointFileName(CallbackInfoReturnable<String> callback) {
        String file = LegacyJourneyMapFiles.fileName(getOrigin(), getDisplayId());
        if (file != null) callback.setReturnValue(file);
    }
}
