package xin.vanilla.narcissus.client.ui;

import org.junit.rules.ExternalResource;
import xin.vanilla.banira.platform.BaniraConfigHandle;
import xin.vanilla.banira.platform.BaniraConfigService;
import xin.vanilla.banira.platform.BaniraPlatform;
import xin.vanilla.banira.platform.BaniraPlatforms;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Paths;

final class UiTestPlatform extends ExternalResource {
    private Field platformField;
    private BaniraPlatform previous;

    @Override
    protected void before() throws Exception {
        platformField = BaniraPlatforms.class.getDeclaredField("platform");
        platformField.setAccessible(true);
        previous = (BaniraPlatform) platformField.get(null);
        BaniraConfigService config = new BaniraConfigService() {
            public <T> void register(Class<T> type, String modId) {
                throw new UnsupportedOperationException("UI fixture does not register configurations");
            }

            public <T> T view(Class<?> type, Class<T> view) {
                throw new IllegalStateException("No configuration registered in UI fixture");
            }

            public BaniraConfigHandle handle(Class<?> type) {
                return null;
            }
        };
        BaniraPlatforms.install((BaniraPlatform) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{BaniraPlatform.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "loaderType": return "test";
                        case "minecraftVersion": return "1.16.5";
                        case "isClient": return false;
                        case "isDedicatedServer":
                        case "isDevelopment": return true;
                        case "isModLoaded": return "narcissus_farewell".equals(args[0]) || "banira_codex".equals(args[0]);
                        case "modDisplayName": return args[0];
                        case "modIdFromMainClass": return args[0] == xin.vanilla.narcissus.NarcissusFarewell.class
                                ? "narcissus_farewell" : "banira_codex";
                        case "modMainClass": return "narcissus_farewell".equals(args[0])
                                ? xin.vanilla.narcissus.NarcissusComponent.class : xin.vanilla.banira.BaniraComponent.class;
                        case "configService": return config;
                        case "configDir": return Paths.get("build", "ui-test-config");
                        default: throw new UnsupportedOperationException(method.toString());
                    }
                }));
    }

    @Override
    protected void after() {
        try {
            platformField.set(null, previous);
        } catch (IllegalAccessException error) {
            throw new IllegalStateException("Could not restore the test platform", error);
        }
    }
}
