package xin.vanilla.narcissus.integration.grave;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import ovh.corail.tombstone.block.BlockGrave;
import ovh.corail.tombstone.helper.Location;
import ovh.corail.tombstone.item.ItemGraveKey;
import ovh.corail.tombstone.tileentity.TileEntityPlayerGrave;
import xin.vanilla.banira.common.util.DimensionUtils;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;

import java.util.List;
import java.util.UUID;

public final class CorailTombstoneIntegration {

    private CorailTombstoneIntegration() {
    }

    /**
     * 在指定坐标附近搜索属于该玩家的墓碑
     */
    public static void findNear(SafeWorldCoordinate center, int radius, UUID playerUuid, List<SafeWorldCoordinate> out) {
        try {
            RegistryKey<World> dim = center.dimension();
            ServerWorld world = DimensionUtils.getLevel(dim);
            if (world == null) return;

            BlockPos centerPos = center.toBlockPos();
            for (BlockPos pos : BlockPos.betweenClosed(
                    centerPos.offset(-radius, -radius, -radius),
                    centerPos.offset(radius, radius, radius))) {
                if (world.getBlockState(pos).getBlock() instanceof BlockGrave) {
                    TileEntity te = world.getBlockEntity(pos);
                    if (te instanceof TileEntityPlayerGrave) {
                        TileEntityPlayerGrave grave = (TileEntityPlayerGrave) te;
                        if (playerUuid.equals(grave.getOwnerId())) {
                            out.add(new SafeWorldCoordinate(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, world.dimension()));
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static SafeWorldCoordinate parseObituary(ServerPlayerEntity player) {
        try {
            ItemStack selected = player.inventory.getSelected();
            if (selected.getItem() instanceof ItemGraveKey) {
                ItemGraveKey item = (ItemGraveKey) selected.getItem();
                Location pos = item.getTombPos(selected);
                if (!pos.isOrigin()) {
                    return new SafeWorldCoordinate(pos.x, pos.y, pos.z, pos.dim);
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
