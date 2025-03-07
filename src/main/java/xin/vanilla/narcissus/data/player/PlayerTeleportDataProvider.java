package xin.vanilla.narcissus.data.player;

import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 玩家传送数据提供者类，实现了ICapabilityProvider和INBTSerializable接口，
 * 用于管理和序列化玩家的传送数据
 */
public class PlayerTeleportDataProvider implements ICapabilityProvider {

    private final IPlayerTeleportData instance = PlayerTeleportDataCapability.PLAYER_DATA.getDefaultInstance();

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == PlayerTeleportDataCapability.PLAYER_DATA;
    }

    @Override
    @Nullable
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        return capability == PlayerTeleportDataCapability.PLAYER_DATA ? PlayerTeleportDataCapability.PLAYER_DATA.cast(instance) : null;
    }
}
