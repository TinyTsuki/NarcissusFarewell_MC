package xin.vanilla.narcissus.data.player;

import lombok.NonNull;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.PacketBuffer;
import xin.vanilla.narcissus.data.Coordinate;
import xin.vanilla.narcissus.data.KeyValue;
import xin.vanilla.narcissus.data.PlayerAccess;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.network.packet.PlayerDataSyncPacket;
import xin.vanilla.narcissus.util.CollectionUtils;
import xin.vanilla.narcissus.util.DateUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 玩家传送数据
 */
public final class PlayerTeleportData implements IPlayerData<PlayerTeleportData> {

    // region override

    private static final Map<UUID, PlayerTeleportData> CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    private final PlayerEntity player;
    private boolean dirty = false;

    private PlayerTeleportData(PlayerEntity player) {
        this.player = player;
        if (this.player instanceof ServerPlayerEntity) {
            this.deserializeNBT(PlayerDataManager.instance().getOrCreate(player), false);
        }
    }

    /**
     * 获取或创建 PlayerTeleportData
     */
    public static PlayerTeleportData getData(PlayerEntity player) {
        return CACHE.computeIfAbsent(player.getUUID(), k -> new PlayerTeleportData(player));
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public void setDirty() {
        this.dirty = true;
    }

    @Override
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
    public void writeToBuffer(PacketBuffer buffer) {
        buffer.writeBoolean(this.notified);

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

        buffer.writeNbt(this.getAccess().writeToNBT());
    }

    @Override
    public void readFromBuffer(PacketBuffer buffer) {
        this.notified = buffer.readBoolean();

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

        this.access = PlayerAccess.readFromNBT(Objects.requireNonNull(buffer.readNbt()));

        this.save();
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT tag = new CompoundNBT();
        tag.putBoolean("notified", this.notified);

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

        // 序列化黑白名单
        tag.put("access", this.getAccess().writeToNBT());

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt, boolean dirty) {
        this.notified = nbt.getBoolean("notified");

        this.lastCardTime = DateUtils.format(nbt.getString("lastCardTime"));
        this.lastTpTime = DateUtils.format(nbt.getString("lastTpTime"));
        this.teleportCard.set(nbt.getInt("teleportCard"));

        // 反序列化传送记录
        ListNBT recordsNBT = nbt.getList("teleportRecords", 10);
        List<TeleportRecord> records = new ArrayList<>();
        for (int i = 0; i < recordsNBT.size(); i++) {
            records.add(TeleportRecord.readFromNBT(recordsNBT.getCompound(i)));
        }
        this.teleportRecords = records;

        // 反序列化家坐标
        ListNBT homeCoordinateNBT = nbt.getList("homeCoordinate", 10);
        Map<KeyValue<String, String>, Coordinate> homeCoordinateMap = new HashMap<>();
        for (int i = 0; i < homeCoordinateNBT.size(); i++) {
            CompoundNBT homeCoordinateTag = homeCoordinateNBT.getCompound(i);
            homeCoordinateMap.put(new KeyValue<>(homeCoordinateTag.getString("key"), homeCoordinateTag.getString("value")),
                    Coordinate.readFromNBT(homeCoordinateTag.getCompound("coordinate")));
        }
        this.homeCoordinate = homeCoordinateMap;

        // 反序列化默认家
        ListNBT defaultHomeNBT = nbt.getList("defaultHome", 10);
        Map<String, String> defaultHomeMap = new HashMap<>();
        for (int i = 0; i < defaultHomeNBT.size(); i++) {
            CompoundNBT defaultHomeTag = defaultHomeNBT.getCompound(i);
            defaultHomeMap.put(defaultHomeTag.getString("key"), defaultHomeTag.getString("value"));
        }
        this.defaultHome = defaultHomeMap;

        // 反序列化黑白名单
        this.access = PlayerAccess.readFromNBT(nbt.getCompound("access"));

        if (dirty) {
            this.save();
        }
    }

    @Override
    public void copyFrom(PlayerTeleportData playerData) {
        if (playerData == null) return;

        this.notified = playerData.isNotified();
        this.lastCardTime = playerData.getLastCardTime();
        this.lastTpTime = playerData.getLastTpTime();
        this.teleportCard.set(playerData.getTeleportCard());
        this.teleportRecords = playerData.getTeleportRecords();
        this.homeCoordinate = playerData.getHomeCoordinate();
        this.defaultHome = playerData.getDefaultHome();
        this.access = playerData.getAccess();

        this.save();
    }

    @Override
    public void save() {
        if (this.player instanceof ServerPlayerEntity) {
            PlayerDataManager.instance().put(player, serializeNBT());
        }
    }

    public static void clear() {
        CACHE.clear();
    }

    // endregion override


    private boolean notified;
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
    /**
     * 玩家自定义的黑白名单
     */
    private PlayerAccess access;


    public boolean isNotified() {
        if (this.isDirty()) this.saveEx();
        return notified;
    }

    public void setNotified(boolean notified) {
        this.notified = notified;
        this.save();
    }

    public @NonNull Date getLastCardTime() {
        if (this.isDirty()) this.saveEx();
        return this.lastCardTime == null ? this.lastCardTime = DateUtils.getDate(0, 1, 1) : this.lastCardTime;
    }

    public void setLastCardTime(Date time) {
        this.lastCardTime = time;
        this.save();
    }

    public @NonNull Date getLastTpTime() {
        if (this.isDirty()) this.saveEx();
        return this.lastTpTime == null ? this.lastTpTime = DateUtils.getDate(0, 1, 1) : this.lastTpTime;
    }

    public void setLastTpTime(Date time) {
        this.lastTpTime = time;
        this.save();
    }

    public int getTeleportCard() {
        if (this.isDirty()) this.saveEx();
        return this.teleportCard.get();
    }

    public void setTeleportCard(int num) {
        this.teleportCard.set(num);
        this.save();
    }

    public void plusTeleportCard(int num) {
        this.setTeleportCard(this.getTeleportCard() + num);
    }

    public void subTeleportCard(int num) {
        this.setTeleportCard(this.getTeleportCard() - num);
    }

    public @NonNull List<TeleportRecord> getTeleportRecords() {
        if (this.isDirty()) this.saveEx();
        return this.teleportRecords = CollectionUtils.isNullOrEmpty(this.teleportRecords) ? new ArrayList<>() : this.teleportRecords;
    }

    public @NonNull List<TeleportRecord> getTeleportRecords(EnumTeleportType type) {
        if (this.isDirty()) this.saveEx();
        return CollectionUtils.isNullOrEmpty(this.teleportRecords) ? this.teleportRecords = new ArrayList<>() :
                this.teleportRecords.stream().filter(record -> record.getTeleportType() == type).collect(Collectors.toList());
    }

    public void setTeleportRecords(List<TeleportRecord> records) {
        this.teleportRecords = records;
        this.save();
    }

    public void addTeleportRecords(TeleportRecord... records) {
        this.teleportRecords.addAll(Arrays.asList(records));
        Arrays.stream(records).map(TeleportRecord::getTeleportTime).max(Date::compareTo).ifPresent(this::setLastTpTime);
        this.save();
    }

    public Map<KeyValue<String, String>, Coordinate> getHomeCoordinate() {
        if (this.isDirty()) this.saveEx();
        return this.homeCoordinate = this.homeCoordinate == null ? new HashMap<>() : this.homeCoordinate;
    }

    public void setHomeCoordinate(Map<KeyValue<String, String>, Coordinate> homeCoordinate) {
        this.homeCoordinate = homeCoordinate;
        this.save();
    }

    public void addHomeCoordinate(KeyValue<String, String> key, Coordinate coordinate) {
        this.getHomeCoordinate().put(key, coordinate);
        this.save();
    }

    public Map<String, String> getDefaultHome() {
        if (this.isDirty()) this.saveEx();
        return this.defaultHome = this.defaultHome == null ? new HashMap<>() : this.defaultHome;
    }

    public void setDefaultHome(Map<String, String> defaultHome) {
        this.defaultHome = defaultHome;
        this.save();
    }

    public void addDefaultHome(String key, String value) {
        this.getDefaultHome().put(key, value);
        this.save();
    }

    public KeyValue<String, String> getDefaultHome(String dimension) {
        if (this.getDefaultHome().containsKey(dimension)) {
            return new KeyValue<>(dimension, this.getDefaultHome().get(dimension));
        }
        return null;
    }

    public PlayerAccess getAccess() {
        if (this.isDirty()) this.saveEx();
        return this.access = this.access == null ? new PlayerAccess() : this.access;
    }

    public void setAccess(PlayerAccess access) {
        this.access = access;
        this.save();
    }


    /**
     * 同步玩家数据到客户端
     */
    public static void syncPlayerData(ServerPlayerEntity player) {
        // 创建自定义包并发送到客户端
        PlayerDataSyncPacket packet = new PlayerDataSyncPacket(player.getUUID(), getData(player));
        for (PlayerDataSyncPacket syncPacket : packet.split()) {
            NarcissusUtils.sendPacketToPlayer(syncPacket, player);
        }
    }

}
