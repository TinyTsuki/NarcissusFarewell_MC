package xin.vanilla.narcissus.data.world;

import lombok.Getter;
import lombok.NonNull;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import xin.vanilla.banira.api.BaniraServer;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 世界驿站数据
 */
@Getter
public class WorldStageData extends SavedData {
    private static final String DATA_NAME = "world_stage_data";

    // dimension:name coordinate
    private Map<KeyValue<String, String>, SafeWorldCoordinate> stageCoordinate = new LinkedHashMap<>();

    public WorldStageData() {
    }

    public static WorldStageData load(CompoundTag nbt, HolderLookup.Provider provider) {
        WorldStageData data = new WorldStageData();
        ListTag stageCoordinateNBT = nbt.getList("stageCoordinate", 10);
        Map<KeyValue<String, String>, SafeWorldCoordinate> stageCoordinate = new LinkedHashMap<>();
        for (int i = 0; i < stageCoordinateNBT.size(); i++) {
            CompoundTag stageCoordinateTag = stageCoordinateNBT.getCompound(i);
            stageCoordinate.put(new KeyValue<>(stageCoordinateTag.getString("key"), stageCoordinateTag.getString("value")),
                    SafeWorldCoordinate.fromTag(stageCoordinateTag.getCompound("coordinate")));
        }
        data.setCoordinate(stageCoordinate);
        return data;
    }

    @Override
    @NonNull
    @ParametersAreNonnullByDefault
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider provider) {
        ListTag stageCoordinateNBT = new ListTag();
        for (Map.Entry<KeyValue<String, String>, SafeWorldCoordinate> entry : this.getStageCoordinate().entrySet()) {
            CompoundTag stageCoordinateTag = new CompoundTag();
            stageCoordinateTag.putString("key", entry.getKey().key());
            stageCoordinateTag.putString("value", entry.getKey().value());
            stageCoordinateTag.put("coordinate", entry.getValue().toTag());
            stageCoordinateNBT.add(stageCoordinateTag);
        }
        nbt.put("stageCoordinate", stageCoordinateNBT);
        return nbt;
    }

    public void setCoordinate(Map<KeyValue<String, String>, SafeWorldCoordinate> stageCoordinate) {
        this.stageCoordinate = stageCoordinate;
        super.setDirty();
    }

    public void addCoordinate(KeyValue<String, String> key, SafeWorldCoordinate coordinate) {
        this.stageCoordinate.put(key, coordinate);
        super.setDirty();
    }

    public int getCoordinateSize(String name) {
        return (int) this.getStageCoordinate().keySet().stream().filter(keyValue -> keyValue.value().equals(name)).count();
    }

    public int getCoordinateSize(String dimension, String name) {
        return (int) this.getStageCoordinate().keySet().stream().filter(keyValue -> keyValue.key().equals(dimension) && keyValue.value().equals(name)).count();
    }

    public SafeWorldCoordinate getCoordinate(String name) {
        return getCoordinateSize(name) == 1 ? this.getStageCoordinate().entrySet().stream()
                .filter(entry -> entry.getKey().value().equals(name))
                .findFirst().map(Map.Entry::getValue).orElse(null)
                : null;
    }

    public SafeWorldCoordinate getCoordinate(String dimension, String name) {
        return getCoordinateSize(dimension, name) == 1 ? this.getStageCoordinate().entrySet().stream()
                .filter(entry -> entry.getKey().key().equals(dimension) && entry.getKey().value().equals(name))
                .findFirst().map(Map.Entry::getValue).orElse(null)
                : null;
    }

    public static WorldStageData get() {
        return get(BaniraServer.require(MinecraftServer.class).getAllLevels().iterator().next());
    }

    public static WorldStageData get(ServerPlayer player) {
        return get(player.serverLevel());
    }

    public static WorldStageData get(ServerLevel world) {
        return world.getDataStorage().computeIfAbsent(new Factory<>(WorldStageData::new, WorldStageData::load, DataFixTypes.SAVED_DATA_MAP_DATA), DATA_NAME);
    }
}
