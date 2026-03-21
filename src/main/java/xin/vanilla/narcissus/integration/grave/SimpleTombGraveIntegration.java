package xin.vanilla.narcissus.integration.grave;

import com.lothrazar.simpletomb.block.BlockEntityTomb;
import com.lothrazar.simpletomb.block.BlockTomb;
import com.lothrazar.simpletomb.data.DeathHelper;
import com.lothrazar.simpletomb.item.GraveKeyItem;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
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
            ResourceKey<Level> dim = center.dimension();
            ServerLevel world = DimensionUtils.getLevel(dim);
            if (world == null) return;

            BlockPos centerPos = center.toBlockPos();
            for (BlockPos pos : BlockPos.betweenClosed(
                    centerPos.offset(-radius, -radius, -radius),
                    centerPos.offset(radius, radius, radius))) {
                if (world.getBlockState(pos).getBlock() instanceof BlockTomb) {
                    BlockEntity te = world.getBlockEntity(pos);
                    if (te instanceof BlockEntityTomb tomb) {
                        ServerPlayer player = world.getServer() != null
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

    public static SafeWorldCoordinate parseObituary(ServerPlayer player) {
        try {
            ItemStack selected = player.getInventory().getSelected();
            if (selected.getItem() instanceof GraveKeyItem item) {
                var pos = item.getTombPos(selected);
                if (pos != null && pos != DeathHelper.ORIGIN) {
                    return new SafeWorldCoordinate(pos.pos().getX(), pos.pos().getY(), pos.pos().getZ(), pos.dimension());
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
