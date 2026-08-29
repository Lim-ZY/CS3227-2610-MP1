package timey.ui.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class DashboardFailureTextTest {
    @Test
    void commuteUpdate_internalFailure_returnsSafeUserMessage() {
        String message = DashboardFailureText.commuteUpdate(new IllegalStateException("Provider secret failure"));

        assertEquals("Timey could not complete that command. Your current plan has not changed. "
                + "Check your internet connection or saved data, then try again.", message);
        assertFalse(message.contains("Provider secret failure"));
    }
}
