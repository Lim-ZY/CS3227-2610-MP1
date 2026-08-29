package timey.domain.alert;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class SavedPlanTest {
    private static final LocalDate DATE = LocalDate.of(2026, 8, 29);
    private static final LocalTime ARRIVAL = LocalTime.of(17, 0);
    private static final LocalTime LEAVE_BY = LocalTime.of(15, 50);

    @Test
    void constructor_completePlan_createsPlan() {
        assertDoesNotThrow(() -> new SavedPlan(DATE, ARRIVAL, "Admiralty MRT", "COM3", LEAVE_BY));
    }

    @Test
    void constructor_blankOrigin_rejectsPlan() {
        assertThrows(IllegalArgumentException.class, () -> new SavedPlan(DATE, ARRIVAL, " ", "COM3", LEAVE_BY));
    }

    @Test
    void constructor_missingLeaveBy_rejectsPlan() {
        assertThrows(NullPointerException.class, () -> new SavedPlan(DATE, ARRIVAL, "Admiralty MRT", "COM3", null));
    }
}
