package xin.vanilla.narcissus.integration.grave;

import de.maxhenkel.corpse.entities.CorpseEntity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.common.util.DimensionUtils;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;

import java.util.List;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/**
 * Corpse 模组遗体查找
 */
public final class CorpseGraveIntegration {
    private static final Logger LOGGER = LogManager.getLogger();
    private static Method ownerMethod;
    private static boolean lookupAttempted;

    private CorpseGraveIntegration() {
    }

    /**
     * 在指定坐标附近搜索属于该玩家的遗体
     */
    public static void findNear(SafeWorldCoordinate center, int radius, UUID playerUuid, List<SafeWorldCoordinate> out) {
        try {

            ResourceKey<Level> dim = center.dimension();
            ServerLevel world = DimensionUtils.getLevel(dim);
            if (world == null) return;

            double cx = center.x(), cy = center.y(), cz = center.z();
            double r2 = (double) radius * radius;

            for (Entity entity : world.getAllEntities()) {
                if (entity instanceof CorpseEntity corpse) {
                    if (playerUuid.equals(ownerOf(corpse))) {
                        double dx = entity.getX() - cx, dy = entity.getY() - cy, dz = entity.getZ() - cz;
                        if (dx * dx + dy * dy + dz * dz <= r2) {
                            out.add(new SafeWorldCoordinate(entity.getX(), entity.getY(), entity.getZ(), world.dimension()));
                        }
                    }
                }
            }
        } catch (ReflectiveOperationException | LinkageError error) {
            LOGGER.warn("Unable to read Corpse owner", error);
        }
    }

    private static UUID ownerOf(CorpseEntity corpse) throws ReflectiveOperationException {
        if (!lookupAttempted) {
            lookupAttempted = true;
            try {
                ownerMethod = CorpseEntity.class.getMethod("getPlayerUuid");
            } catch (NoSuchMethodException modernMissing) {
                try {
                    ownerMethod = CorpseEntity.class.getMethod("getCorpseUUID");
                } catch (NoSuchMethodException legacyMissing) {
                    LOGGER.warn("Corpse owner API is unavailable", legacyMissing);
                }
            }
        }
        if (ownerMethod == null) return null;
        Object owner = ownerMethod.invoke(corpse);
        if (owner instanceof Optional<?> optional) owner = optional.orElse(null);
        return owner instanceof UUID uuid ? uuid : null;
    }

    public static SafeWorldCoordinate parseObituary(ServerPlayer player) {
        return null;
    }
}
