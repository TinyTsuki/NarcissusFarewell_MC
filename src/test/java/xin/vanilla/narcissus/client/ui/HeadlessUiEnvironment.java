package xin.vanilla.narcissus.client.ui;

import org.junit.Rule;
import org.junit.rules.ExternalResource;
import xin.vanilla.banira.platform.BaniraPlatform;
import xin.vanilla.banira.platform.BaniraPlatforms;

public abstract class HeadlessUiEnvironment {
    @Rule
    public final ExternalResource environment = new ExternalResource() {
        private BaniraPlatform previous;

        @Override
        protected void before() {
            xin.vanilla.narcissus.test.NativeBootstrap.initialize();
            previous = BaniraPlatforms.get();
            BaniraPlatforms.install((BaniraPlatform) java.lang.reflect.Proxy.newProxyInstance(
                    BaniraPlatform.class.getClassLoader(), new Class<?>[]{BaniraPlatform.class},
                    (proxy, method, args) -> method.getName().equals("isClient") ? false : method.invoke(previous, args)));
        }

        @Override
        protected void after() {
            BaniraPlatforms.install(previous);
        }
    };
}
