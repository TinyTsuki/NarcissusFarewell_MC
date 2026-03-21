package xin.vanilla.narcissus.integration.grave;

import com.lothrazar.simpletomb.block.BlockTomb;
import com.lothrazar.simpletomb.block.TileEntityTomb;
import com.lothrazar.simpletomb.data.LocationBlockPos;
import com.lothrazar.simpletomb.item.GraveKeyItem;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import xin.vanilla.banira.common.util.DimensionUtils;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;

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
    public static void findNear(SafeWorldCoordinate center, int radius, UUID playerUuid, List<SafeWorldCoordinate> out) {
        try {
            RegistryKey<World> dim = center.dimension();
            ServerWorld world = DimensionUtils.getLevel(dim);
            if (world == null) return;

            BlockPos centerPos = center.toBlockPos();
            for (BlockPos pos : BlockPos.betweenClosed(
                    centerPos.offset(-radius, -radius, -radius),
                    centerPos.offset(radius, radius, radius))) {
                if (world.getBlockState(pos).getBlock() instanceof BlockTomb) {
                    TileEntity te = world.getBlockEntity(pos);
                    if (te instanceof TileEntityTomb) {
                        TileEntityTomb tomb = (TileEntityTomb) te;
                        ServerPlayerEntity player = world.getServer() != null
                                ? world.getServer().getPlayerList().getPlayer(playerUuid) : null;
                        if (player != null && tomb.isOwner(player)) {
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
            if (selected.getItem() instanceof GraveKeyItem) {
                GraveKeyItem item = (GraveKeyItem) selected.getItem();
                LocationBlockPos pos = item.getTombPos(selected);
                if (pos != null && !pos.isOrigin()) {
                    return new SafeWorldCoordinate(pos.x, pos.y, pos.z, pos.dim);
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
