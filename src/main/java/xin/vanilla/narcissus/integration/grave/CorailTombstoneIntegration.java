package xin.vanilla.narcissus.integration.grave;

import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.narcissus.data.Coordinate;

import java.util.List;
import java.util.UUID;

public final class CorailTombstoneIntegration {

    private CorailTombstoneIntegration() {
    }

    /**
     * 在指定坐标附近搜索属于该玩家的墓碑
     */
    public static void findNear(Coordinate center, int radius, UUID playerUuid, List<Coordinate> out) {
        // try {
        //     ResourceKey<Level> dim = center.dimension();
        //     ServerLevel world = DimensionUtils.getLevel(dim);
        //     if (world == null) return;
        //
        //     BlockPos centerPos = center.toBlockPos();
        //     for (BlockPos pos : BlockPos.betweenClosed(
        //             centerPos.offset(-radius, -radius, -radius),
        //             centerPos.offset(radius, radius, radius))) {
        //         if (world.getBlockState(pos).getBlock() instanceof BlockGrave) {
        //             BlockEntity te = world.getBlockEntity(pos);
        //             if (te instanceof BlockEntityPlayerGrave grave) {
        //                 if (playerUuid.equals(grave.getOwnerId())) {
        //                     out.add(new Coordinate(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, world.dimension()));
        //                 }
        //             }
        //         }
        //     }
        // } catch (Throwable ignored) {
        // }
    }

    public static Coordinate parseObituary(ServerPlayer player) {
        // try {
        //     ItemStack selected = player.getInventory().getSelected();
        //     if (selected.getItem() instanceof ItemGraveKey item) {
        //         Location pos = item.getTombPos(selected);
        //         if (!pos.isOrigin()) {
        //             return new Coordinate(pos.x, pos.y, pos.z, pos.dim);
        //         }
        //     }
        // } catch (Throwable ignored) {
        // }
        return null;
    }
}
