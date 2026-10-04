package xin.vanilla.narcissus.internal.client.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ConnectingScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.text.StringTextComponent;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.narcissus.data.cost.CostQuote;
import xin.vanilla.narcissus.data.cost.CostQuoteTarget;
import xin.vanilla.narcissus.internal.client.ClientCostQuotes;
import xin.vanilla.narcissus.internal.client.NarcissusClientSyncState;
import xin.vanilla.narcissus.internal.dev.NarcissusCostSmokeState;
import xin.vanilla.narcissus.network.packet.CostQuoteToServer;

import java.util.Optional;

import static xin.vanilla.narcissus.internal.dev.NarcissusCostSmokeState.require;

/** Real socket handshake and native packet handlers, with no desktop input automation. */
public final class NarcissusCostSmokeClientRunner {
    private static NarcissusCostSmokeClientRunner instance;
    private final NarcissusCostSmokeState state = NarcissusCostSmokeState.from(System.getProperties(), "client", System.nanoTime());
    private boolean connecting, connected, finished, disconnecting;
    private int quoteStage, ticks;
    private CostQuoteTarget lastTarget;

    private NarcissusCostSmokeClientRunner() { }
    public static void register() {
        if (NarcissusCostSmokeState.enabled()) instance = new NarcissusCostSmokeClientRunner();
    }
    public static void tick(Minecraft client) { if (instance != null) instance.run(client); }
    private void run(Minecraft client) {
        if (finished) return;
        try {
            state.checkDeadline(System.nanoTime());
            if (!connecting) {
                if (client.getOverlay() != null || ++ticks < 20) return;
                String host = System.getProperty("narcissus.costSmoke.host", "127.0.0.1");
                int port = Integer.getInteger("narcissus.costSmoke.port", 25577);
                ServerData server = new ServerData("Cost smoke", host + ":" + port, false);
                client.setCurrentServer(server); client.setScreen(new ConnectingScreen(client.screen, client, server));
                connecting = true; return;
            }
            boolean remote = client.player != null && client.level != null && client.getConnection() != null
                    && client.getConnection().getConnection().isConnected() && !client.getConnection().getConnection().isMemoryConnection()
                    && client.getSingleplayerServer() == null;
            if (disconnecting) {
                if (client.player != null || client.level != null) return;
                ClientCostQuotes quotes = NarcissusClientSyncState.costQuotes();
                require(quotes.size() == 0, "Native logout retained quote cache");
                if (lastTarget != null) require(!quotes.request(lastTarget, System.nanoTime() + 1_000_000_000L).isPresent(), "Native logout retained quote capability");
                state.append("PASS client-disconnected"); state.append("FINISHED " + state.phase());
                finished = true; client.stop(); return;
            }
            if (connected && !remote && !disconnecting) throw new IllegalStateException("Unexpected connection loss");
            if (!remote) return;
            if (!connected) { connected = true; state.append("PASS socket-login"); }
            if (state.phase().equals("restart")) {
                if (state.peerHas("PASS restart")) disconnect(client);
                return;
            }
            String marker = quoteStage == 0 ? "quote-home" : quoteStage == 1 ? "quote-custom" : "quote-reloaded";
            int amount = quoteStage == 0 ? 3 : 7;
            if (quoteStage < 3 && state.peerHas("READY " + marker)) {
                ClientCostQuotes quotes = NarcissusClientSyncState.costQuotes();
                CostQuoteTarget target = CostQuoteTarget.home(client.player.getUUID(), "minecraft:overworld", "CostHome");
                lastTarget = target;
                long now = System.nanoTime();
                Optional<CostQuote> result = quotes.cached(target, now);
                if (result.isPresent()) {
                    require(result.get().status() == CostQuote.Status.READY && result.get().amount().orElse(-1) == amount,
                            "Unexpected quote " + marker + ": " + result.get());
                    state.append("PASS " + marker); quoteStage++; quotes.closeView();
                } else quotes.request(target, now).ifPresent(request -> PacketUtils.sendPacketToServer(new CostQuoteToServer(request)));
            }
            if (state.peerHas("FINISHED integration")) disconnect(client);
        } catch (Throwable error) {
            state.append("FAIL client " + error); finished = true;
            org.apache.logging.log4j.LogManager.getLogger().error("Cost smoke client failed", error); client.stop();
        }
    }
    private void disconnect(Minecraft client) {
        disconnecting = true;
        client.getConnection().getConnection().disconnect(new StringTextComponent("Cost smoke complete"));
    }
}
