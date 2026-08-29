package timey.ui.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

class DashboardCommandExecutionGateTest {
    @Test
    void execute_overlappingOperations_runsOneOperationAtATime() throws Exception {
        var gate = new DashboardCommandExecutionGate();
        var firstStarted = new CountDownLatch(1);
        var allowFirstToFinish = new CountDownLatch(1);
        var secondStarted = new AtomicBoolean();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> gate.execute(() -> {
                firstStarted.countDown();
                await(allowFirstToFinish);
                return "first";
            }));
            assertTrue(firstStarted.await(1, TimeUnit.SECONDS));
            var second = executor.submit(() -> gate.execute(() -> {
                secondStarted.set(true);
                return "second";
            }));

            assertFalse(secondStarted.get());
            allowFirstToFinish.countDown();

            assertEquals("first", first.get(1, TimeUnit.SECONDS));
            assertEquals("second", second.get(1, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Test operation was interrupted.", exception);
        }
    }
}
