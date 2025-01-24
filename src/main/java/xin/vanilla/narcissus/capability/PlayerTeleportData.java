package xin.vanilla.narcissus.capability;

import lombok.NonNull;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.PacketBuffer;
import xin.vanilla.narcissus.config.Coordinate;
import xin.vanilla.narcissus.config.KeyValue;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.enums.ETeleportType;
import xin.vanilla.narcissus.util.CollectionUtils;
import xin.vanilla.narcissus.util.DateUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 玩家传送数据
 */
public class PlayerTeleportData implements IPlayerTeleportData {
    private Date lastCardTime;
    private Date lastTpTime;
    private final AtomicInteger teleportCard = new AtomicInteger();
    private List<TeleportRecord> teleportRecords;
    // dimension:name coordinate
    private Map<KeyValue<String, String>, Coordinate> homeCoordinate;
    // dimension:name
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
        return teleportRecords = CollectionUtils.isNullOrEmpty(teleportRecords) ? new ArrayList<>() :
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
        int limit = ServerConfig.TELEPORT_RECORD_LIMIT.get();
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

    public void writeToBuffer(PacketBuffer buffer) {
        buffer.writeUtf(DateUtils.toDateTimeString(this.getLastCardTime()));
        buffer.writeUtf(DateUtils.toDateTimeString(this.getLastTpTime()));
        buffer.writeInt(this.getTeleportCard());
        buffer.writeInt(this.teleportRecords.size());
        for (TeleportRecord teleportRecord : this.getTeleportRecords()) {
            buffer.writeNbt(teleportRecord.writeToNBT());
        }
        buffer.writeInt(this.getHomeCoordinate().size());
        for (Map.Entry<KeyValue<String, String>, Coordinate> entry : this.getHomeCoordinate().entrySet()) {
            buffer.writeUtf(entry.getKey().getKey());
            buffer.writeUtf(entry.getKey().getValue());
            buffer.writeNbt(entry.getValue().writeToNBT());
        }
        buffer.writeInt(this.getDefaultHome().size());
        for (Map.Entry<String, String> entry : this.getDefaultHome().entrySet()) {
            buffer.writeUtf(entry.getKey());
            buffer.writeUtf(entry.getValue());
        }
    }

    public void readFromBuffer(PacketBuffer buffer) {
        this.lastCardTime = DateUtils.format(buffer.readUtf());
        this.lastTpTime = DateUtils.format(buffer.readUtf());
        this.teleportCard.set(buffer.readInt());
        this.teleportRecords = new ArrayList<>();
        for (int i = 0; i < buffer.readInt(); i++) {
            this.teleportRecords.add(TeleportRecord.readFromNBT(Objects.requireNonNull(buffer.readNbt())));
        }
        this.homeCoordinate = new HashMap<>();
        for (int i = 0; i < buffer.readInt(); i++) {
            this.homeCoordinate.put(new KeyValue<>(buffer.readUtf(), buffer.readUtf()), Coordinate.readFromNBT(Objects.requireNonNull(buffer.readNbt())));
        }
        this.defaultHome = new HashMap<>();
        for (int i = 0; i < buffer.readInt(); i++) {
            this.defaultHome.put(buffer.readUtf(), buffer.readUtf());
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

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT tag = new CompoundNBT();
        tag.putString("lastCardTime", DateUtils.toDateTimeString(this.getLastCardTime()));
        tag.putString("lastTpTime", DateUtils.toDateTimeString(this.getLastTpTime()));
        tag.putInt("teleportCard", this.getTeleportCard());
        // 序列化传送记录
        ListNBT recordsNBT = new ListNBT();
        for (TeleportRecord record : this.getTeleportRecords()) {
            recordsNBT.add(record.writeToNBT());
        }
        tag.put("teleportRecords", recordsNBT);
        // 序列化家坐标
        ListNBT homeCoordinateNBT = new ListNBT();
        for (Map.Entry<KeyValue<String, String>, Coordinate> entry : this.getHomeCoordinate().entrySet()) {
            CompoundNBT homeCoordinateTag = new CompoundNBT();
            homeCoordinateTag.putString("key", entry.getKey().getKey());
            homeCoordinateTag.putString("value", entry.getKey().getValue());
            homeCoordinateTag.put("coordinate", entry.getValue().writeToNBT());
            homeCoordinateNBT.add(homeCoordinateTag);
        }
        tag.put("homeCoordinate", homeCoordinateNBT);
        // 序列化默认家
        ListNBT defaultHomeNBT = new ListNBT();
        for (Map.Entry<String, String> entry : this.getDefaultHome().entrySet()) {
            CompoundNBT defaultHomeTag = new CompoundNBT();
            defaultHomeTag.putString("key", entry.getKey());
            defaultHomeTag.putString("value", entry.getValue());
            defaultHomeNBT.add(defaultHomeTag);
        }
        tag.put("defaultHome", defaultHomeNBT);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        this.setLastCardTime(DateUtils.format(nbt.getString("lastCardTime")));
        this.setLastTpTime(DateUtils.format(nbt.getString("lastTpTime")));
        this.setTeleportCard(nbt.getInt("teleportCard"));
        // 反序列化传送记录
        ListNBT recordsNBT = nbt.getList("teleportRecords", 10); // 10 是 CompoundNBT 的类型ID
        List<TeleportRecord> records = new ArrayList<>();
        for (int i = 0; i < recordsNBT.size(); i++) {
            records.add(TeleportRecord.readFromNBT(recordsNBT.getCompound(i)));
        }
        this.setTeleportRecords(records);
        // 反序列化家坐标
        ListNBT homeCoordinateNBT = nbt.getList("homeCoordinate", 10);
        Map<KeyValue<String, String>, Coordinate> homeCoordinate = new HashMap<>();
        for (int i = 0; i < homeCoordinateNBT.size(); i++) {
            CompoundNBT homeCoordinateTag = homeCoordinateNBT.getCompound(i);
            homeCoordinate.put(new KeyValue<>(homeCoordinateTag.getString("key"), homeCoordinateTag.getString("value")),
                    Coordinate.readFromNBT(homeCoordinateTag.getCompound("coordinate")));
        }
        this.setHomeCoordinate(homeCoordinate);
        // 反序列化默认家
        ListNBT defaultHomeNBT = nbt.getList("defaultHome", 10);
        Map<String, String> defaultHome = new HashMap<>();
        for (int i = 0; i < defaultHomeNBT.size(); i++) {
            CompoundNBT defaultHomeTag = defaultHomeNBT.getCompound(i);
            defaultHome.put(defaultHomeTag.getString("key"), defaultHomeTag.getString("value"));
        }
        this.setDefaultHome(defaultHome);
    }

    @Override
    public void save(ServerPlayerEntity player) {
        player.getCapability(PlayerTeleportDataCapability.PLAYER_DATA).ifPresent(this::copyFrom);
    }
}
