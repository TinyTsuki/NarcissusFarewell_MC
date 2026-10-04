package xin.vanilla.narcissus.search;

public final class SearchExecutionSettings {
    private final double timeBudgetMs;
    private final int maxConcurrentSearches;
    private final int timeoutSeconds;

    public SearchExecutionSettings(double timeBudgetMs, int maxConcurrentSearches, int timeoutSeconds) {
        if (!Double.isFinite(timeBudgetMs) || timeBudgetMs < .1 || timeBudgetMs > 10
                || maxConcurrentSearches < 1 || maxConcurrentSearches > 256
                || timeoutSeconds < 1 || timeoutSeconds > 300) {
            throw new IllegalArgumentException("Invalid search execution settings");
        }
        this.timeBudgetMs = timeBudgetMs;
        this.maxConcurrentSearches = maxConcurrentSearches;
        this.timeoutSeconds = timeoutSeconds;
    }

    public double timeBudgetMs() { return timeBudgetMs; }
    public int maxConcurrentSearches() { return maxConcurrentSearches; }
    public int timeoutSeconds() { return timeoutSeconds; }
}
