package xin.vanilla.narcissus.integration.grave;

// import de.maxhenkel.corpse.entities.CorpseEntity;

import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;

import java.util.List;
import java.util.UUID;

/**
 * Corpse 模组遗体查找
 */
public final class CorpseGraveIntegration {

    private CorpseGraveIntegration() {
    }

    /**
     * 在指定坐标附近搜索属于该玩家的遗体
     */
    public static void findNear(SafeWorldCoordinate center, int radius, UUID playerUuid, List<SafeWorldCoordinate> out) {
        // try {
        //
        //     ResourceKey<Level> dim = center.dimension();
        //     ServerLevel world = DimensionUtils.getLevel(dim);
        //     if (world == null) return;
        //
        //     double cx = center.x(), cy = center.y(), cz = center.z();
        //     double r2 = (double) radius * radius;
        //
        //     for (Entity entity : world.getAllEntities()) {
        //         if (entity instanceof CorpseEntity corpse) {
        //             if (corpse.getCorpseUUID().filter(playerUuid::equals).isPresent()) {
        //                 double dx = entity.getX() - cx, dy = entity.getY() - cy, dz = entity.getZ() - cz;
        //                 if (dx * dx + dy * dy + dz * dz <= r2) {
        //                     out.add(new SafeWorldCoordinate(entity.getX(), entity.getY(), entity.getZ(), world.dimension()));
        //                 }
        //             }
        //         }
        //     }
        // } catch (Throwable ignored) {
        // }
    }

    public static SafeWorldCoordinate parseObituary(ServerPlayer player) {
        return null;
    }
}
