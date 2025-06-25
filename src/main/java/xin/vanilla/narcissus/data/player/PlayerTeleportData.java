package xin.vanilla.narcissus.data.player;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import org.jetbrains.annotations.NotNull;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.data.Coordinate;
import xin.vanilla.narcissus.data.KeyValue;
import xin.vanilla.narcissus.data.PlayerAccess;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.CollectionUtils;
import xin.vanilla.narcissus.util.DateUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 玩家传送数据
 */
public class PlayerTeleportData implements ValueIOSerializable {
    /**
     * 是否已发送使用说明
     */
    @Getter
    @Setter
    private boolean notified;
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
     * 玩家自定义的黑白名单
     */
    @Setter
    private PlayerAccess access;

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
        return this.teleportRecords = CollectionUtils.isNullOrEmpty(this.teleportRecords) ? new ArrayList<>() : this.teleportRecords;
    }

    public @NonNull List<TeleportRecord> getTeleportRecords(EnumTeleportType type) {
        return CollectionUtils.isNullOrEmpty(this.teleportRecords) ? this.teleportRecords = new ArrayList<>() :
                this.teleportRecords.stream().filter(record -> record.getTeleportType() == type).collect(Collectors.toList());
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
        return this.homeCoordinate = this.homeCoordinate == null ? new HashMap<>() : this.homeCoordinate;
    }

    public void addHomeCoordinate(KeyValue<String, String> key, Coordinate coordinate) {
        this.getHomeCoordinate().put(key, coordinate);
    }

    public Map<String, String> getDefaultHome() {
        return this.defaultHome = this.defaultHome == null ? new HashMap<>() : this.defaultHome;
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

    public PlayerAccess getAccess() {
        return this.access = this.access == null ? new PlayerAccess() : this.access;
    }

    public void writeToBuffer(FriendlyByteBuf buffer) {
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

    public void readFromBuffer(FriendlyByteBuf buffer) {
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
    }

    public void copyFrom(PlayerTeleportData capability) {
        this.notified = capability.isNotified();
        this.lastCardTime = capability.getLastCardTime();
        this.lastTpTime = capability.getLastTpTime();
        this.teleportCard.set(capability.getTeleportCard());
        this.teleportRecords = capability.getTeleportRecords();
        this.homeCoordinate = capability.getHomeCoordinate();
        this.defaultHome = capability.getDefaultHome();
        this.access = capability.getAccess();
    }

    @Override
    public void serialize(@NotNull ValueOutput output) {
        output.putBoolean("notified", this.notified);
        output.putString("lastCardTime", DateUtils.toDateTimeString(this.getLastCardTime()));
        output.putString("lastTpTime", DateUtils.toDateTimeString(this.getLastTpTime()));
        output.putInt("teleportCard", this.getTeleportCard());

        // 序列化传送记录
        ValueOutput.TypedOutputList<CompoundTag> recordsNBT = output.list("teleportRecords", CompoundTag.CODEC);
        for (TeleportRecord record : this.getTeleportRecords()) {
            recordsNBT.add(record.writeToNBT());
        }

        // 序列化家坐标
        ValueOutput.TypedOutputList<CompoundTag> homeCoordinateNBT = output.list("homeCoordinate", CompoundTag.CODEC);
        for (Map.Entry<KeyValue<String, String>, Coordinate> entry : this.getHomeCoordinate().entrySet()) {
            CompoundTag homeCoordinateTag = new CompoundTag();
            homeCoordinateTag.putString("key", entry.getKey().getKey());
            homeCoordinateTag.putString("value", entry.getKey().getValue());
            homeCoordinateTag.put("coordinate", entry.getValue().writeToNBT());
            homeCoordinateNBT.add(homeCoordinateTag);
        }

        // 序列化默认家
        ValueOutput.TypedOutputList<CompoundTag> defaultHomeNBT = output.list("defaultHome", CompoundTag.CODEC);
        for (Map.Entry<String, String> entry : this.getDefaultHome().entrySet()) {
            CompoundTag defaultHomeTag = new CompoundTag();
            defaultHomeTag.putString("key", entry.getKey());
            defaultHomeTag.putString("value", entry.getValue());
            defaultHomeNBT.add(defaultHomeTag);
        }

        // 序列化黑白名单
        output.store("access", CompoundTag.CODEC, this.getAccess().writeToNBT());
    }

    @Override
    public void deserialize(@NotNull ValueInput input) {
        this.notified = input.getBooleanOr("notified", false);
        this.setLastCardTime(DateUtils.format(input.getString("lastCardTime").orElse(DateUtils.toDateTimeString(new Date(0)))));
        this.setLastTpTime(DateUtils.format(input.getString("lastTpTime").orElse(DateUtils.toDateTimeString(new Date(0)))));
        this.setTeleportCard(input.getInt("teleportCard").orElse(0));

        // 反序列化传送记录
        input.list("teleportRecords", CompoundTag.CODEC).ifPresent(recordsNBT -> {
            List<TeleportRecord> records = new ArrayList<>();
            for (CompoundTag tag : recordsNBT) {
                records.add(TeleportRecord.readFromNBT(tag));
            }
            this.setTeleportRecords(records);
        });

        // 反序列化家坐标
        input.list("homeCoordinate", CompoundTag.CODEC).ifPresent(homeCoordinateNBT -> {
            Map<KeyValue<String, String>, Coordinate> homeCoordinate = new HashMap<>();
            for (CompoundTag tag : homeCoordinateNBT) {
                homeCoordinate.put(new KeyValue<>(tag.getString("key").orElse(""), tag.getString("value").orElse("")),
                        Coordinate.readFromNBT(tag.getCompound("coordinate").orElse(new Coordinate().writeToNBT())));
            }
            this.setHomeCoordinate(homeCoordinate);
        });

        // 反序列化默认家
        input.list("defaultHome", CompoundTag.CODEC).ifPresent(defaultHomeNBT -> {
            Map<String, String> defaultHome = new HashMap<>();
            for (CompoundTag tag : defaultHomeNBT) {
                defaultHome.put(tag.getString("key").orElse(""), tag.getString("value").orElse(""));
                this.setDefaultHome(defaultHome);
            }
        });

        // 反序列化黑白名单
        this.setAccess(PlayerAccess.readFromNBT(input.read("access", CompoundTag.CODEC).orElse(new CompoundTag())));
    }
}
