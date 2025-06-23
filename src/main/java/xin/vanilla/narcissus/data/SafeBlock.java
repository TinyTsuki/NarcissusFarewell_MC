package xin.vanilla.narcissus.data;

import lombok.Data;
import lombok.experimental.Accessors;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
@Accessors(chain = true)
public class SafeBlock {
    /**
     * 安全的方块
     */
    private List<BlockState> safeBlocksState;
    /**
     * 安全的方块
     */
    private List<Block> safeBlocks;
    /**
     * 不安全的方块
     */
    private List<BlockState> unsafeBlocksState;
    /**
     * 不安全的方块
     */
    private List<Block> unsafeBlocks;
    /**
     * 窒息的方块
     */
    private List<BlockState> suffocatingBlocksState;
    /**
     * 窒息的方块
     */
    private List<Block> suffocatingBlocks;

    public void init() {
        if (this.safeBlocksState == null) {
            this.safeBlocksState = ServerConfig.SAFE_BLOCKS.get().stream()
                    .map(NarcissusUtils::deserializeBlockState)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
        }
        if (this.safeBlocks == null) {
            this.safeBlocks = safeBlocksState.stream()
                    .filter(Objects::nonNull)
                    .map(BlockState::getBlock)
                    .distinct()
                    .collect(Collectors.toList());
        }
        if (this.unsafeBlocksState == null) {
            this.unsafeBlocksState = ServerConfig.UNSAFE_BLOCKS.get().stream()
                    .map(NarcissusUtils::deserializeBlockState)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
        }
        if (this.unsafeBlocks == null) {
            this.unsafeBlocks = unsafeBlocksState.stream()
                    .filter(Objects::nonNull)
                    .map(BlockState::getBlock)
                    .distinct()
                    .collect(Collectors.toList());
        }
        if (this.suffocatingBlocksState == null) {
            this.suffocatingBlocksState = ServerConfig.SUFFOCATING_BLOCKS.get().stream()
                    .map(NarcissusUtils::deserializeBlockState)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
        }
        if (this.suffocatingBlocks == null) {
            this.suffocatingBlocks = suffocatingBlocksState.stream()
                    .filter(Objects::nonNull)
                    .map(BlockState::getBlock)
                    .distinct()
                    .collect(Collectors.toList());
        }
    }
}
