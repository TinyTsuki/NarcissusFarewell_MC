package xin.vanilla.narcissus.integration.grave;

import de.maxhenkel.corpse.entities.CorpseEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import xin.vanilla.narcissus.data.Coordinate;
import xin.vanilla.narcissus.util.NarcissusUtils;

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
    public static void findNear(Coordinate center, int radius, UUID playerUuid, List<Coordinate> out) {
        try {

            RegistryKey<World> dim = center.dimension();
            ServerWorld world = NarcissusUtils.getWorld(dim);
            if (world == null) return;

            double cx = center.x(), cy = center.y(), cz = center.z();
            double r2 = (double) radius * radius;

            for (Entity entity : world.getAllEntities()) {
                if (entity instanceof CorpseEntity) {
                    CorpseEntity corpse = (CorpseEntity) entity;
                    if (corpse.getCorpseUUID().filter(playerUuid::equals).isPresent()) {
                        double dx = entity.getX() - cx, dy = entity.getY() - cy, dz = entity.getZ() - cz;
                        if (dx * dx + dy * dy + dz * dz <= r2) {
                            out.add(new Coordinate(entity.getX(), entity.getY(), entity.getZ(), world.dimension()));
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static Coordinate parseObituary(ServerPlayerEntity player) {
        return null;
    }
}
