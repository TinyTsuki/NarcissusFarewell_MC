package xin.vanilla.narcissus.data.player;

import lombok.NonNull;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;
import xin.vanilla.narcissus.config.Coordinate;
import xin.vanilla.narcissus.config.KeyValue;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.enums.ETeleportType;
import xin.vanilla.narcissus.util.CollectionUtils;
import xin.vanilla.narcissus.util.DateUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 玩家传送数据
 */
public class PlayerTeleportData implements INBTSerializable<CompoundTag> {

    @Setter
    private Date lastCardTime;
    @Setter
    private Date lastTpTime;
    private final AtomicInteger teleportCard = new AtomicInteger();
    @Setter
    private List<TeleportRecord> teleportRecords;
    /**
     * dimension:name coordinate
     */
    @Setter
    private Map<KeyValue<String, String>, Coordinate> homeCoordinate;
    /**
     * dimension:name
     */
    @Setter
    private Map<String, String> defaultHome;
    /**
     * 是否已发送使用说明
     */
    private boolean notified;
    private String language = "client";

    public int getTeleportCard() {
        return this.teleportCard.get();
    }

    public int plusTeleportCard() {
        return this.teleportCard.incrementAndGet();
    }

    public int plusTeleportCard(int num) {
        return this.teleportCard.addAndGet(num);
    }

    public int subTeleportCard() {
        return this.teleportCard.decrementAndGet();
    }

    public int subTeleportCard(int num) {
        return this.teleportCard.addAndGet(-num);
    }

    public void setTeleportCard(int num) {
        this.teleportCard.set(num);
    }

    public @NonNull Date getLastCardTime() {
        return this.lastCardTime = this.lastCardTime == null ? DateUtils.getDate(0, 1, 1) : this.lastCardTime;
    }

    public @NonNull Date getLastTpTime() {
        return this.lastTpTime = this.lastTpTime == null ? DateUtils.getDate(0, 1, 1) : this.lastTpTime;
    }

    public @NonNull List<TeleportRecord> getTeleportRecords() {
        return teleportRecords = CollectionUtils.isNullOrEmpty(teleportRecords) ? new ArrayList<>() : teleportRecords;
    }

    public @NonNull List<TeleportRecord> getTeleportRecords(ETeleportType type) {
        return CollectionUtils.isNullOrEmpty(teleportRecords) ? teleportRecords = new ArrayList<>() :
                teleportRecords.stream().filter(record -> record.getTeleportType() == type).collect(Collectors.toList());
    }

    public void addTeleportRecords(TeleportRecord... records) {
        this.getTeleportRecords().addAll(Arrays.asList((records)));
        this.getTeleportRecords().sort(Comparator.comparing(TeleportRecord::getTeleportTime));
        int limit = ServerConfig.TELEPORT_RECORD_LIMIT.get();
        int size = this.getTeleportRecords().size();
        if (limit > 0 && limit < size) {
            this.getTeleportRecords().subList(0, size - limit).clear();
        }
    }

    public Map<KeyValue<String, String>, Coordinate> getHomeCoordinate() {
        return homeCoordinate = homeCoordinate == null ? new HashMap<>() : homeCoordinate;
    }

    public void addHomeCoordinate(KeyValue<String, String> key, Coordinate coordinate) {
        this.getHomeCoordinate().put(key, coordinate);
    }

    public Map<String, String> getDefaultHome() {
        return defaultHome = defaultHome == null ? new HashMap<>() : defaultHome;
    }

    public String getLanguage() {
        return this.language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @NonNull
    public String getValidLanguage(@Nullable Player player) {
        return NarcissusUtils.getValidLanguage(player, this.getLanguage());
    }

    public void addDefaultHome(String key, String value) {
        this.getDefaultHome().put(key, value);
    }

    public KeyValue<String, String> getDefaultHome(String dimension) {
        if (this.getDefaultHome().containsKey(dimension)) {
            return new KeyValue<>(dimension, this.getDefaultHome().get(dimension));
        }
        return null;
    }

    public boolean isNotified() {
        return this.notified;
    }

    public void setNotified(boolean notified) {
        this.notified = notified;
    }

    public void writeToBuffer(FriendlyByteBuf buffer) {
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
        buffer.writeBoolean(this.notified);
        buffer.writeUtf(this.getLanguage());
    }

    public void readFromBuffer(FriendlyByteBuf buffer) {
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
        this.notified = buffer.readBoolean();
        this.language = buffer.readUtf();
    }

    public void copyFrom(PlayerTeleportData capability) {
        this.lastCardTime = capability.getLastCardTime();
        this.lastTpTime = capability.getLastTpTime();
        this.teleportCard.set(capability.getTeleportCard());
        this.teleportRecords = capability.getTeleportRecords();
        this.homeCoordinate = capability.getHomeCoordinate();
        this.defaultHome = capability.getDefaultHome();
        this.notified = capability.isNotified();
        this.language = capability.getLanguage();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        CompoundTag tag = new CompoundTag();
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
        for (Map.Entry<KeyValue<String, String>, Coordinate> entry : this.getHomeCoordinate().entrySet()) {
            CompoundTag homeCoordinateTag = new CompoundTag();
            homeCoordinateTag.putString("key", entry.getKey().getKey());
            homeCoordinateTag.putString("value", entry.getKey().getValue());
            homeCoordinateTag.put("coordinate", entry.getValue().writeToNBT());
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
        tag.putBoolean("notified", this.notified);
        tag.putString("language", this.getLanguage());
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, CompoundTag nbt) {
        this.setLastCardTime(DateUtils.format(nbt.getString("lastCardTime").orElse(DateUtils.toDateTimeString(new Date(0)))));
        this.setLastTpTime(DateUtils.format(nbt.getString("lastTpTime").orElse(DateUtils.toDateTimeString(new Date(0)))));
        this.setTeleportCard(nbt.getInt("teleportCard").orElse(0));
        // 反序列化传送记录
        ListTag recordsNBT = nbt.getList("teleportRecords").orElse(new ListTag());
        List<TeleportRecord> records = new ArrayList<>();
        for (int i = 0; i < recordsNBT.size(); i++) {
            records.add(TeleportRecord.readFromNBT(recordsNBT.getCompound(i).orElse(new Coordinate().writeToNBT())));
        }
        this.setTeleportRecords(records);
        // 反序列化家坐标
        ListTag homeCoordinateNBT = nbt.getList("homeCoordinate").orElse(new ListTag());
        Map<KeyValue<String, String>, Coordinate> homeCoordinate = new HashMap<>();
        for (int i = 0; i < homeCoordinateNBT.size(); i++) {
            CompoundTag homeCoordinateTag = homeCoordinateNBT.getCompound(i).orElse(new CompoundTag());
            homeCoordinate.put(new KeyValue<>(homeCoordinateTag.getString("key").orElse(""), homeCoordinateTag.getString("value").orElse("")),
                    Coordinate.readFromNBT(homeCoordinateTag.getCompound("coordinate").orElse(new Coordinate().writeToNBT())));
        }
        this.setHomeCoordinate(homeCoordinate);
        // 反序列化默认家
        ListTag defaultHomeNBT = nbt.getList("defaultHome").orElse(new ListTag());
        Map<String, String> defaultHome = new HashMap<>();
        for (int i = 0; i < defaultHomeNBT.size(); i++) {
            CompoundTag defaultHomeTag = defaultHomeNBT.getCompound(i).orElse(new CompoundTag());
            defaultHome.put(defaultHomeTag.getString("key").orElse(""), defaultHomeTag.getString("value").orElse(""));
        }
        this.setDefaultHome(defaultHome);
        this.notified = nbt.getBoolean("notified").orElse(false);
        this.setLanguage(nbt.getString("language").orElse("client"));
    }
}
