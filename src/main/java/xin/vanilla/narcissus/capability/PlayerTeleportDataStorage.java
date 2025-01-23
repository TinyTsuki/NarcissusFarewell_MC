package xin.vanilla.narcissus.capability;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;
import xin.vanilla.narcissus.util.DateUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家传送数据存储类，实现了IStorage接口，用于对玩家传送数据(IPlayerTeleportData)的读写操作
 */
public class PlayerTeleportDataStorage implements IStorage<IPlayerTeleportData> {

    /**
     * 将玩家传送数据写入NBT标签
     *
     * @param capability 用于存储玩家传送数据的能力对象
     * @param instance   玩家传送数据实例
     * @param side       侧边标识，用于指定数据交换的方向
     * @return 返回包含玩家传送数据的CompoundNBT对象如果实例为null，则返回一个空的CompoundNBT对象
     */
    @Override
    public CompoundNBT writeNBT(Capability<IPlayerTeleportData> capability, IPlayerTeleportData instance, Direction side) {
        // 检查instance是否为null，如果是，则返回一个空的CompoundNBT对象，避免后续操作出错
        if (instance == null) {
            return new CompoundNBT();
        }
        CompoundNBT tag = new CompoundNBT();
        tag.putString("lastCardTime", DateUtils.toDateTimeString(instance.getLastCardTime()));
        tag.putString("lastTpTime", DateUtils.toDateTimeString(instance.getLastTpTime()));
        tag.putInt("teleportCard", instance.getTeleportCard());
        // 序列化传送记录
        ListNBT recordsNBT = new ListNBT();
        for (TeleportRecord record : instance.getTeleportRecords()) {
            recordsNBT.add(record.writeToNBT());
        }
        tag.put("teleportRecords", recordsNBT);
        return tag;
    }

    /**
     * 从NBT标签读取玩家传送数据
     *
     * @param capability 用于存储玩家传送数据的能力对象
     * @param instance   玩家传送数据实例
     * @param side       侧边标识，用于指定数据交换的方向
     * @param nbt        包含玩家传送数据的NBT标签
     */
    @Override
    public void readNBT(Capability<IPlayerTeleportData> capability, IPlayerTeleportData instance, Direction side, INBT nbt) {
        // 检查nbt是否为CompoundNBT实例，如果不是，则不进行操作
        if (nbt instanceof CompoundNBT) {
            CompoundNBT nbtTag = (CompoundNBT) nbt;
            instance.setLastCardTime(DateUtils.format(nbtTag.getString("lastCardTime")));
            instance.setLastTpTime(DateUtils.format(nbtTag.getString("lastTpTime")));
            instance.setTeleportCard(nbtTag.getInt("teleportCard"));
            // 反序列化传送记录
            ListNBT recordsNBT = nbtTag.getList("teleportRecords", 10); // 10 是 CompoundNBT 的类型ID
            List<TeleportRecord> records = new ArrayList<>();
            for (int i = 0; i < recordsNBT.size(); i++) {
                records.add(TeleportRecord.readFromNBT(recordsNBT.getCompound(i)));
            }
            instance.setTeleportRecords(records);
        }
    }
}
