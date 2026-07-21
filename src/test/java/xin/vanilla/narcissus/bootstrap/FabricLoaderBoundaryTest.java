package xin.vanilla.narcissus.bootstrap;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** 约束 Fabric 分支只在内部入口和适配器中接触加载器 API。 */
public class FabricLoaderBoundaryTest {

    @Test
    public void fabricEntrypointsDelegateToStableBootstraps() throws Exception {
        String metadata = read("src/main/resources/fabric.mod.json");
        String commonEntry = read("src/main/java/xin/vanilla/narcissus/internal/fabric/FabricNarcissusEntry.java");
        String clientEntry = read("src/main/java/xin/vanilla/narcissus/internal/fabric/client/FabricNarcissusClientEntry.java");
        String main = read("src/main/java/xin/vanilla/narcissus/NarcissusFarewell.java");

        assertTrue(metadata.contains("FabricNarcissusEntry"));
        assertTrue(metadata.contains("FabricNarcissusClientEntry"));
        assertTrue(commonEntry.contains("NarcissusFarewell.bootstrapCommon();"));
        assertTrue(commonEntry.contains("FabricNarcissusGameEventAdapter.register();"));
        assertTrue(clientEntry.contains("NarcissusClientBootstrap.init();"));
        assertFalse(main.contains("NarcissusClientBootstrap"));
        assertFalse(main.contains("EnvironmentUtils.isClient()"));
    }

    @Test
    public void productionSourcesDoNotLeakForgeTypes() throws IOException {
        Path root = Paths.get("src", "main", "java");
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : (Iterable<Path>) files.filter(path -> path.toString().endsWith(".java"))::iterator) {
                String source = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                if (source.contains("net.minecraftforge") || source.contains("internal.forge")) {
                    violations.add(root.relativize(file).toString());
                }
            }
        }
        if (!violations.isEmpty()) {
            fail("Fabric production source contains Forge coupling: " + String.join(", ", violations));
        }
    }

    @Test
    public void loaderAdaptersOwnCommandsEventsAndWorldPersistence() throws Exception {
        String adapter = read("src/main/java/xin/vanilla/narcissus/internal/fabric/event/FabricNarcissusGameEventAdapter.java");
        String worldData = read("src/main/java/xin/vanilla/narcissus/data/world/WorldStageData.java");

        assertTrue(adapter.contains("CommandRegistrationCallback.EVENT.register"));
        assertTrue(adapter.contains("ServerPlayerEvents.AFTER_RESPAWN.register"));
        assertTrue(worldData.contains("extends SavedData"));
        assertFalse(worldData.contains("WorldCapabilityData"));
    }

    @Test
    public void overloadedTeleportMixinUsesTheCrossDimensionDescriptor() throws Exception {
        String mixin = read("src/main/java/xin/vanilla/narcissus/mixin/ServerPlayerTeleportMixin.java");

        assertTrue(mixin.contains("teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDFF)V"));
    }

    @Test
    public void livingHealthAccessorMatchesTheStaticVanillaField() throws Exception {
        String mixin = read("src/main/java/xin/vanilla/narcissus/mixin/LivingEntityInvoker.java");
        String utility = read("src/main/java/xin/vanilla/narcissus/util/NarcissusUtils.java");

        assertTrue(mixin.contains("static EntityDataAccessor<Float> narcissus$dataHealthId()"));
        assertFalse(utility.contains(").narcissus$dataHealthId()"));
    }

    private static String read(String relativePath) throws IOException {
        Path path = Paths.get(relativePath);
        assertTrue("Missing Fabric contract file: " + relativePath, Files.isRegularFile(path));
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
