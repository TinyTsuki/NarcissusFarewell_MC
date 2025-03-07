package xin.vanilla.narcissus.data.player;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 玩家传送数据提供者类，实现了ICapabilityProvider和INBTSerializable接口，
 * 用于管理和序列化玩家的传送数据
 */
public class PlayerTeleportDataProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    // 玩家传送数据实例，使用PlayerTeleportData类进行管理
    private IPlayerTeleportData playerData;
    private final LazyOptional<IPlayerTeleportData> instance = LazyOptional.of(this::getOrCreateCapability);

    /**
     * 获取指定能力的实例
     *
     * @param cap  要获取的能力实例
     * @param side 方向，可为空
     * @param <T>  泛型类型，表示能力的类型
     * @return 返回包含指定能力实例的LazyOptional对象，如果指定的能力不匹配，则返回空的LazyOptional
     * <p>
     * 该方法用于能力系统的交互，只有当请求的能力类型为PlayerTeleportDataCapability.PLAYER_DATA时，
     * 才会返回相应的实例，否则返回空
     */
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == PlayerTeleportDataCapability.PLAYER_DATA ? instance.cast() : LazyOptional.empty();
    }

    @Nonnull
    IPlayerTeleportData getOrCreateCapability() {
        if (playerData == null) {
            this.playerData = new PlayerTeleportData();
        }
        return this.playerData;
    }

    /**
     * 序列化玩家传送数据为NBT格式
     *
     * @return 返回包含玩家传送数据的CompoundTag对象
     * <p>
     * 该方法实现了玩家传送数据的序列化，返回的数据可以用于存储或传输
     */
    @Override
    public CompoundTag serializeNBT() {
        return this.getOrCreateCapability().serializeNBT();
    }

    /**
     * 从NBT格式的数据中反序列化玩家传送数据
     *
     * @param nbt 包含玩家传送数据的CompoundTag对象
     *            <p>
     *            该方法实现了玩家传送数据的反序列化，从提供的NBT数据中恢复玩家传送信息
     */
    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.getOrCreateCapability().deserializeNBT(nbt);
    }
}
