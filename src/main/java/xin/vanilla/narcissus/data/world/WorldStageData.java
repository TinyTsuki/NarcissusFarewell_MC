package xin.vanilla.narcissus.data.world;

import lombok.Getter;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.common.util.Constants;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.Coordinate;
import xin.vanilla.narcissus.config.KeyValue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 世界驿站数据
 */
@Getter
public class WorldStageData extends WorldSavedData {
    public static final String DATA_NAME = "world_stage_data";

    // dimension:name coordinate
    private Map<KeyValue<String, String>, Coordinate> stageCoordinate = new LinkedHashMap<>();

    public WorldStageData() {
        super(DATA_NAME);
    }

    public WorldStageData(String name) {
        super(name);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        this.stageCoordinate = new LinkedHashMap<>();
        NBTTagList stageCoordinateNBT = nbt.getTagList("stageCoordinate", Constants.NBT.TAG_COMPOUND);
        Map<KeyValue<String, String>, Coordinate> stageCoordinate = new HashMap<>();
        for (int i = 0; i < stageCoordinateNBT.tagCount(); i++) {
            NBTTagCompound stageCoordinateTag = stageCoordinateNBT.getCompoundTagAt(i);
            stageCoordinate.put(new KeyValue<>(stageCoordinateTag.getString("key"), stageCoordinateTag.getString("value")),
                    Coordinate.readFromNBT(stageCoordinateTag.getCompoundTag("coordinate")));
        }
        this.setCoordinate(stageCoordinate);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        NBTTagList stageCoordinateNBT = new NBTTagList();
        for (Map.Entry<KeyValue<String, String>, Coordinate> entry : this.getStageCoordinate().entrySet()) {
            NBTTagCompound stageCoordinateTag = new NBTTagCompound();
            stageCoordinateTag.setString("key", entry.getKey().getKey());
            stageCoordinateTag.setString("value", entry.getKey().getValue());
            stageCoordinateTag.setTag("coordinate", entry.getValue().writeToNBT());
            stageCoordinateNBT.appendTag(stageCoordinateTag);
        }
        nbt.setTag("stageCoordinate", stageCoordinateNBT);
    }

    public void setCoordinate(Map<KeyValue<String, String>, Coordinate> stageCoordinate) {
        this.stageCoordinate = stageCoordinate;
        this.markDirty();
    }

    public void addCoordinate(KeyValue<String, String> key, Coordinate coordinate) {
        this.stageCoordinate.put(key, coordinate);
        this.markDirty();
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
        MapStorage storage = NarcissusFarewell.getServerInstance().worldServers[0].mapStorage;
        WorldStageData data = (WorldStageData) storage.loadData(WorldStageData.class, DATA_NAME);
        if (data == null) {
            data = new WorldStageData();
            storage.setData(DATA_NAME, data);
        }
        return data;
    }
}
