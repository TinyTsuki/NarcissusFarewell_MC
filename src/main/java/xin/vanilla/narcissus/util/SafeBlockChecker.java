package xin.vanilla.narcissus.util;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.data.SafeBlock;

import java.util.HashMap;
import java.util.Map;

public class SafeBlockChecker {
    private final World level;
    private final SafeBlock safeBlock;

    public SafeBlockChecker(World level) {
        this.level = level;
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
        boolean isCurrentPassable = !block.getMaterial().blocksMotion()
                && !safeBlock.getUnsafeBlocksState().contains(block)
                && !safeBlock.getUnsafeBlocks().contains(block.getBlock())

                && !fluid.getMaterial().blocksMotion()
                && !safeBlock.getUnsafeBlocksState().contains(fluid)
                && !safeBlock.getUnsafeBlocks().contains(fluid.getBlock());

        // 可呼吸判断
        BlockPos above = pos.above();
        BlockState blockAbove = getCachedBlockState(above);
        BlockState fluidAbove = getCachedFluidLegacyState(above);
        boolean isHeadSafe = !blockAbove.isSuffocating(level, above)
                && !blockAbove.getMaterial().blocksMotion()
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
        if (blockBelow.getMaterial().isLiquid()) {
            isBelowValid = !safeBlock.getUnsafeBlocksState().contains(blockBelow)
                    && !safeBlock.getUnsafeBlocks().contains(blockBelow.getBlock());
        } else {
            isBelowValid = blockBelow.getMaterial().isSolid()
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
