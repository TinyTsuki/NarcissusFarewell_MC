package xin.vanilla.narcissus.data.world;

import com.mojang.serialization.Codec;
import lombok.Getter;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.SavedDataStorage;
import xin.vanilla.banira.api.BaniraServer;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.WaypointOrder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 世界驿站数据
 */
@Getter
public class WorldStageData extends SavedData {
    private static final String DATA_NAME = "world_stage_data";
    private static final Codec<WorldStageData> CODEC = CompoundTag.CODEC.xmap(WorldStageData::load, WorldStageData::save);
    private static final SavedDataType<WorldStageData> TYPE = new SavedDataType<>(
            Identifier.withDefaultNamespace(DATA_NAME), WorldStageData::new, CODEC, DataFixTypes.SAVED_DATA_MAP_DATA);

    // dimension:name coordinate
    private Map<KeyValue<String, String>, SafeWorldCoordinate> stageCoordinate = new LinkedHashMap<>();

    public WorldStageData() {
    }

    static WorldStageData load(CompoundTag nbt) {
        WorldStageData data = new WorldStageData();
        ListTag stageCoordinateNBT = nbt.getListOrEmpty("stageCoordinate");
        Map<KeyValue<String, String>, SafeWorldCoordinate> stageCoordinate = new LinkedHashMap<>();
        for (int i = 0; i < stageCoordinateNBT.size(); i++) {
            CompoundTag stageCoordinateTag = stageCoordinateNBT.getCompoundOrEmpty(i);
            stageCoordinate.put(new KeyValue<>(stageCoordinateTag.getStringOr("key", ""), stageCoordinateTag.getStringOr("value", "")),
                    SafeWorldCoordinate.fromTag(stageCoordinateTag.getCompoundOrEmpty("coordinate")));
        }
        data.stageCoordinate = stageCoordinate;
        return data;
    }

    private CompoundTag save() {
        CompoundTag nbt = new CompoundTag();
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
        this.stageCoordinate = WaypointOrder.prepend(key, coordinate, this.stageCoordinate);
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
        return get(player.level());
    }

    public static WorldStageData get(ServerLevel world) {
        return get(world.getDataStorage(), world.getServer().getWorldPath(LevelResource.ROOT), world.dimension());
    }

    static WorldStageData get(SavedDataStorage storage, Path root, ResourceKey<Level> dimension) {
        WorldStageData existing = storage.get(TYPE);
        if (existing != null) return existing;

        Path dataDir = DimensionType.getStorageFolder(dimension, root).resolve("data");
        Path current = TYPE.id().withSuffix(".dat").resolveAgainst(dataDir);
        if (!Files.notExists(current, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("Cannot read waypoint storage: " + current);
        }

        Path flatFile = dataDir.resolve(DATA_NAME + ".dat");
        Path legacy = Files.notExists(flatFile, LinkOption.NOFOLLOW_LINKS)
                ? legacyDataFile(root, dimension, flatFile) : flatFile;
        if (Files.notExists(legacy, LinkOption.NOFOLLOW_LINKS)) return storage.computeIfAbsent(TYPE);

        try {
            CompoundTag tag = storage.readTagFromDisk(legacy, TYPE.dataFixType(),
                    SharedConstants.getCurrentVersion().dataVersion().version());
            WorldStageData imported = load(tag.getCompound("data")
                    .orElseThrow(() -> new IOException("Missing waypoint data in " + legacy)));
            imported.setDirty();
            storage.set(TYPE, imported);
            return imported;
        } catch (IOException | ReportedNbtException e) {
            throw new IllegalStateException("Cannot read legacy waypoint storage: " + legacy, e);
        }
    }

    private static Path legacyDataFile(Path root, ResourceKey<Level> dimension, Path flatFile) {
        if (Level.OVERWORLD.equals(dimension)) return root.resolve("data").resolve(flatFile.getFileName());
        if (Level.NETHER.equals(dimension)) return root.resolve("DIM-1/data").resolve(flatFile.getFileName());
        if (Level.END.equals(dimension)) return root.resolve("DIM1/data").resolve(flatFile.getFileName());
        return flatFile;
    }
}
