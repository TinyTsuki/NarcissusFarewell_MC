package xin.vanilla.narcissus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xin.vanilla.narcissus.integration.map.LegacyJourneyMapFiles;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

@Pseudo
@Mixin(targets = "journeymap.client.waypoint.JmReader", remap = false)
public abstract class JourneyMapWaypointReaderMixin {
    @Inject(method = "loadWaypoints(Ljava/io/File;)Ljava/util/Collection;", at = @At("HEAD"), remap = false)
    private void narcissus$prepareWaypointFiles(File directory, CallbackInfoReturnable<Collection<?>> callback) throws IOException {
        LegacyJourneyMapFiles.prepare(directory.toPath());
    }
}
