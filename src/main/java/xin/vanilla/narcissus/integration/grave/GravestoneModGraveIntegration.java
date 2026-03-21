package xin.vanilla.narcissus.integration.grave;

import de.maxhenkel.gravestone.blocks.GraveStoneBlock;
import de.maxhenkel.gravestone.corelib.death.Death;
import de.maxhenkel.gravestone.items.ObituaryItem;
import de.maxhenkel.gravestone.tileentity.GraveStoneTileEntity;
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
 * Gravestone-mod 墓碑查找
 */
public final class GravestoneModGraveIntegration {

    private GravestoneModGraveIntegration() {
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
                if (world.getBlockState(pos).getBlock() instanceof GraveStoneBlock) {
                    TileEntity te = world.getBlockEntity(pos);
                    if (te instanceof GraveStoneTileEntity) {
                        GraveStoneTileEntity grave = (GraveStoneTileEntity) te;
                        if (playerUuid.equals(grave.getDeath().getPlayerUUID())) {
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
            if (selected.getItem() instanceof ObituaryItem) {
                ObituaryItem item = (ObituaryItem) selected.getItem();
                Death death = item.fromStack(player, selected);
                if (death != null) {
                    return new SafeWorldCoordinate(death.getPosX(), death.getPosY(), death.getPosZ(), death.getDimension());
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
