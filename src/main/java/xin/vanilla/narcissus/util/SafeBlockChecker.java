package xin.vanilla.narcissus.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.data.SafeBlock;

import java.util.HashMap;
import java.util.Map;

public class SafeBlockChecker {
    private final Level level;
    private final Entity entity;
    private final SafeBlock safeBlock;

    public SafeBlockChecker(Level level, Entity entity) {
        this.level = level;
        this.entity = entity;
        safeBlock = NarcissusFarewell.getSafeBlock();
        safeBlock.init();
    }

    private final Map<BlockPos, BlockState> blockStateCaches = new HashMap<>();
    private final Map<BlockPos, BlockState> fluidStateCaches = new HashMap<>();

    /**
     * 判断指定坐标是否安全
     */
    public boolean isSafeBlock(BlockPos pos, boolean belowAllowAir) {

        // 可穿过判断
        BlockState block = getCachedBlockState(pos);
        BlockState fluid = getCachedFluidLegacyState(pos);
        boolean isCurrentPassable = !block.isCollisionShapeFullBlock(level, pos)
                && !safeBlock.getUnsafeBlocksState().contains(block)
                && !safeBlock.getUnsafeBlocks().contains(block.getBlock())

                && !fluid.isCollisionShapeFullBlock(level, pos)
                && !safeBlock.getUnsafeBlocksState().contains(fluid)
                && !safeBlock.getUnsafeBlocks().contains(fluid.getBlock());

        // 可呼吸判断
        BlockPos above = pos.above();
        BlockState blockAbove = getCachedBlockState(above);
        BlockState fluidAbove = getCachedFluidLegacyState(above);
        boolean isHeadSafe = !blockAbove.isSuffocating(level, above)
                && !blockAbove.isCollisionShapeFullBlock(level, above)
                && !safeBlock.getUnsafeBlocksState().contains(blockAbove)
                && !safeBlock.getUnsafeBlocks().contains(blockAbove.getBlock())
                && !safeBlock.getSuffocatingBlocksState().contains(blockAbove)
                && !safeBlock.getSuffocatingBlocks().contains(blockAbove.getBlock())

                && !fluidAbove.isSuffocating(level, above)
                && !safeBlock.getUnsafeBlocksState().contains(fluidAbove)
                && !safeBlock.getUnsafeBlocks().contains(fluidAbove.getBlock())
                && !safeBlock.getSuffocatingBlocksState().contains(fluidAbove)
                && !safeBlock.getSuffocatingBlocks().contains(fluidAbove.getBlock());

        // 可站立判断
        BlockPos below = pos.below();
        BlockState blockBelow = getCachedBlockState(below);
        BlockState fluidBelow = getCachedFluidLegacyState(below);
        boolean isBelowValid;
        if (!blockBelow.getFluidState().isEmpty()) {
            isBelowValid = !safeBlock.getUnsafeBlocksState().contains(blockBelow)
                    && !safeBlock.getUnsafeBlocks().contains(blockBelow.getBlock());
        } else {
            isBelowValid = blockBelow.entityCanStandOn(level, below, entity)
                    && !safeBlock.getUnsafeBlocksState().contains(blockBelow)
                    && !safeBlock.getUnsafeBlocks().contains(blockBelow.getBlock())

                    && !safeBlock.getUnsafeBlocksState().contains(fluidBelow)
                    && !safeBlock.getUnsafeBlocks().contains(fluidBelow.getBlock());
        }

        if (belowAllowAir) isBelowValid = isBelowValid || blockBelow.is(Blocks.AIR) || blockBelow.is(Blocks.CAVE_AIR);

        return isCurrentPassable && isHeadSafe && isBelowValid;
    }

    private BlockState getCachedBlockState(BlockPos pos) {
        return blockStateCaches.computeIfAbsent(pos, level::getBlockState);
    }

    private BlockState getCachedFluidLegacyState(BlockPos pos) {
        return fluidStateCaches.computeIfAbsent(pos, p -> getCachedBlockState(p).getFluidState().createLegacyBlock());
    }
}
