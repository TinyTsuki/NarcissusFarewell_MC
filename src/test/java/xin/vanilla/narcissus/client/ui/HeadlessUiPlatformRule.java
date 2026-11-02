package xin.vanilla.narcissus.client.ui;

import org.junit.rules.ExternalResource;
import xin.vanilla.banira.platform.BaniraPlatform;
import xin.vanilla.banira.platform.BaniraPlatforms;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

public final class HeadlessUiPlatformRule extends ExternalResource {
    private BaniraPlatform original;

    @Override
    protected void before() {
        original = BaniraPlatforms.get();
        BaniraPlatforms.install((BaniraPlatform) Proxy.newProxyInstance(BaniraPlatform.class.getClassLoader(),
                new Class<?>[]{BaniraPlatform.class}, (proxy, method, args) -> {
                    if (method.getName().equals("isClient")) return false;
                    try {
                        return method.invoke(original, args);
                    } catch (InvocationTargetException failure) {
                        throw failure.getCause();
                    }
                }));
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Override
    protected void after() {
        if (original != null) BaniraPlatforms.install(original);
    }
}
