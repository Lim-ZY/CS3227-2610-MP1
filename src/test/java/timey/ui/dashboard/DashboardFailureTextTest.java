package timey.ui.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class DashboardFailureTextTest {
    @Test
    void commuteUpdate_internalFailure_returnsSafeUserMessage() {
        String message = DashboardFailureText.commuteUpdate(new IllegalStateException("Provider secret failure"));

        assertEquals("Your previous plan is unchanged. Please try again.", message);
        assertFalse(message.contains("Provider secret failure"));
    }
}
