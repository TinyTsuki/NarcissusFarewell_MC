package xin.vanilla.narcissus.data.player;

import lombok.NonNull;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import xin.vanilla.narcissus.BuildConfig;
import xin.vanilla.narcissus.config.Coordinate;
import xin.vanilla.narcissus.config.KeyValue;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.enums.ETeleportType;
import xin.vanilla.narcissus.network.ModNetworkHandler;
import xin.vanilla.narcissus.network.PlayerDataSyncPacket;
import xin.vanilla.narcissus.util.CollectionUtils;
import xin.vanilla.narcissus.util.DateUtils;
import xin.vanilla.narcissus.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 玩家传送数据
 */
public class PlayerTeleportData implements IPlayerTeleportData {

    public static final String ID = "player_teleport_data";

    private EntityPlayer player;

    private Date lastCardTime;
    private Date lastTpTime;
    private final AtomicInteger teleportCard = new AtomicInteger();
    private List<TeleportRecord> teleportRecords;
    /**
     * dimension:name coordinate
     */
    private Map<KeyValue<String, String>, Coordinate> homeCoordinate;
    /**
     * dimension:name
     */
    private Map<String, String> defaultHome;

    @Override
    public int getTeleportCard() {
        return this.teleportCard.get();
    }

    @Override
    public int plusTeleportCard() {
        return this.teleportCard.incrementAndGet();
    }

    @Override
    public int plusTeleportCard(int num) {
        return this.teleportCard.addAndGet(num);
    }

    @Override
    public int subTeleportCard() {
        return this.teleportCard.decrementAndGet();
    }

    @Override
    public int subTeleportCard(int num) {
        return this.teleportCard.addAndGet(-num);
    }

    @Override
    public void setTeleportCard(int num) {
        this.teleportCard.set(num);
    }

    @Override
    public @NonNull Date getLastCardTime() {
        return this.lastCardTime = this.lastCardTime == null ? DateUtils.getDate(0, 1, 1) : this.lastCardTime;
    }

    @Override
    public void setLastCardTime(Date time) {
        this.lastCardTime = time;
    }

    @Override
    public @NonNull Date getLastTpTime() {
        return this.lastTpTime = this.lastTpTime == null ? DateUtils.getDate(0, 1, 1) : this.lastTpTime;
    }

    @Override
    public void setLastTpTime(Date time) {
        this.lastTpTime = time;
    }

    @Override
    public @NonNull List<TeleportRecord> getTeleportRecords() {
        return teleportRecords = CollectionUtils.isNullOrEmpty(teleportRecords) ? new ArrayList<>() : teleportRecords;
    }

    @Override
    public @NonNull List<TeleportRecord> getTeleportRecords(ETeleportType type) {
        return CollectionUtils.isNullOrEmpty(teleportRecords) ? teleportRecords = new ArrayList<>() :
                teleportRecords.stream().filter(record -> record.getTeleportType() == type).collect(Collectors.toList());
    }

    @Override
    public void setTeleportRecords(List<TeleportRecord> records) {
        this.teleportRecords = records;
    }

    @Override
    public void addTeleportRecords(TeleportRecord... records) {
        this.getTeleportRecords().addAll(Arrays.asList((records)));
        this.getTeleportRecords().sort(Comparator.comparing(TeleportRecord::getTeleportTime));
        int limit = ServerConfig.TELEPORT_RECORD_LIMIT;
        int size = this.getTeleportRecords().size();
        if (limit > 0 && limit < size) {
            this.getTeleportRecords().subList(0, size - limit).clear();
        }
    }

    @Override
    public Map<KeyValue<String, String>, Coordinate> getHomeCoordinate() {
        return homeCoordinate = homeCoordinate == null ? new HashMap<>() : homeCoordinate;
    }

    @Override
    public void setHomeCoordinate(Map<KeyValue<String, String>, Coordinate> homeCoordinate) {
        this.homeCoordinate = homeCoordinate;
    }

    @Override
    public void addHomeCoordinate(KeyValue<String, String> key, Coordinate coordinate) {
        this.getHomeCoordinate().put(key, coordinate);
    }

    @Override
    public Map<String, String> getDefaultHome() {
        return defaultHome = defaultHome == null ? new HashMap<>() : defaultHome;
    }

    @Override
    public void setDefaultHome(Map<String, String> defaultHome) {
        this.defaultHome = defaultHome;
    }

    @Override
    public void addDefaultHome(String key, String value) {
        this.getDefaultHome().put(key, value);
    }

    @Override
    public KeyValue<String, String> getDefaultHome(String dimension) {
        if (this.getDefaultHome().containsKey(dimension)) {
            return new KeyValue<>(dimension, this.getDefaultHome().get(dimension));
        }
        return null;
    }

    public void serializeNBT(NBTTagCompound tag) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("lastCardTime", DateUtils.toDateTimeString(this.getLastCardTime()));
        nbt.setString("lastTpTime", DateUtils.toDateTimeString(this.getLastTpTime()));
        nbt.setInteger("teleportCard", this.getTeleportCard());
        // 序列化传送记录
        NBTTagList recordsNBT = new NBTTagList();
        for (TeleportRecord record : this.getTeleportRecords()) {
            recordsNBT.appendTag(record.writeToNBT());
        }
        nbt.setTag("teleportRecords", recordsNBT);
        // 序列化家坐标
        NBTTagList homeCoordinateNBT = new NBTTagList();
        for (Map.Entry<KeyValue<String, String>, Coordinate> entry : this.getHomeCoordinate().entrySet()) {
            if (StringUtils.isNotNullOrEmpty(entry.getKey().getKey()) && StringUtils.isNotNullOrEmpty(entry.getKey().getValue())) {
                NBTTagCompound homeCoordinateTag = new NBTTagCompound();
                homeCoordinateTag.setString("key", entry.getKey().getKey());
                homeCoordinateTag.setString("value", entry.getKey().getValue());
                homeCoordinateTag.setTag("coordinate", entry.getValue().writeToNBT());
                homeCoordinateNBT.appendTag(homeCoordinateTag);
            }
        }
        nbt.setTag("homeCoordinate", homeCoordinateNBT);
        // 序列化默认家
        NBTTagList defaultHomeNBT = new NBTTagList();
        for (Map.Entry<String, String> entry : this.getDefaultHome().entrySet()) {
            NBTTagCompound defaultHomeTag = new NBTTagCompound();
            defaultHomeTag.setString("key", entry.getKey());
            defaultHomeTag.setString("value", entry.getValue());
            defaultHomeNBT.appendTag(defaultHomeTag);
        }
        nbt.setTag("defaultHome", defaultHomeNBT);
        tag.setTag(BuildConfig.MODID, nbt);
    }

    public void deserializeNBT(NBTTagCompound tag) {
        NBTTagCompound nbt = tag.getCompoundTag(BuildConfig.MODID);
        this.setLastCardTime(DateUtils.format(nbt.getString("lastCardTime")));
        this.setLastTpTime(DateUtils.format(nbt.getString("lastTpTime")));
        this.setTeleportCard(nbt.getInteger("teleportCard"));
        // 反序列化传送记录
        NBTTagList recordsNBT = nbt.getTagList("teleportRecords", 10); // 10 是 NBTTagCompound 的类型ID
        List<TeleportRecord> records = new ArrayList<>();
        for (int i = 0; i < recordsNBT.tagCount(); i++) {
            records.add(TeleportRecord.readFromNBT(recordsNBT.getCompoundTagAt(i)));
        }
        this.setTeleportRecords(records);
        // 反序列化家坐标
        NBTTagList homeCoordinateNBT = nbt.getTagList("homeCoordinate", 10);
        Map<KeyValue<String, String>, Coordinate> homeCoordinate = new HashMap<>();
        for (int i = 0; i < homeCoordinateNBT.tagCount(); i++) {
            NBTTagCompound homeCoordinateTag = homeCoordinateNBT.getCompoundTagAt(i);
            homeCoordinate.put(new KeyValue<>(homeCoordinateTag.getString("key"), homeCoordinateTag.getString("value")),
                    Coordinate.readFromNBT(homeCoordinateTag.getCompoundTag("coordinate")));
        }
        this.setHomeCoordinate(homeCoordinate);
        // 反序列化默认家
        NBTTagList defaultHomeNBT = nbt.getTagList("defaultHome", 10);
        Map<String, String> defaultHome = new HashMap<>();
        for (int i = 0; i < defaultHomeNBT.tagCount(); i++) {
            NBTTagCompound defaultHomeTag = defaultHomeNBT.getCompoundTagAt(i);
            defaultHome.put(defaultHomeTag.getString("key"), defaultHomeTag.getString("value"));
        }
        this.setDefaultHome(defaultHome);
    }

    // 初始化方法（Forge要求）
    @Override
    public void init(Entity entity, World world) {
        this.player = (EntityPlayer) entity;
    }

    @Override
    public void saveNBTData(NBTTagCompound compound) {

    }

    @Override
    public void loadNBTData(NBTTagCompound compound) {

    }

    public void writeToBuffer(PacketBuffer buffer) throws IOException {
        buffer.writeStringToBuffer(DateUtils.toDateTimeString(this.getLastCardTime()));
        buffer.writeStringToBuffer(DateUtils.toDateTimeString(this.getLastTpTime()));
        buffer.writeInt(this.getTeleportCard());
        buffer.writeInt(this.teleportRecords.size());
        for (TeleportRecord teleportRecord : this.getTeleportRecords()) {
            buffer.writeNBTTagCompoundToBuffer(teleportRecord.writeToNBT());
        }
        buffer.writeInt(this.getHomeCoordinate().size());
        for (Map.Entry<KeyValue<String, String>, Coordinate> entry : this.getHomeCoordinate().entrySet()) {
            buffer.writeStringToBuffer(entry.getKey().getKey());
            buffer.writeStringToBuffer(entry.getKey().getValue());
            buffer.writeNBTTagCompoundToBuffer(entry.getValue().writeToNBT());
        }
        buffer.writeInt(this.getDefaultHome().size());
        for (Map.Entry<String, String> entry : this.getDefaultHome().entrySet()) {
            buffer.writeStringToBuffer(entry.getKey());
            buffer.writeStringToBuffer(entry.getValue());
        }
    }

    public void readFromBuffer(PacketBuffer buffer) throws IOException {
        this.lastCardTime = DateUtils.format(buffer.readStringFromBuffer(32));
        this.lastTpTime = DateUtils.format(buffer.readStringFromBuffer(32));
        this.teleportCard.set(buffer.readInt());
        this.teleportRecords = new ArrayList<>();
        for (int i = 0; i < buffer.readInt(); i++) {
            try {
                this.teleportRecords.add(TeleportRecord.readFromNBT(Objects.requireNonNull(buffer.readNBTTagCompoundFromBuffer())));
            } catch (IOException ignored) {
            }
        }
        this.homeCoordinate = new HashMap<>();
        for (int i = 0; i < buffer.readInt(); i++) {
            try {
                this.homeCoordinate.put(new KeyValue<>(buffer.readStringFromBuffer(32), buffer.readStringFromBuffer(32)), Coordinate.readFromNBT(Objects.requireNonNull(buffer.readNBTTagCompoundFromBuffer())));
            } catch (IOException ignored) {
            }
        }
        this.defaultHome = new HashMap<>();
        for (int i = 0; i < buffer.readInt(); i++) {
            this.defaultHome.put(buffer.readStringFromBuffer(32), buffer.readStringFromBuffer(32));
        }
    }

    public void copyFrom(IPlayerTeleportData capability) {
        this.lastCardTime = capability.getLastCardTime();
        this.lastTpTime = capability.getLastTpTime();
        this.teleportCard.set(capability.getTeleportCard());
        this.teleportRecords = capability.getTeleportRecords();
        this.homeCoordinate = capability.getHomeCoordinate();
        this.defaultHome = capability.getDefaultHome();
    }

    public static IPlayerTeleportData get(EntityPlayer player) {
        return (IPlayerTeleportData) player.getExtendedProperties(ID);
    }

    /**
     * 同步玩家传送数据到客户端
     */
    public static void syncPlayerData(EntityPlayerMP player) {
        // 创建自定义包并发送到客户端
        PlayerDataSyncPacket packet = new PlayerDataSyncPacket(player.getUniqueID(), get(player));
        for (PlayerDataSyncPacket syncPacket : packet.split()) {
            ModNetworkHandler.INSTANCE.sendTo(syncPacket, player);
        }
    }
}
