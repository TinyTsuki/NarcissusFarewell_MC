package xin.vanilla.narcissus.data.player;

import lombok.NonNull;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import xin.vanilla.banira.api.BaniraPlayerData;
import xin.vanilla.banira.common.api.ICommandNotify;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.common.player.IPlayerData;
import xin.vanilla.banira.common.util.CollectionUtils;
import xin.vanilla.banira.common.util.DateUtils;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.TeleportCountdownHelper;
import xin.vanilla.narcissus.data.PlayerAccess;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.internal.network.NarcissusNbtPacketCodec;
import xin.vanilla.narcissus.network.packet.PlayerDataSyncToClient;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 玩家传送数据
 */
public final class PlayerTeleportData implements IPlayerData<PlayerTeleportData>, ICommandNotify {

    // region override

    private static final Map<UUID, PlayerTeleportData> CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    private final Player player;
    private boolean dirty = false;

    private PlayerTeleportData(Player player) {
        this.player = player;
        if (this.player instanceof ServerPlayer) {
            this.deserializeNBT(BaniraPlayerData.getOrCreate(
                    player.getUUID(), NarcissusFarewell.MODID, CompoundTag.class), false);
        }
    }

    /**
     * 获取或创建 PlayerTeleportData
     */
    public static PlayerTeleportData getData(Player player) {
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
    public void writeToBuffer(BaniraPacketBuffer buffer) {
        buffer.writeBoolean(this.notified);
        buffer.writeUtf(DateUtils.toDateTimeString(this.getLastCardTime()));
        buffer.writeUtf(DateUtils.toDateTimeString(this.getLastTpTime()));
        buffer.writeInt(this.getTeleportCard());

        buffer.writeInt(this.teleportRecords.size());
        for (TeleportRecord teleportRecord : this.getTeleportRecords()) {
            NarcissusNbtPacketCodec.write(buffer, teleportRecord.writeToNBT());
        }

        buffer.writeInt(this.getHomeCoordinate().size());
        for (Map.Entry<KeyValue<String, String>, SafeWorldCoordinate> entry : this.getHomeCoordinate().entrySet()) {
            buffer.writeUtf(entry.getKey().key());
            buffer.writeUtf(entry.getKey().value());
            NarcissusNbtPacketCodec.write(buffer, entry.getValue().toTag());
        }

        buffer.writeInt(this.getDefaultHome().size());
        for (Map.Entry<String, String> entry : this.getDefaultHome().entrySet()) {
            buffer.writeUtf(entry.getKey());
            buffer.writeUtf(entry.getValue());
        }

        NarcissusNbtPacketCodec.write(buffer, this.getAccess().writeToNBT());
    }

    @Override
    public void readFromBuffer(BaniraPacketBuffer buffer) {
        this.notified = buffer.readBoolean();
        this.lastCardTime = DateUtils.format(buffer.readUtf());
        this.lastTpTime = DateUtils.format(buffer.readUtf());
        this.teleportCard.set(buffer.readInt());

        this.teleportRecords = new ArrayList<>();
        for (int i = 0; i < buffer.readInt(); i++) {
            this.teleportRecords.add(TeleportRecord.readFromNBT(NarcissusNbtPacketCodec.read(buffer)));
        }

        this.homeCoordinate = new LinkedHashMap<>();
        for (int i = 0; i < buffer.readInt(); i++) {
            this.homeCoordinate.put(new KeyValue<>(buffer.readUtf(), buffer.readUtf()),
                    SafeWorldCoordinate.fromTag(NarcissusNbtPacketCodec.read(buffer)));
        }

        this.defaultHome = new HashMap<>();
        for (int i = 0; i < buffer.readInt(); i++) {
            this.defaultHome.put(buffer.readUtf(), buffer.readUtf());
        }

        this.access = PlayerAccess.readFromNBT(NarcissusNbtPacketCodec.read(buffer));

        this.save();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("notified", this.notified);
        tag.putString("lastCardTime", DateUtils.toDateTimeString(this.getLastCardTime()));
        tag.putString("lastTpTime", DateUtils.toDateTimeString(this.getLastTpTime()));
        tag.putInt("teleportCard", this.getTeleportCard());

        // 序列化传送记录
        ListTag recordsNBT = new ListTag();
        for (TeleportRecord record : this.getTeleportRecords()) {
            recordsNBT.add(record.writeToNBT());
        }
        tag.put("teleportRecords", recordsNBT);

        // 序列化家坐标
        ListTag homeCoordinateNBT = new ListTag();
        for (Map.Entry<KeyValue<String, String>, SafeWorldCoordinate> entry : this.getHomeCoordinate().entrySet()) {
            CompoundTag homeCoordinateTag = new CompoundTag();
            homeCoordinateTag.putString("key", entry.getKey().key());
            homeCoordinateTag.putString("value", entry.getKey().value());
            homeCoordinateTag.put("coordinate", entry.getValue().toTag());
            homeCoordinateNBT.add(homeCoordinateTag);
        }
        tag.put("homeCoordinate", homeCoordinateNBT);

        // 序列化默认家
        ListTag defaultHomeNBT = new ListTag();
        for (Map.Entry<String, String> entry : this.getDefaultHome().entrySet()) {
            CompoundTag defaultHomeTag = new CompoundTag();
            defaultHomeTag.putString("key", entry.getKey());
            defaultHomeTag.putString("value", entry.getValue());
            defaultHomeNBT.add(defaultHomeTag);
        }
        tag.put("defaultHome", defaultHomeNBT);

        // 序列化黑白名单
        tag.put("access", this.getAccess().writeToNBT());

        tag.put("tpCountdowns", writeTeleportCountdownToNbt());

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt, boolean dirty) {
        this.notified = nbt.getBoolean("notified");
        this.lastCardTime = DateUtils.format(nbt.getString("lastCardTime"));
        this.lastTpTime = DateUtils.format(nbt.getString("lastTpTime"));
        this.teleportCard.set(nbt.getInt("teleportCard"));

        // 反序列化传送记录
        ListTag recordsNBT = nbt.getList("teleportRecords", 10);
        List<TeleportRecord> records = new ArrayList<>();
        for (int i = 0; i < recordsNBT.size(); i++) {
            records.add(TeleportRecord.readFromNBT(recordsNBT.getCompound(i)));
        }
        this.teleportRecords = records;

        // 反序列化家坐标
        ListTag homeCoordinateNBT = nbt.getList("homeCoordinate", 10);
        Map<KeyValue<String, String>, SafeWorldCoordinate> homeCoordinateMap = new LinkedHashMap<>();
        for (int i = 0; i < homeCoordinateNBT.size(); i++) {
            CompoundTag homeCoordinateTag = homeCoordinateNBT.getCompound(i);
            homeCoordinateMap.put(new KeyValue<>(homeCoordinateTag.getString("key"), homeCoordinateTag.getString("value")),
                    SafeWorldCoordinate.fromTag(homeCoordinateTag.getCompound("coordinate")));
        }
        this.homeCoordinate = homeCoordinateMap;

        // 反序列化默认家
        ListTag defaultHomeNBT = nbt.getList("defaultHome", 10);
        Map<String, String> defaultHomeMap = new HashMap<>();
        for (int i = 0; i < defaultHomeNBT.size(); i++) {
            CompoundTag defaultHomeTag = defaultHomeNBT.getCompound(i);
            defaultHomeMap.put(defaultHomeTag.getString("key"), defaultHomeTag.getString("value"));
        }
        this.defaultHome = defaultHomeMap;

        // 反序列化黑白名单
        this.access = PlayerAccess.readFromNBT(nbt.getCompound("access"));

        readTeleportCountdownFromNbt(nbt.contains("tpCountdowns", 10) ? nbt.getCompound("tpCountdowns") : new CompoundTag());

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
        this.teleportCountdownSeconds = playerData.teleportCountdownSeconds == null
                ? null
                : new EnumMap<>(playerData.teleportCountdownSeconds);

        this.save();
    }

    @Override
    public void save() {
        if (this.player instanceof ServerPlayer) {
            BaniraPlayerData.put(player.getUUID(), NarcissusFarewell.MODID, serializeNBT());
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
    private Map<KeyValue<String, String>, SafeWorldCoordinate> homeCoordinate;
    /**
     * dimension:name
     */
    private Map<String, String> defaultHome;
    /**
     * 玩家自定义的黑白名单
     */
    private PlayerAccess access;
    /**
     * 各传送类型的传送前倒计时（秒）
     */
    private EnumMap<EnumTeleportType, Integer> teleportCountdownSeconds;

    public CompoundTag writeTeleportCountdownToNbt() {
        CompoundTag cd = new CompoundTag();
        EnumMap<EnumTeleportType, Integer> map = this.teleportCountdownSeconds;
        if (map != null) {
            for (Map.Entry<EnumTeleportType, Integer> e : map.entrySet()) {
                if (e.getValue() != null && e.getValue() > 0) {
                    cd.putInt(e.getKey().name(), e.getValue());
                }
            }
        }
        return cd;
    }

    public void readTeleportCountdownFromNbt(CompoundTag cd) {
        this.teleportCountdownSeconds = new EnumMap<>(EnumTeleportType.class);
        if (cd == null || cd.isEmpty()) {
            return;
        }
        for (EnumTeleportType t : EnumTeleportType.countdownConfigurableTypes()) {
            if (!cd.contains(t.name())) {
                continue;
            }
            int v = TeleportCountdownHelper.clampToPlayerAllowedRange(cd.getInt(t.name()));
            if (v > 0) {
                this.teleportCountdownSeconds.put(t, v);
            }
        }
    }

    /**
     * 玩家为该类型存储的倒计时偏好（秒），未设置时为 0；读取时按当前 common 配置的「玩家允许范围」夹取。
     */
    public int getTeleportCountdownSeconds(EnumTeleportType type) {
        if (type == null || !EnumTeleportType.countdownConfigurableTypes().contains(type)) {
            return 0;
        }
        if (this.isDirty()) this.saveEx();
        if (teleportCountdownSeconds == null) {
            return 0;
        }
        Integer v = teleportCountdownSeconds.get(type);
        if (v == null) {
            return 0;
        }
        return TeleportCountdownHelper.clampToPlayerAllowedRange(v);
    }

    public void setTeleportCountdownSeconds(EnumTeleportType type, int seconds) {
        if (type == null || !EnumTeleportType.countdownConfigurableTypes().contains(type)) {
            return;
        }
        int v = TeleportCountdownHelper.clampToPlayerAllowedRange(seconds);
        if (teleportCountdownSeconds == null) {
            teleportCountdownSeconds = new EnumMap<>(EnumTeleportType.class);
        }
        if (v == 0) {
            teleportCountdownSeconds.remove(type);
        } else {
            teleportCountdownSeconds.put(type, v);
        }
        this.save();
    }

    /**
     * 用客户端提交的完整表替换各传送倒计时（秒），仅接受 {@link EnumTeleportType#countdownConfigurableTypes()} 中的键。
     */
    public void replaceAllTeleportCountdownsFromTag(CompoundTag tag) {
        this.teleportCountdownSeconds = new EnumMap<>(EnumTeleportType.class);
        if (tag != null) {
            for (EnumTeleportType t : EnumTeleportType.countdownConfigurableTypes()) {
                if (!tag.contains(t.name())) {
                    continue;
                }
                int v = TeleportCountdownHelper.clampToPlayerAllowedRange(tag.getInt(t.name()));
                if (v > 0) {
                    this.teleportCountdownSeconds.put(t, v);
                }
            }
        }
        this.save();
    }

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

    public Map<KeyValue<String, String>, SafeWorldCoordinate> getHomeCoordinate() {
        if (this.isDirty()) this.saveEx();
        return this.homeCoordinate = this.homeCoordinate == null ? new LinkedHashMap<>() : this.homeCoordinate;
    }

    public void setHomeCoordinate(Map<KeyValue<String, String>, SafeWorldCoordinate> homeCoordinate) {
        this.homeCoordinate = homeCoordinate;
        this.save();
    }

    public void addHomeCoordinate(KeyValue<String, String> key, SafeWorldCoordinate coordinate) {
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
    public static void syncPlayerData(ServerPlayer player) {
        PlayerDataSyncToClient packet = new PlayerDataSyncToClient(player.getUUID(), getData(player));
        PacketUtils.sendSplitPacketToPlayer(packet, player);
    }

}
