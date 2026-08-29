package timey.ui.dashboard;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DashboardCommandTrackerTest {
    @Test
    void startRequest_newerRequestMakesOlderRequestStale() {
        var tracker = new DashboardCommandTracker();
        long firstRequest = tracker.startRequest();
        long secondRequest = tracker.startRequest();

        assertFalse(tracker.isCurrent(firstRequest));
        assertTrue(tracker.isCurrent(secondRequest));
    }

    @Test
    void close_activeRequestMakesRequestStaleAndPreventsNewRequests() {
        var tracker = new DashboardCommandTracker();
        long request = tracker.startRequest();

        tracker.close();

        assertFalse(tracker.isCurrent(request));
        assertThrows(IllegalStateException.class, tracker::startRequest);
    }
}
