package xin.vanilla.narcissus.data.player;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;
import xin.vanilla.narcissus.config.Coordinate;
import xin.vanilla.narcissus.config.KeyValue;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.util.DateUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        // 序列化家坐标
        ListNBT homeCoordinateNBT = new ListNBT();
        for (Map.Entry<KeyValue<String, String>, Coordinate> entry : instance.getHomeCoordinate().entrySet()) {
            CompoundNBT homeCoordinateTag = new CompoundNBT();
            homeCoordinateTag.putString("key", entry.getKey().getKey());
            homeCoordinateTag.putString("value", entry.getKey().getValue());
            homeCoordinateTag.put("coordinate", entry.getValue().writeToNBT());
            homeCoordinateNBT.add(homeCoordinateTag);
        }
        tag.put("homeCoordinate", homeCoordinateNBT);
        // 序列化默认家
        ListNBT defaultHomeNBT = new ListNBT();
        for (Map.Entry<String, String> entry : instance.getDefaultHome().entrySet()) {
            CompoundNBT defaultHomeTag = new CompoundNBT();
            defaultHomeTag.putString("key", entry.getKey());
            defaultHomeTag.putString("value", entry.getValue());
            defaultHomeNBT.add(defaultHomeTag);
        }
        tag.put("defaultHome", defaultHomeNBT);
        tag.putBoolean("notified", instance.isNotified());
        tag.putString("language", instance.getLanguage());
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
            // 反序列化家坐标
            ListNBT homeCoordinateNBT = nbtTag.getList("homeCoordinate", 10);
            Map<KeyValue<String, String>, Coordinate> homeCoordinate = instance.getHomeCoordinate();
            for (int i = 0; i < homeCoordinateNBT.size(); i++) {
                CompoundNBT homeCoordinateTag = homeCoordinateNBT.getCompound(i);
                homeCoordinate.put(new KeyValue<>(homeCoordinateTag.getString("key"), homeCoordinateTag.getString("value")),
                        Coordinate.readFromNBT(homeCoordinateTag.getCompound("coordinate")));
            }
            instance.setHomeCoordinate(homeCoordinate);
            // 反序列化默认家
            ListNBT defaultHomeNBT = nbtTag.getList("defaultHome", 10);
            Map<String, String> defaultHome = instance.getDefaultHome();
            for (int i = 0; i < defaultHomeNBT.size(); i++) {
                CompoundNBT defaultHomeTag = defaultHomeNBT.getCompound(i);
                defaultHome.put(defaultHomeTag.getString("key"), defaultHomeTag.getString("value"));
            }
            instance.setDefaultHome(defaultHome);
            instance.setNotified(nbtTag.getBoolean("notified"));
            instance.setLanguage(nbtTag.getString("language"));
        }
    }
}
