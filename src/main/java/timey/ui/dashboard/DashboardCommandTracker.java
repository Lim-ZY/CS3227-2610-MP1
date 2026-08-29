package timey.ui.dashboard;

/** Tracks which asynchronous dashboard command is still allowed to update the interface. */
final class DashboardCommandTracker {
    private long latestRequestId;
    private boolean isClosed;

    long startRequest() {
        if (isClosed) {
            throw new IllegalStateException("Dashboard command session has ended.");
        }
        return ++latestRequestId;
    }

    boolean isCurrent(long requestId) {
        return !isClosed && requestId == latestRequestId;
    }

    void close() {
        isClosed = true;
    }
}
