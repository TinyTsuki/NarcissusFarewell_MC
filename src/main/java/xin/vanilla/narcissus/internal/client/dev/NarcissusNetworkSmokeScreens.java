package xin.vanilla.narcissus.internal.client.dev;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.ConfigEditorScreen;
import xin.vanilla.banira.client.gui.widget.CollapsiblePanelWidget;
import xin.vanilla.banira.client.gui.widget.IWidget;
import xin.vanilla.banira.client.gui.widget.ScrollbarWidget;
import xin.vanilla.narcissus.config.ClientConfig;
import xin.vanilla.narcissus.data.PlayerAccess;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.internal.dev.NarcissusNetworkSmokeStatus;
import xin.vanilla.narcissus.screen.AccessListScreen;
import xin.vanilla.narcissus.screen.PlayerConfigScreen;
import xin.vanilla.narcissus.screen.WaypointScreen;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Dev-only wrappers measure the actual production renderers, never synthetic screenshots. */
public final class NarcissusNetworkSmokeScreens {
    private final BooleanSupplier sampling;
    private final Metrics waypoints;
    private final Metrics accessList;
    private final Metrics clientConfig;
    private final Metrics playerConfig;
    private final PlayerAccess accessFixture = new PlayerAccess();
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private Screen active;
    private Metrics activeMetrics;
    private PendingFrame pending;
    private boolean registered;
    private boolean closed;
    private static Method renderInputUpdate;

    NarcissusNetworkSmokeScreens(BooleanSupplier sampling) {
        if (!NarcissusNetworkSmokeStatus.enabled()) throw new IllegalStateException("Client smoke is disabled");
        this.sampling = sampling;
        waypoints = new Metrics("waypoints", sampling);
        accessList = new Metrics("access-list-local-fixture", sampling);
        clientConfig = new Metrics("client-config", sampling);
        playerConfig = new Metrics("player-config", sampling);
        for (int index = 0; index < 48; index++) {
            accessFixture.addBlackList(new UUID(0L, 10000L + index).toString());
        }
    }

    static boolean hasSyncedHomes(Minecraft client) {
        return client.player != null && PlayerTeleportData.getData(client.player).getHomeCoordinate().size() == 48;
    }

    void open(Minecraft client) {
        if (!hasSyncedHomes(client)) throw new IllegalStateException("UI workload requires 48 synchronized player homes");
        NeoForge.EVENT_BUS.register(this);
        registered = true;
        NarcissusNetworkSmokeStatus.append("INFO client-ui-data waypoints=server-sync homes=48"
                + " access-list=local-render-only black-entries=48 access-sentinel=separate-real-roundtrip");
        runCycle(client, 1);
    }

    boolean readyForCycle(Minecraft client) {
        requireActive(client);
        return activeMetrics.readyForCycle();
    }

    void runCycle(Minecraft client, int cycle) {
        if (!hasSyncedHomes(client)) throw new IllegalStateException("Synchronized player homes changed during UI workload");
        pending = null;
        int view = (cycle - 1) % 4;
        if (view == 0) {
            activeMetrics = waypoints;
            active = new WaypointView();
        } else if (view == 1) {
            activeMetrics = accessList;
            active = new AccessView();
        } else if (view == 2) {
            activeMetrics = clientConfig;
            active = new ClientConfigView();
        } else {
            activeMetrics = playerConfig;
            active = new PlayerConfigView();
        }
        ((BaniraScreen) active).previousScreen(null);
        activeMetrics.beginCycle(cycle);
        client.setScreen(active);
        expandPanels(((BaniraScreen) active).widgets());
        if (view < 2) {
            ScrollbarWidget bar = listScrollbar((BaniraScreen) active, view == 0
                    ? "active_waypoint_scrollbar" : "active_access_scrollbar");
            if (bar == null || bar.maxValue() <= 0) {
                throw new IllegalStateException("Populated UI list did not create a scrollable viewport");
            }
            bar.value(bar.maxValue() * (((cycle - 1) / 4) % 3) / 2.0D);
        }
    }

    private void requireActive(Minecraft client) {
        if (client.screen != active || client.getOverlay() != null) {
            throw new IllegalStateException("Smoke UI was replaced or covered before completion");
        }
        if (client.player == null || client.level == null || client.getSingleplayerServer() != null) {
            throw new IllegalStateException("Smoke UI is no longer attached to a remote world");
        }
    }

    private static void expandPanels(List<IWidget> widgets) {
        for (IWidget widget : widgets) {
            if (widget instanceof CollapsiblePanelWidget) ((CollapsiblePanelWidget) widget).expanded(true);
            expandPanels(widget.children());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void afterDraw(ScreenEvent.Render.Post event) {
        PendingFrame frame = pending;
        pending = null;
        if (closed || frame == null || event.getScreen() != active || frame.screen != active) return;
        requireActive(Minecraft.getInstance());
        frame.metrics.render(frame.wallNanos, frame.cpuNanos, frame.content, frame.sampled);
    }

    private void render(Screen screen, Metrics metrics, GuiGraphics graphics, int mouseX, int mouseY,
                        float partialTicks, int x, int y, BooleanSupplier content, RenderCall actualRender) {
        updateRenderInput((BaniraScreen) screen, x, y);
        try {
            boolean sampledAtStart = sampling.getAsBoolean();
            long cpuStartedAt = cpuTime();
            long startedAt = System.nanoTime();
            actualRender.render(graphics, x, y, partialTicks);
            long elapsed = System.nanoTime() - startedAt;
            long cpuEndedAt = cpuTime();
            long cpuElapsed = cpuStartedAt < 0 || cpuEndedAt < cpuStartedAt ? -1 : cpuEndedAt - cpuStartedAt;
            boolean sampledAtEnd = sampling.getAsBoolean();
            pending = new PendingFrame(screen, metrics, elapsed, cpuElapsed, content.getAsBoolean(),
                    sampledAtStart && sampledAtEnd);
        } finally {
            updateRenderInput((BaniraScreen) screen, mouseX, mouseY);
        }
    }

    // Dev-only runtime updater; replaying render-pre would clear deferred tooltips.
    private static void updateRenderInput(BaniraScreen screen, double mouseX, double mouseY) {
        try {
            Object state = screen.inputState();
            if (renderInputUpdate == null) {
                renderInputUpdate = state.getClass().getMethod("handleDrawScreenPre", double.class, double.class);
            }
            renderInputUpdate.invoke(state, mouseX, mouseY);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Unable to update smoke render input", error);
        }
    }

    private long cpuTime() {
        return threadBean.isCurrentThreadCpuTimeSupported() && threadBean.isThreadCpuTimeEnabled()
                ? threadBean.getCurrentThreadCpuTime() : -1L;
    }

    boolean verified() {
        return completeCoverage(waypoints.renderedCycles(), accessList.renderedCycles(),
                clientConfig.renderedCycles(), playerConfig.renderedCycles());
    }

    static boolean completeCoverage(int waypoints, int accessList, int clientConfig, int playerConfig) {
        return waypoints > 0 && accessList > 0 && clientConfig > 0 && playerConfig > 0
                && waypoints + accessList + clientConfig + playerConfig >= 20;
    }

    int completedCycles() {
        return waypoints.renderedCycles() + accessList.renderedCycles()
                + clientConfig.renderedCycles() + playerConfig.renderedCycles();
    }

    String summary() {
        return waypoints.summary() + " " + accessList.summary() + " " + clientConfig.summary() + " " + playerConfig.summary()
                + " waypoints-source=server-sync access-source=local-render-only access-tab=black"
                + " render-timing=screen.render-wall-and-thread-cpu excludes=screen-post-timing,gpu"
                + " frame-gate=render-start,render-end,screen-post config-save=false pixel-verification=false";
    }

    String diagnostics() {
        BaniraScreen screen = (BaniraScreen) active;
        ScrollbarWidget bar = listScrollbar(screen, "active_waypoint_scrollbar");
        return "active-view=" + active.getClass().getSimpleName() + " screen=" + active.width + 'x' + active.height
                + " input=" + screen.inputState().mouseX() + ',' + screen.inputState().mouseY()
                + " bar=" + (bar == null ? "none" : bar.absoluteX() + "," + bar.absoluteY()
                + "," + bar.bounds().width() + "," + bar.bounds().height() + ",offset=" + bar.value());
    }

    void close(Minecraft client) {
        if (closed) return;
        closed = true;
        pending = null;
        if (registered) NeoForge.EVENT_BUS.unregister(this);
        if (client.screen == active) client.setScreen(null);
    }

    private final class WaypointView extends WaypointScreen {
        private final Field hovered = contentField(WaypointScreen.class, "hoveredItem");

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            renderList(this, waypoints, hovered, "active_waypoint_scrollbar",
                    graphics, mouseX, mouseY, partialTicks, super::render);
        }
    }

    private final class AccessView extends AccessListScreen {
        private final Field hovered = contentField(AccessListScreen.class, "hoveredUuid");

        @Override
        protected void onInit() {
            PlayerTeleportData data = PlayerTeleportData.getData(Minecraft.getInstance().player);
            withRenderFixture(data::getAccess, data::setAccess, accessFixture, () -> super.onInit());
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            PlayerTeleportData data = PlayerTeleportData.getData(Minecraft.getInstance().player);
            // The real access renderer reads player data directly. Scope its fixture to this synchronous call.
            withRenderFixture(data::getAccess, data::setAccess, accessFixture, () ->
                    renderList(this, accessList, hovered, "active_access_scrollbar",
                            graphics, mouseX, mouseY, partialTicks, super::render));
        }
    }

    static <T> void withRenderFixture(Supplier<T> current, Consumer<T> install, T fixture, Runnable render) {
        T previous = current.get();
        install.accept(fixture);
        try {
            render.run();
        } finally {
            install.accept(previous);
        }
    }

    private void renderList(BaniraScreen screen, Metrics metrics, Field hovered, String scrollbarId,
                            GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, RenderCall actualRender) {
        ScrollbarWidget bar = listScrollbar(screen, scrollbarId);
        int x = bar == null ? mouseX : (int) bar.absoluteX() - 40;
        int y = bar == null ? mouseY : (int) bar.absoluteY() + 14;
        render(screen, metrics, graphics, mouseX, mouseY, partialTicks, x, y,
                () -> bar != null && x > 0 && x < screen.width && y > 0 && y < screen.height
                        && renderedListContent(hovered, screen), actualRender);
    }

    private static ScrollbarWidget listScrollbar(BaniraScreen screen, String id) {
        for (IWidget widget : screen.widgets()) {
            if (widget instanceof ScrollbarWidget && id.equals(widget.id())) return (ScrollbarWidget) widget;
        }
        return null;
    }

    // Read the production renderer's hovered row after super.render; do not inject private screen state.
    private static Field contentField(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Unable to observe actual list content", error);
        }
    }

    private static boolean renderedListContent(Field field, Object screen) {
        try {
            return field.get(screen) != null;
        } catch (IllegalAccessException error) {
            throw new IllegalStateException("Unable to observe rendered list content", error);
        }
    }

    private final class ClientConfigView extends ConfigEditorScreen {
        private ClientConfigView() {
            super(xin.vanilla.banira.api.BaniraConfigs.holder(ClientConfig.class), new ConfigEditorScreen.Args());
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            IWidget content = contentWidget(this, widgets(), false);
            renderEditor(this, clientConfig, content, graphics, mouseX, mouseY, partialTicks, super::render);
        }
    }

    private final class PlayerConfigView extends PlayerConfigScreen {
        private PlayerConfigView() {
            super(new PlayerConfigScreen.Args());
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            IWidget content = contentWidget(this, widgets(), true);
            renderEditor(this, playerConfig, content, graphics, mouseX, mouseY, partialTicks, super::render);
        }
    }

    private void renderEditor(Screen screen, Metrics metrics, IWidget content, GuiGraphics graphics,
                              int mouseX, int mouseY, float partialTicks, RenderCall actualRender) {
        int x = content == null ? mouseX : (int) (content.absoluteX() + content.bounds().width() / 2.0D);
        int y = content == null ? mouseY : (int) (content.absoluteY() + content.bounds().height() / 2.0D);
        render(screen, metrics, graphics, mouseX, mouseY, partialTicks, x, y, () -> content != null, actualRender);
    }

    private static IWidget contentWidget(Screen screen, List<IWidget> widgets, boolean player) {
        for (IWidget widget : widgets) {
            if (!widget.visible() || widget.bounds() == null) continue;
            String id = widget.id();
            boolean content = id != null && (player ? id.startsWith("tp_prefs_lbl_") : id.startsWith("lbl_"));
            double x = widget.absoluteX() + widget.bounds().width() / 2.0D;
            double y = widget.absoluteY() + widget.bounds().height() / 2.0D;
            boolean visible = widget.bounds().width() > 0 && widget.bounds().height() > 0
                    && x > 0 && x < screen.width && y > 40 && y < screen.height - 40;
            for (IWidget parent = widget.parent(); visible && parent != null; parent = parent.parent()) {
                if (parent instanceof CollapsiblePanelWidget && !((CollapsiblePanelWidget) parent).expanded()) visible = false;
                if (!parent.visible() || parent.bounds() == null || x < parent.absoluteX() || y < parent.absoluteY()
                        || x >= parent.absoluteX() + parent.bounds().width()
                        || y >= parent.absoluteY() + parent.bounds().height()) visible = false;
            }
            if (content && visible) return widget;
            IWidget child = contentWidget(screen, widget.children(), player);
            if (child != null) return child;
        }
        return null;
    }

    private interface RenderCall {
        void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks);
    }

    private static final class PendingFrame {
        private final Screen screen;
        private final Metrics metrics;
        private final long wallNanos;
        private final long cpuNanos;
        private final boolean content;
        private final boolean sampled;

        private PendingFrame(Screen screen, Metrics metrics, long wallNanos, long cpuNanos, boolean content, boolean sampled) {
            this.screen = screen;
            this.metrics = metrics;
            this.wallNanos = wallNanos;
            this.cpuNanos = cpuNanos;
            this.content = content;
            this.sampled = sampled;
        }
    }

    static final class Metrics {
        private final String view;
        private final BooleanSupplier sampling;
        private long frames;
        private long contentFrames;
        private long wallTotal;
        private long wallMax;
        private long cpuTotal;
        private long cpuMax;
        private long cpuSamples;
        private int contentCycle;
        private int lastRenderedCycle;
        private int renderedCycles;

        Metrics(String view, BooleanSupplier sampling) {
            this.view = view;
            this.sampling = sampling;
        }

        void beginCycle(int cycle) {
            contentCycle = cycle;
        }

        boolean readyForCycle() {
            return contentCycle > 0 && lastRenderedCycle == contentCycle;
        }

        int renderedCycles() {
            return renderedCycles;
        }

        void render(long wall, long cpu, boolean content, boolean sampledAtStart) {
            if (!sampledAtStart || !sampling.getAsBoolean()) return;
            frames++;
            wallTotal += wall;
            wallMax = Math.max(wallMax, wall);
            if (cpu >= 0) {
                cpuSamples++;
                cpuTotal += cpu;
                cpuMax = Math.max(cpuMax, cpu);
            }
            if (content) {
                contentFrames++;
                if (contentCycle > 0 && contentCycle != lastRenderedCycle) {
                    lastRenderedCycle = contentCycle;
                    renderedCycles++;
                }
            }
        }

        String summary() {
            return view + "-cycles=" + renderedCycles + " " + view + "-render-frames=" + frames
                    + " " + view + "-content-render-frames=" + contentFrames
                    + " " + view + "-render-wall-average-ns=" + (frames == 0 ? 0 : wallTotal / frames)
                    + " " + view + "-render-wall-max-ns=" + wallMax
                    + " " + view + "-render-cpu-samples=" + cpuSamples
                    + " " + view + "-render-cpu-average-ns=" + (cpuSamples == 0 ? -1 : cpuTotal / cpuSamples)
                    + " " + view + "-render-cpu-max-ns=" + (cpuSamples == 0 ? -1 : cpuMax);
        }
    }
}
