package xin.vanilla.narcissus.data.player;

import lombok.NonNull;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.IExtendedEntityProperties;
import xin.vanilla.narcissus.config.Coordinate;
import xin.vanilla.narcissus.config.KeyValue;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.enums.ETeleportType;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 玩家传送数据
 */
public interface IPlayerTeleportData extends IExtendedEntityProperties {

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

    /**
     * 获取语言
     */
    String getLanguage();

    /**
     * 设置语言
     */
    void setLanguage(String language);

    /**
     * 获取有效的语言
     */
    @NonNull
    String getValidLanguage(@Nullable EntityPlayer player);

    void addDefaultHome(String key, String value);

    KeyValue<String, String> getDefaultHome(String key);

    boolean isNotified();

    void setNotified(boolean notified);

    void writeToBuffer(PacketBuffer buffer) throws IOException;

    void readFromBuffer(PacketBuffer buffer) throws IOException;

    void copyFrom(IPlayerTeleportData capability);

    void deserializeNBT(NBTTagCompound entityData);

    void serializeNBT(NBTTagCompound entityData);
}
