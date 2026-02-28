package xin.vanilla.narcissus.integration.grave;

import de.maxhenkel.gravestone.blocks.GraveStoneBlock;
import de.maxhenkel.gravestone.corelib.death.Death;
import de.maxhenkel.gravestone.items.ObituaryItem;
import de.maxhenkel.gravestone.tileentity.GraveStoneTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import xin.vanilla.narcissus.data.Coordinate;
import xin.vanilla.narcissus.util.DimensionUtils;

import java.util.List;
import java.util.UUID;

/**
 * Gravestone-mod 墓碑查找
 */
public final class GravestoneModGraveIntegration {

    private GravestoneModGraveIntegration() {
    }

    /**
     * 在指定坐标附近搜索属于该玩家的墓碑
     */
    public static void findNear(Coordinate center, int radius, UUID playerUuid, List<Coordinate> out) {
        try {
            ResourceKey<Level> dim = center.dimension();
            ServerLevel world = DimensionUtils.getLevel(dim);
            if (world == null) return;

            BlockPos centerPos = center.toBlockPos();
            for (BlockPos pos : BlockPos.betweenClosed(
                    centerPos.offset(-radius, -radius, -radius),
                    centerPos.offset(radius, radius, radius))) {
                if (world.getBlockState(pos).getBlock() instanceof GraveStoneBlock) {
                    BlockEntity te = world.getBlockEntity(pos);
                    if (te instanceof GraveStoneTileEntity grave) {
                        if (playerUuid.equals(grave.getDeath().getPlayerUUID())) {
                            out.add(new Coordinate(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, world.dimension()));
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static Coordinate parseObituary(ServerPlayer player) {
        try {
            ItemStack selected = player.getInventory().getSelected();
            if (selected.getItem() instanceof ObituaryItem item) {
                Death death = item.fromStack(player, selected);
                if (death != null) {
                    return new Coordinate(death.getPosX(), death.getPosY(), death.getPosZ(), death.getDimension());
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
