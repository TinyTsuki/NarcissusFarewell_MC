package xin.vanilla.narcissus.data.player;

import lombok.NonNull;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.INBTSerializable;
import xin.vanilla.narcissus.config.Coordinate;
import xin.vanilla.narcissus.config.KeyValue;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.enums.ETeleportType;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 玩家传送数据
 */
public interface IPlayerTeleportData extends INBTSerializable<NBTTagCompound> {
    // TIPS 加完属性记得去 PlayerTeleportDataStorage 里注册

    /**
     * 获取传送卡数量
     */
    int getTeleportCard();

    /**
     * 增加传送卡数量
     */
    int plusTeleportCard();

    /**
     * 增加传送卡数量
     */
    int plusTeleportCard(int num);

    /**
     * 减少传送卡数量
     */
    int subTeleportCard();

    /**
     * 减少传送卡数量
     */
    int subTeleportCard(int num);

    /**
     * 设置传送卡数量
     */
    void setTeleportCard(int num);

    /**
     * 获取最后领取传送卡时间
     */
    Date getLastCardTime();

    /**
     * 设置最后领取传送卡时间
     */
    void setLastCardTime(Date time);

    /**
     * 获取最后传送时间
     */
    Date getLastTpTime();

    /**
     * 设置最后传送时间
     */
    void setLastTpTime(Date time);

    /**
     * 获取传送记录
     */
    @NonNull
    List<TeleportRecord> getTeleportRecords();

    /**
     * 获取传送记录
     */
    @NonNull
    List<TeleportRecord> getTeleportRecords(ETeleportType type);

    /**
     * 设置传送记录
     */
    void setTeleportRecords(List<TeleportRecord> records);

    /**
     * 添加传送记录
     */
    void addTeleportRecords(TeleportRecord... records);

    /**
     * dimension:name coordinate
     */
    Map<KeyValue<String, String>, Coordinate> getHomeCoordinate();

    void setHomeCoordinate(Map<KeyValue<String, String>, Coordinate> homeCoordinate);

    void addHomeCoordinate(KeyValue<String, String> key, Coordinate coordinate);

    /**
     * dimension:name
     */
    Map<String, String> getDefaultHome();

    void setDefaultHome(Map<String, String> defaultHome);

    void addDefaultHome(String key, String value);

    KeyValue<String, String> getDefaultHome(String key);

    void writeToBuffer(PacketBuffer buffer);

    void readFromBuffer(PacketBuffer buffer);

    void copyFrom(IPlayerTeleportData capability);

    void save(EntityPlayerMP player);
}
