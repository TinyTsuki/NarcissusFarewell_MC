package xin.vanilla.narcissus.integration.grave;

// import com.lothrazar.simpletomb.block.BlockEntityTomb;
// import com.lothrazar.simpletomb.block.BlockTomb;
// import com.lothrazar.simpletomb.item.GraveKeyItem;

import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.narcissus.data.Coordinate;

import java.util.List;
import java.util.UUID;

/**
 * Simple Tomb 模组墓碑查找
 */
public final class SimpleTombGraveIntegration {

    private SimpleTombGraveIntegration() {
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
        //         if (world.getBlockState(pos).getBlock() instanceof BlockTomb) {
        //             BlockEntity te = world.getBlockEntity(pos);
        //             if (te instanceof BlockEntityTomb tomb) {
        //                 ServerPlayer player = world.getServer() != null
        //                         ? world.getServer().getPlayerList().getPlayer(playerUuid) : null;
        //                 if (player != null && tomb.isOwner(player)) {
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
        //     if (selected.getItem() instanceof GraveKeyItem item) {
        //         var pos = item.getTombPos(selected);
        //         if (pos != null && !pos.isOrigin()) {
        //             return new Coordinate(pos.getX(), pos.getY(), pos.getZ(), pos.getDimension());
        //         }
        //     }
        // } catch (Throwable ignored) {
        // }
        return null;
    }
}
