package xin.vanilla.narcissus.data.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import lombok.Getter;
import lombok.NonNull;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.data.Coordinate;
import xin.vanilla.narcissus.data.KeyValue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 世界驿站数据
 */
@Getter
public class WorldStageData extends SavedData {
    private static final String DATA_NAME = "world_stage_data";

    public static final Codec<WorldStageData> CODEC = Codec.PASSTHROUGH.comapFlatMap(
            dynamic -> {
                try {
                    // 将 Dynamic 转换为 CompoundTag
                    if (dynamic.getValue() instanceof CompoundTag tag) {
                        return DataResult.success(WorldStageData.load(tag));
                    } else {
                        return DataResult.error(() -> "Dynamic value is not a CompoundTag.");
                    }
                } catch (Exception e) {
                    return DataResult.error(() -> "Failed to decode WorldStageData: " + e.getMessage());
                }
            },
            data -> {
                CompoundTag tag = new CompoundTag();
                data.save(tag);
                return new Dynamic<>(NbtOps.INSTANCE, tag);
            }
    );

    public static final SavedDataType<WorldStageData> TYPE = new SavedDataType<>(
            DATA_NAME,
            context -> new WorldStageData(new HashMap<>()),
            ctx -> CODEC,
            DataFixTypes.SAVED_DATA_MAP_DATA
    );

    // dimension:name coordinate
    private Map<KeyValue<String, String>, Coordinate> stageCoordinate = new LinkedHashMap<>();

    public WorldStageData() {
    }

    public WorldStageData(Map<KeyValue<String, String>, Coordinate> stageCoordinate) {
        this.stageCoordinate = stageCoordinate;
    }

    public static WorldStageData load(CompoundTag nbt) {
        WorldStageData data = new WorldStageData();
        ListTag stageCoordinateNBT = nbt.getList("stageCoordinate").orElse(new ListTag());
        Map<KeyValue<String, String>, Coordinate> stageCoordinate = new HashMap<>();
        for (int i = 0; i < stageCoordinateNBT.size(); i++) {
            CompoundTag stageCoordinateTag = stageCoordinateNBT.getCompound(i).orElse(new CompoundTag());
            stageCoordinate.put(new KeyValue<>(stageCoordinateTag.getString("key").orElse(""), stageCoordinateTag.getString("value").orElse("")),
                    Coordinate.readFromNBT(stageCoordinateTag.getCompound("coordinate").orElse(new Coordinate().writeToNBT())));
        }
        data.setCoordinate(stageCoordinate);
        return data;
    }

    @NonNull
    public CompoundTag save(CompoundTag nbt) {
        ListTag stageCoordinateNBT = new ListTag();
        for (Map.Entry<KeyValue<String, String>, Coordinate> entry : this.getStageCoordinate().entrySet()) {
            CompoundTag stageCoordinateTag = new CompoundTag();
            stageCoordinateTag.putString("key", entry.getKey().getKey());
            stageCoordinateTag.putString("value", entry.getKey().getValue());
            stageCoordinateTag.put("coordinate", entry.getValue().writeToNBT());
            stageCoordinateNBT.add(stageCoordinateTag);
        }
        nbt.put("stageCoordinate", stageCoordinateNBT);
        return nbt;
    }

    public void setCoordinate(Map<KeyValue<String, String>, Coordinate> stageCoordinate) {
        this.stageCoordinate = stageCoordinate;
        super.setDirty();
    }

    public void addCoordinate(KeyValue<String, String> key, Coordinate coordinate) {
        this.stageCoordinate.put(key, coordinate);
        super.setDirty();
    }

    public int getCoordinateSize(String name) {
        return (int) this.getStageCoordinate().keySet().stream().filter(keyValue -> keyValue.getValue().equals(name)).count();
    }

    public int getCoordinateSize(String dimension, String name) {
        return (int) this.getStageCoordinate().keySet().stream().filter(keyValue -> keyValue.getKey().equals(dimension) && keyValue.getValue().equals(name)).count();
    }

    public Coordinate getCoordinate(String name) {
        return getCoordinateSize(name) == 1 ? this.getStageCoordinate().entrySet().stream()
                .filter(entry -> entry.getKey().getValue().equals(name))
                .findFirst().map(Map.Entry::getValue).orElse(null)
                : null;
    }

    public Coordinate getCoordinate(String dimension, String name) {
        return getCoordinateSize(dimension, name) == 1 ? this.getStageCoordinate().entrySet().stream()
                .filter(entry -> entry.getKey().getKey().equals(dimension) && entry.getKey().getValue().equals(name))
                .findFirst().map(Map.Entry::getValue).orElse(null)
                : null;
    }

    public static WorldStageData get() {
        return get(NarcissusFarewell.getServerInstance().getAllLevels().iterator().next());
    }

    public static WorldStageData get(ServerPlayer player) {
        return get(player.level());
    }

    public static WorldStageData get(ServerLevel world) {
        return world.getDataStorage().computeIfAbsent(WorldStageData.TYPE);
    }
}
