package xin.vanilla.narcissus.mixin;

import journeymap.client.waypoint.JmReader;
import journeymap.client.waypoint.Waypoint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xin.vanilla.narcissus.integration.map.LegacyJourneyMapFiles;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

@Pseudo
@Mixin(targets = "journeymap.client.waypoint.JmReader", remap = false)
public abstract class JourneyMapWaypointReaderMixin {
    @Shadow(remap = false)
    private Waypoint load(File file) {
        throw new AssertionError();
    }

    @Inject(method = "loadWaypoints(Ljava/io/File;)Ljava/util/Collection;", at = @At("HEAD"), remap = false)
    private void narcissus$prepareWaypointFiles(File directory, CallbackInfoReturnable<Collection<?>> callback) throws IOException {
        LegacyJourneyMapFiles.prepareSafely(directory.toPath());
    }

    @Redirect(method = "loadWaypoints(Ljava/io/File;)Ljava/util/Collection;",
            at = @At(value = "INVOKE", target = "Ljourneymap/client/waypoint/JmReader;load(Ljava/io/File;)Ljourneymap/client/waypoint/Waypoint;"), remap = false)
    private Waypoint narcissus$loadWithoutUnprovenRename(JmReader reader, File source) throws IOException {
        if (!LegacyJourneyMapFiles.safePath(source.toPath())) return null;
        Waypoint point = load(source);
        // The reader deletes mismatched sources after collecting every loaded point.
        if (point != null && LegacyJourneyMapFiles.fileName(point.getOrigin(), point.getDisplayId()) != null
                && !source.getName().equals(point.getFileName())) {
            // Keep a failed owned migration on disk; do not interrupt unrelated native waypoints.
            return null;
        }
        return point;
    }
}
