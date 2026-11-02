package xin.vanilla.narcissus.client.ui;

import org.junit.rules.ExternalResource;

public final class HeadlessUiPlatformRule extends ExternalResource {
    private final UiTestPlatform fixture = new UiTestPlatform();

    @Override
    protected void before() throws Exception {
        fixture.before();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (RuntimeException | Error failure) {
            fixture.after();
            throw failure;
        }
    }

    @Override
    protected void after() {
        fixture.after();
    }
}
