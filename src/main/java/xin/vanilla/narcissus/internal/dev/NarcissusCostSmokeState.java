package xin.vanilla.narcissus.internal.dev;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Explicit opt-in, exact peer acknowledgements and a bounded process lifetime.
 */
public final class NarcissusCostSmokeState {
    private final String phase;
    private final Path status, peer;
    private final long started;

    private NarcissusCostSmokeState(String phase, Path status, Path peer, long started) {
        this.phase = phase;
        this.status = status;
        this.peer = peer;
        this.started = started;
    }

    public static boolean enabled() {
        return Boolean.getBoolean("narcissus.costSmoke");
    }

    public static NarcissusCostSmokeState from(Properties values, String side, long now) {
        String phase = required(values, "phase");
        if (!phase.equals("integration") && !phase.equals("restart"))
            throw new IllegalArgumentException("Unknown cost smoke phase");
        if (!side.equals("server") && !side.equals("client"))
            throw new IllegalArgumentException("Unknown cost smoke side");
        Path status = Paths.get(required(values, "status")).toAbsolutePath().normalize();
        Path peer = Paths.get(required(values, "peer")).toAbsolutePath().normalize();
        if (status.equals(peer)) throw new IllegalArgumentException("Ambiguous status files");
        return new NarcissusCostSmokeState(phase, status, peer, now);
    }

    private static String required(Properties values, String name) {
        String result = values.getProperty("narcissus.costSmoke." + name, "").trim();
        if (result.isEmpty()) throw new IllegalArgumentException("Missing cost smoke " + name);
        return result;
    }

    public String phase() {
        return phase;
    }

    public void checkDeadline(long now) {
        if (now - started >= TimeUnit.SECONDS.toNanos(300))
            throw new IllegalStateException("Cost smoke exceeded 300 seconds");
    }

    public void append(String marker) {
        if (marker.indexOf('\n') >= 0 || marker.indexOf('\r') >= 0)
            throw new IllegalArgumentException("Multiline marker");
        try {
            Files.createDirectories(status.getParent());
            Files.write(status, (marker + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot write smoke status", error);
        }
    }

    public boolean peerHas(String marker) {
        try {
            if (!Files.isRegularFile(peer)) return false;
            if (Files.size(peer) > 131072) throw new IllegalStateException("Oversized smoke status");
            List<String> lines = Files.readAllLines(peer, StandardCharsets.UTF_8);
            for (String line : lines) if (line.startsWith("FAIL ")) throw new IllegalStateException("Peer " + line);
            return lines.contains(marker);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read peer smoke status", error);
        }
    }

    public boolean peerDisconnected(boolean playerPresent) {
        return peerHas("PASS client-disconnected") && !playerPresent;
    }

    public static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
