package Timey.domain.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class RouteAlternativeTest {
    @Test
    void calculatesTotalDurationFromWalkingAndTransitLegs() {
        RouteAlternative route = new RouteAlternative(
                "Fastest Transit", Duration.ofMinutes(8), Duration.ofMinutes(35), 1);

        assertEquals(Duration.ofMinutes(43), route.totalDuration());
    }
}
