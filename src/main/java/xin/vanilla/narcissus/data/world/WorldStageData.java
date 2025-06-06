package xin.vanilla.narcissus.data.world;

import lombok.Getter;
import lombok.NonNull;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.WorldCapabilityData;
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
public class WorldStageData extends WorldCapabilityData {
    private static final String DATA_NAME = "world_stage_data";

    // dimension:name coordinate
    private Map<KeyValue<String, String>, Coordinate> stageCoordinate = new LinkedHashMap<>();

    public WorldStageData() {
        super(DATA_NAME);
    }

    public void load(CompoundNBT nbt) {
        this.stageCoordinate = new LinkedHashMap<>();
        ListNBT stageCoordinateNBT = nbt.getList("stageCoordinate", 10);
        Map<KeyValue<String, String>, Coordinate> stageCoordinate = new HashMap<>();
        for (int i = 0; i < stageCoordinateNBT.size(); i++) {
            CompoundNBT stageCoordinateTag = stageCoordinateNBT.getCompound(i);
            stageCoordinate.put(new KeyValue<>(stageCoordinateTag.getString("key"), stageCoordinateTag.getString("value")),
                    Coordinate.readFromNBT(stageCoordinateTag.getCompound("coordinate")));
        }
        this.setCoordinate(stageCoordinate);
    }

    @Override
    @NonNull
    public CompoundNBT save(CompoundNBT nbt) {
        ListNBT stageCoordinateNBT = new ListNBT();
        for (Map.Entry<KeyValue<String, String>, Coordinate> entry : this.getStageCoordinate().entrySet()) {
            CompoundNBT stageCoordinateTag = new CompoundNBT();
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

    public static WorldStageData get(ServerPlayerEntity player) {
        return get(player.getLevel());
    }

    public static WorldStageData get(ServerWorld world) {
        return world.getDataStorage().computeIfAbsent(WorldStageData::new, DATA_NAME);
    }
}
