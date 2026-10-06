package xin.vanilla.narcissus.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.config.CommonConfigView;
import xin.vanilla.narcissus.search.SafeBlockPolicy;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SafeBlockChecker {
    public interface BlockAccess {
        BlockState state(BlockPos pos);

        boolean suffocates(BlockState state, BlockPos pos);
    }

    private final BlockAccess access;
    private final SafeBlockPolicy policy;

    public SafeBlockChecker(Level level) {
        this(level, currentPolicy());
    }

    public SafeBlockChecker(Level level, SafeBlockPolicy policy) {
        this(new BlockAccess() {
            @Override
            public BlockState state(BlockPos pos) {
                return level.getBlockState(pos);
            }

            @Override
            public boolean suffocates(BlockState state, BlockPos pos) {
                return state.isSuffocating(level, pos);
            }
        }, policy);
    }

    public SafeBlockChecker(BlockAccess access, SafeBlockPolicy policy) {
        this.access = Objects.requireNonNull(access, "access");
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    private static SafeBlockPolicy currentPolicy() {
        CommonConfigView.BaseView.SafeTeleportView config = CommonConfig.get().base().safeTeleport();
        return SafeBlockPolicy.from(config.safeBlocks(), config.unsafeBlocks(), config.suffocatingBlocks());
    }

    public void beginSlice() {
        blockStateCaches.clear();
        fluidStateCaches.clear();
    }

    private final Map<BlockPos, BlockState> blockStateCaches = new HashMap<>();
    private final Map<BlockPos, BlockState> fluidStateCaches = new HashMap<>();

    /**
     * 判断指定坐标是否安全
     */
    public boolean isSafeBlock(BlockPos pos, boolean belowAllowAir) {

        BlockState block = getCachedBlockState(pos);
        if (block.getMaterial().blocksMotion() || policy.unsafe(block)) return false;
        BlockState fluid = getCachedFluidLegacyState(pos);
        if (fluid.getMaterial().blocksMotion() || policy.unsafe(fluid)) return false;

        BlockPos above = pos.above();
        BlockState blockAbove = getCachedBlockState(above);
        if (access.suffocates(blockAbove, above) || blockAbove.getMaterial().blocksMotion()
                || policy.unsafe(blockAbove) || policy.suffocating(blockAbove)) return false;
        BlockState fluidAbove = getCachedFluidLegacyState(above);
        if (access.suffocates(fluidAbove, above) || policy.unsafe(fluidAbove)
                || policy.suffocating(fluidAbove)) return false;

        BlockPos below = pos.below();
        BlockState blockBelow = getCachedBlockState(below);
        if (belowAllowAir && (blockBelow.is(Blocks.AIR) || blockBelow.is(Blocks.CAVE_AIR))) return true;
        if (blockBelow.getMaterial().isLiquid()) {
            return !policy.unsafe(blockBelow);
        }
        return blockBelow.getMaterial().isSolid() && !policy.unsafe(blockBelow)
                && !policy.unsafe(getCachedFluidLegacyState(below));
    }

    private BlockState getCachedBlockState(BlockPos pos) {
        BlockState cached = blockStateCaches.get(pos);
        return cached != null ? cached : blockStateCaches.computeIfAbsent(pos.immutable(), access::state);
    }

    private BlockState getCachedFluidLegacyState(BlockPos pos) {
        BlockState cached = fluidStateCaches.get(pos);
        return cached != null ? cached : fluidStateCaches.computeIfAbsent(pos.immutable(),
                p -> getCachedBlockState(p).getFluidState().createLegacyBlock());
    }
}
