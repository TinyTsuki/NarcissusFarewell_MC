package xin.vanilla.narcissus.data.world;

import lombok.Getter;
import lombok.NonNull;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.WorldCapabilityData;
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
public class WorldStageData extends WorldCapabilityData {
    private static final String DATA_NAME = "world_stage_data";

    // dimension:name coordinate
    private Map<KeyValue<String, String>, Coordinate> stageCoordinate = new LinkedHashMap<>();

    public WorldStageData() {
        super(DATA_NAME);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        this.stageCoordinate = new LinkedHashMap<>();
        NBTTagList stageCoordinateNBT = nbt.getTagList("stageCoordinate", 10);
        Map<KeyValue<String, String>, Coordinate> stageCoordinate = new HashMap<>();
        for (int i = 0; i < stageCoordinateNBT.tagCount(); i++) {
            NBTTagCompound stageCoordinateTag = stageCoordinateNBT.getCompoundTagAt(i);
            stageCoordinate.put(new KeyValue<>(stageCoordinateTag.getString("key"), stageCoordinateTag.getString("value")),
                    Coordinate.readFromNBT(stageCoordinateTag.getCompoundTag("coordinate")));
        }
        this.setCoordinate(stageCoordinate);
    }

    @Override
    @NonNull
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList stageCoordinateNBT = new NBTTagList();
        for (Map.Entry<KeyValue<String, String>, Coordinate> entry : this.getStageCoordinate().entrySet()) {
            NBTTagCompound stageCoordinateTag = new NBTTagCompound();
            stageCoordinateTag.setString("key", entry.getKey().getKey());
            stageCoordinateTag.setString("value", entry.getKey().getValue());
            stageCoordinateTag.setTag("coordinate", entry.getValue().writeToNBT());
            stageCoordinateNBT.appendTag(stageCoordinateTag);
        }
        nbt.setTag("stageCoordinate", stageCoordinateNBT);
        return nbt;
    }

    public void setCoordinate(Map<KeyValue<String, String>, Coordinate> stageCoordinate) {
        this.stageCoordinate = stageCoordinate;
        super.markDirty();
    }

    public void addCoordinate(KeyValue<String, String> key, Coordinate coordinate) {
        this.stageCoordinate.put(key, coordinate);
        super.markDirty();
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
        return get(NarcissusFarewell.getServerInstance().worlds[0]);
    }

    public static WorldStageData get(EntityPlayerMP player) {
        return get(player.getServerWorld());
    }

    public static WorldStageData get(WorldServer world) {
        WorldStageData stageData = (WorldStageData) world.loadData(WorldStageData.class, DATA_NAME);
        if (stageData == null) {
            stageData = new WorldStageData();
            world.setData(DATA_NAME, stageData);
        }
        return stageData;
    }
}
