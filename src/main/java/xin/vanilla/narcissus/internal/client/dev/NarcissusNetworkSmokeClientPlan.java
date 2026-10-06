package xin.vanilla.narcissus.internal.client.dev;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Pure wall-clock and server-phase gates used by the real client runner.
 */
final class NarcissusNetworkSmokeClientPlan {
    private static final long TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(300);
    private static final long STATE_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(120);
    private static final long UI_DURATION_NANOS = TimeUnit.SECONDS.toNanos(20);
    private static final java.util.regex.Pattern SAMPLE_INTERVAL = java.util.regex.Pattern.compile(
            "PASS server-sampling-window start-ms=(\\d+) end-ms=(\\d+)");

    private final String phase;
    private final long startedAt;
    private long stateStartedAt;
    private boolean sustainedReady;
    private boolean checkpointReady;
    private boolean finished;
    private long serverSampleStart;
    private long serverSampleEnd;

    NarcissusNetworkSmokeClientPlan(String phase, long now) {
        if (!"phase-one".equals(phase) && !"phase-two".equals(phase)) {
            throw new IllegalArgumentException("Unknown network smoke phase: " + phase);
        }
        this.phase = phase;
        startedAt = now;
        stateStartedAt = now;
    }

    void transition(long now) {
        stateStartedAt = now;
    }

    static boolean isCustomChannelReady(int ticksSinceLogin, boolean remoteModConfirmed) {
        return remoteModConfirmed && ticksSinceLogin >= 20;
    }

    boolean timedOut(long now) {
        return now - startedAt >= TIMEOUT_NANOS || now - stateStartedAt >= STATE_TIMEOUT_NANOS;
    }

    void accept(List<String> lines) {
        for (String line : lines) {
            if (line.startsWith("FAIL")) throw new IllegalStateException("Server reported " + line);
            java.util.regex.Matcher interval = SAMPLE_INTERVAL.matcher(line);
            if (interval.matches()) {
                serverSampleStart = Long.parseLong(interval.group(1));
                serverSampleEnd = Long.parseLong(interval.group(2));
            }
        }
        sustainedReady |= lines.contains("PASS sustained-ready");
        checkpointReady |= lines.stream().anyMatch(line -> line.equals("PASS final-checkpoint")
                || line.startsWith("PASS final-checkpoint "));
        finished |= lines.contains("FINISHED " + phase);
    }

    boolean readyForUi() {
        if (!"phase-one".equals(phase)) return false;
        requireServerSamplingWindow();
        return sustainedReady;
    }

    void requireSampleInterval(long start, long end) {
        if (serverSampleStart <= 0 || serverSampleEnd <= serverSampleStart
                || start < serverSampleStart || end <= start || end > serverSampleEnd) {
            throw new IllegalStateException("Client sample is not entirely inside the server sampling interval");
        }
    }

    void requireServerSamplingWindow() {
        if (finished) throw new IllegalStateException("Server finished before raw client UI report was staged");
    }

    boolean canFinish(boolean uiVerified, boolean rawWritten) {
        if (!finished) return false;
        if ("phase-one".equals(phase)) {
            if (!checkpointReady)
                throw new IllegalStateException("Server finished phase-one without PASS final-checkpoint");
            return uiVerified && rawWritten;
        }
        return true;
    }

    static boolean uiComplete(boolean samplerDone, boolean rawWritten, long elapsed, boolean verified) {
        if (samplerDone && !verified) {
            throw new IllegalStateException("Client Spark sampling ended before 20 completed content cycles");
        }
        return samplerDone && rawWritten && elapsed >= UI_DURATION_NANOS && verified;
    }
}
