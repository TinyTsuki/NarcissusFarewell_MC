package xin.vanilla.narcissus.data.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.test.NativeBootstrap;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class WorldStageStorageTest {
    private static HolderLookup.Provider registries;

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @BeforeClass
    public static void bootstrap() {
        NativeBootstrap.initialize();
        registries = VanillaRegistries.createLookup();
    }

    @Test
    public void legacyNetherCoordinatesImportInOrderAndRemainAfterReopen() throws Exception {
        Path world = temporary.newFolder().toPath();
        Path legacy = world.resolve("DIM-1/data/world_stage_data.dat");
        Path dataDir = world.resolve("dimensions/minecraft/the_nether/data");
        String dimension = "minecraft:the_nether";
        ListTag list = new ListTag();
        for (String name : List.of("north", "south")) {
            CompoundTag entry = new CompoundTag();
            entry.putString("key", dimension);
            entry.putString("value", name);
            entry.put("coordinate", new SafeWorldCoordinate(-20.25, 72, 18.75, Level.NETHER).toTag());
            list.add(entry);
        }
        CompoundTag payload = new CompoundTag();
        payload.put("stageCoordinate", list);
        CompoundTag root = new CompoundTag();
        root.putInt("DataVersion", 3955);
        root.put("data", payload);
        Files.createDirectories(legacy.getParent());
        NbtIo.writeCompressed(root, legacy);
        byte[] original = Files.readAllBytes(legacy);

        try (var storage = storage(dataDir)) {
            WorldStageData imported = WorldStageData.get(storage, world, Level.NETHER);
            assertEquals(List.of(new KeyValue<>(dimension, "north"), new KeyValue<>(dimension, "south")),
                    new ArrayList<>(imported.getStageCoordinate().keySet()));
            assertEquals(-20.25, imported.getCoordinate(dimension, "north").x(), 0);
            storage.saveAndJoin();
        }
        assertArrayEquals(original, Files.readAllBytes(legacy));
        Files.write(legacy, new byte[]{1, 2, 3});
        try (var storage = storage(dataDir)) {
            assertEquals(18.75, WorldStageData.get(storage, world, Level.NETHER).getCoordinate(dimension, "south").z(), 0);
        }
    }

    @Test
    public void unreadableLegacyFileIsKeptWithoutCreatingReplacement() throws Exception {
        Path world = temporary.newFolder().toPath();
        Path legacy = world.resolve("data/world_stage_data.dat");
        Path dataDir = world.resolve("dimensions/minecraft/overworld/data");
        Files.createDirectories(legacy.getParent());
        byte[] bytes = {1, 2, 3};
        Files.write(legacy, bytes);
        try (var storage = storage(dataDir)) {
            assertThrows(IllegalStateException.class, () -> WorldStageData.get(storage, world, Level.OVERWORLD));
        }
        assertArrayEquals(bytes, Files.readAllBytes(legacy));
        assertFalse(Files.exists(dataDir.resolve("minecraft/world_stage_data.dat")));
    }

    private static SavedDataStorage storage(Path directory) {
        return new SavedDataStorage(directory, DataFixers.getDataFixer(), registries);
    }
}
