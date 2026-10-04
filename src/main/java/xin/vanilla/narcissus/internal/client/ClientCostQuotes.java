package xin.vanilla.narcissus.internal.client;

import xin.vanilla.narcissus.data.cost.*;
import java.util.*;

/** Owner-thread display cache, never a payment authorization. */
public final class ClientCostQuotes {
    private static final long REFRESH = 500_000_000L, TIMEOUT = 3_000_000_000L;
    private final Thread owner = Thread.currentThread();
    private final LinkedHashMap<CostQuoteTarget, Entry> values = new LinkedHashMap<>(32, .75f, true);
    private UUID session;
    private long generation, sequence, sentAt;
    private boolean sent;
    private CostQuoteTarget selected;
    private CostQuoteRequest pending;

    public void capabilities(UUID sessionId, long generationId) {
        checkOwner(); Objects.requireNonNull(sessionId, "sessionId");
        if (generationId < 1) throw new IllegalArgumentException("Invalid quote generation");
        if (sessionId.equals(session) && generationId <= generation) return;
        if (!sessionId.equals(session)) clear(); else closeView();
        session = sessionId; generation = generationId;
    }
    public Optional<CostQuoteRequest> request(CostQuoteTarget target, long nowNanos) {
        checkOwner(); Objects.requireNonNull(target, "target");
        if (session == null) return Optional.empty();
        if (!target.equals(selected)) { selected = target; pending = null; }
        if (pending != null && elapsed(nowNanos, sentAt) >= TIMEOUT) pending = null;
        if (pending != null || cached(target, nowNanos).isPresent() || sent && elapsed(nowNanos, sentAt) < REFRESH) return Optional.empty();
        if (sequence == Long.MAX_VALUE) return Optional.empty();
        pending = new CostQuoteRequest(session, ++sequence, generation, target);
        sentAt = nowNanos; sent = true;
        return Optional.of(pending);
    }
    public boolean accept(CostQuote quote, long nowNanos) {
        checkOwner(); Objects.requireNonNull(quote, "quote");
        if (pending == null || !pending.equals(quote.request()) || !pending.target().equals(selected)) return false;
        if (elapsed(nowNanos, sentAt) >= TIMEOUT) { pending = null; return false; }
        values.put(selected, new Entry(quote, nowNanos)); pending = null;
        if (values.size() > 32) values.remove(values.keySet().iterator().next());
        return true;
    }
    public Optional<CostQuote> cached(CostQuoteTarget target, long nowNanos) {
        checkOwner(); Entry entry = values.get(target);
        if (entry == null || elapsed(nowNanos, entry.at) >= REFRESH) return Optional.empty();
        return Optional.of(entry.quote);
    }
    public void invalidate(CostQuoteTarget target) {
        checkOwner(); values.remove(target);
        if (target.equals(selected)) { selected = null; pending = null; }
    }
    public void closeView() { checkOwner(); selected = null; pending = null; values.clear(); }
    public void clear() { checkOwner(); closeView(); session = null; generation = 0; sequence = 0; sent = false; }
    public int size() { checkOwner(); return values.size(); }
    private static long elapsed(long now, long before) { return Math.max(0, now - before); }
    private void checkOwner() { if (Thread.currentThread() != owner) throw new IllegalStateException("Client quotes require owner thread"); }
    private static final class Entry {
        final CostQuote quote; final long at;
        Entry(CostQuote quote, long at) { this.quote = quote; this.at = at; }
    }
}
