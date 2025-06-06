package xin.vanilla.narcissus.data.player;

import lombok.NonNull;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;

/**
 * 玩家数据提供者类
 * 用于管理和序列化玩家的数据
 */
public class PlayerTeleportDataProvider implements ICapabilityProvider, INBTSerializable<CompoundNBT> {

    /**
     * 玩家数据实例
     */
    private IPlayerTeleportData playerData;
    private final LazyOptional<IPlayerTeleportData> instance = LazyOptional.of(this::getOrCreateCapability);

    /**
     * 获取指定能力的实例
     *
     * @param cap  能力实例
     * @param side 方向，可为空
     * @param <T>  泛型类型，表示能力的类型
     */
    @NonNull
    @Override
    public <T> LazyOptional<T> getCapability(@NonNull Capability<T> cap, @Nullable Direction side) {
        return cap == PlayerTeleportDataCapability.PLAYER_DATA ? instance.cast() : LazyOptional.empty();
    }

    @NonNull
    IPlayerTeleportData getOrCreateCapability() {
        if (playerData == null) {
            this.playerData = new PlayerTeleportData();
        }
        return this.playerData;
    }

    /**
     * 序列化玩家数据为NBT格式
     */
    @Override
    public CompoundNBT serializeNBT() {
        return this.getOrCreateCapability().serializeNBT();
    }

    /**
     * 从NBT格式的数据中反序列化玩家数据
     */
    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        this.getOrCreateCapability().deserializeNBT(nbt);
    }
}
