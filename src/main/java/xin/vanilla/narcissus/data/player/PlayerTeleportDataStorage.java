package xin.vanilla.narcissus.data.player;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

/**
 * 玩家传送数据存储类，实现了IStorage接口，用于对玩家传送数据(IPlayerTeleportData)的读写操作
 */
public class PlayerTeleportDataStorage implements Capability.IStorage<IPlayerTeleportData> {
    @Override
    public NBTTagCompound writeNBT(Capability<IPlayerTeleportData> capability, IPlayerTeleportData instance, EnumFacing side) {
        return instance.serializeNBT(); // 直接调用实现类的序列化方法
    }

    @Override
    public void readNBT(Capability<IPlayerTeleportData> capability, IPlayerTeleportData instance, EnumFacing side, NBTBase nbt) {
        instance.deserializeNBT((NBTTagCompound) nbt); // 直接调用实现类的反序列化方法
    }
}
