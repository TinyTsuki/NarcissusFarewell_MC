package xin.vanilla.narcissus.search;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import xin.vanilla.banira.common.util.BlockUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class SafeBlockPolicy {
    private final List<BlockState> supports;
    private final Set<BlockState> unsafeStates;
    private final Set<Block> unsafeBlocks;
    private final Set<BlockState> suffocatingStates;
    private final Set<Block> suffocatingBlocks;

    private SafeBlockPolicy(List<String> safe, List<String> unsafe, List<String> suffocating) {
        supports = Collections.unmodifiableList(new ArrayList<>(parse(safe)));
        unsafeStates = parse(unsafe);
        unsafeBlocks = blocks(unsafeStates);
        suffocatingStates = parse(suffocating);
        suffocatingBlocks = blocks(suffocatingStates);
    }

    public static SafeBlockPolicy from(List<String> safe, List<String> unsafe, List<String> suffocating) {
        return new SafeBlockPolicy(safe, unsafe, suffocating);
    }

    public List<BlockState> supportStates() {
        return supports;
    }

    public boolean unsafe(BlockState state) {
        return unsafeStates.contains(state) || unsafeBlocks.contains(state.getBlock());
    }

    public boolean suffocating(BlockState state) {
        return suffocatingStates.contains(state) || suffocatingBlocks.contains(state.getBlock());
    }

    private static Set<BlockState> parse(List<String> entries) {
        Set<BlockState> states = new LinkedHashSet<>();
        for (String entry : Objects.requireNonNull(entries, "entries")) {
            BlockState state = BlockUtils.deserializeBlockState(entry);
            if (state != null) states.add(state);
        }
        return Collections.unmodifiableSet(states);
    }

    private static Set<Block> blocks(Set<BlockState> states) {
        Set<Block> result = new HashSet<>();
        for (BlockState state : states) result.add(state.getBlock());
        return Collections.unmodifiableSet(result);
    }
}
