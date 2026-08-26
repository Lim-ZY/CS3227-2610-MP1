package Timey.domain.alert;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import Timey.command.PlanCommand;
import Timey.domain.transit.RouteAlternative;

class DepartureCalculatorTest {
    private final DepartureCalculator calculator = new DepartureCalculator();

    @Test
    void calculate_travelTimeAndBuffer_returnsDepartureTime() {
        PlanCommand plan = new PlanCommand("COM3", "VivoCity", LocalTime.of(18, 30), Duration.ofMinutes(10));
        RouteAlternative route = new RouteAlternative(
                "Fastest Transit", Duration.ofMinutes(8), Duration.ofMinutes(35), 1);

        DepartureRecommendation result = calculator.calculate(plan, route);

        assertEquals(LocalTime.of(17, 37), result.departureTime());
        assertEquals(Duration.ofMinutes(43), result.travelDuration());
        assertEquals(Duration.ofMinutes(10), result.buffer());
    }

    @Test
    void calculate_departureBeforeMidnight_returnsPreviousDayTime() {
        PlanCommand plan = new PlanCommand("COM3", "VivoCity", LocalTime.of(0, 30), Duration.ofMinutes(10));
        RouteAlternative route = new RouteAlternative(
                "Late route", Duration.ofMinutes(15), Duration.ofMinutes(20), 0);

        DepartureRecommendation result = calculator.calculate(plan, route);

        assertEquals(LocalTime.of(23, 45), result.departureTime());
    }
}
