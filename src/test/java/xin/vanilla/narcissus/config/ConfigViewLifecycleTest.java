package xin.vanilla.narcissus.config;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigValueStore;
import xin.vanilla.banira.internal.fabric.config.FabricBaniraConfigService;
import xin.vanilla.banira.platform.BaniraConfigService;
import xin.vanilla.banira.platform.BaniraPlatform;
import xin.vanilla.banira.platform.BaniraPlatforms;

import java.lang.reflect.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class ConfigViewLifecycleTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();
    private BaniraPlatform previous;

    @BeforeClass
    public static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Before
    public void rememberPlatform() {
        previous = BaniraPlatforms.get();
    }

    @After
    public void restorePlatform() {
        BaniraPlatforms.install(previous);
    }

    @Test
    public void retainedUnsafeRulesFollowRealFileReloadAndRebinding() throws Exception {
        verify(false);
    }

    @Test
    public void retainedClientCategoryFollowsRealFileReloadAndRebinding() throws Exception {
        verify(true);
    }

    private void verify(boolean client) throws Exception {
        Path directory = temporary.newFolder().toPath();
        install(directory, FabricBaniraConfigService.INSTANCE);
        Class<?> type = client ? ClientConfig.class : CommonConfig.class;
        String key = client ? "client.syncHomeMapWaypoint" : "base.safeTeleport.unsafeBlocks";
        FabricBaniraConfigService.INSTANCE.register(type, "narcissus_farewell");
        ConfigHolder original = holder(type);
        Supplier<Object> read;
        Consumer<Object> write;
        if (client) {
            ClientConfigView.ClientView retained = ClientConfigView.get().client();
            read = retained::syncHomeMapWaypoint;
            write = value -> retained.syncHomeMapWaypoint((Boolean) value);
        } else {
            CommonConfigView.BaseView.SafeTeleportView retained = CommonConfigView.get().base().safeTeleport();
            read = retained::unsafeBlocks;
            write = value -> retained.unsafeBlocks((List<String>) value);
        }
        Object initial = read.get();
        Path file = directory.resolve(original.getConfigName() + ".toml");
        for (int i = 0; i < 20; i++) {
            Object changed = client ? i % 2 == 0
                    : Arrays.asList("tick, clazz -> tick >= " + (i + 5), "minecraft:arrow");
            ConfigValueStore external = disk(file, original);
            external.set(key, changed);
            external.save();
            FabricBaniraConfigService.INSTANCE.register(type, "narcissus_farewell");
            assertNotSame(original, holder(type));
            assertEquals(changed, read.get());
            assertEquals(initial, original.get(key));
        }
        Object saved = client ? false : Collections.singletonList("resource -> resource != 'minecraft:air'");
        write.accept(saved);
        ConfigHolder latest = holder(type);
        latest.save();
        assertEquals(saved, disk(file, latest).get(key));
        ConfigBaselineFixture.bind(type, null);
        assertEquals(initial, read.get());
        write.accept(initial);
        assertEquals(saved, latest.get(key));
        install(directory, FabricBaniraConfigService.INSTANCE);
        assertEquals(saved, read.get());
    }

    @Test
    public void generatedViewsRetainFluentAccessorsWithoutExposingRootFields() throws Exception {
        ConfigBaselineFixture common = new ConfigBaselineFixture(CommonConfig.class);
        common.bind(CommonConfig.class);
        CommonConfigView.BaseView.SafeTeleportView safe = CommonConfigView.get().base().safeTeleport();
        List<String> rules = Collections.singletonList("a, b -> true");
        assertSame(safe, safe.unsafeBlocks(rules));
        assertEquals(rules, safe.unsafeBlocks());
        assertEquals(rules, common.holder.get("base.safeTeleport.unsafeBlocks"));
        ConfigBaselineFixture local = new ConfigBaselineFixture(ClientConfig.class);
        local.bind(ClientConfig.class);
        ClientConfigView.ClientView client = ClientConfigView.get().client();
        assertSame(client, client.syncHomeMapWaypoint(false));
        assertFalse(client.syncHomeMapWaypoint());
        assertEquals(Boolean.FALSE, local.holder.get("client.syncHomeMapWaypoint"));
        for (Class<?> root : Arrays.asList(CommonConfig.class, ClientConfig.class)) {
            for (Field field : root.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                for (Method method : root.getDeclaredMethods()) {
                    assertFalse("Declaration field exposed: " + field.getName(),
                            method.getName().equals(field.getName()) && !Modifier.isStatic(method.getModifiers()));
                }
            }
        }
    }

    private static ConfigHolder holder(Class<?> type) {
        return (ConfigHolder) FabricBaniraConfigService.INSTANCE.handle(type);
    }

    private static ConfigValueStore disk(Path path, ConfigHolder holder) throws Exception {
        Class<?> type = Class.forName("xin.vanilla.banira.internal.fabric.config.FabricConfigValueStore");
        Constructor<?> constructor = type.getDeclaredConstructor(Path.class, List.class);
        constructor.setAccessible(true);
        return (ConfigValueStore) constructor.newInstance(path, holder.getDescriptors());
    }

    private static void install(Path directory, BaniraConfigService service) {
        BaniraPlatforms.install((BaniraPlatform) Proxy.newProxyInstance(
                ConfigViewLifecycleTest.class.getClassLoader(), new Class<?>[]{BaniraPlatform.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("configService")) return service;
                    if (method.getName().equals("configDir")) return directory;
                    throw new UnsupportedOperationException(method.toString());
                }));
    }
}
