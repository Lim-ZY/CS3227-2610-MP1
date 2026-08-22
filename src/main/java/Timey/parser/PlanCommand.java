package Timey.parser;

import java.time.Duration;
import java.time.LocalTime;

/** A validated request to plan a commute. */
public record PlanCommand(String origin, String destination, LocalTime arrivalTime, Duration buffer) {
    public PlanCommand {
        if (origin == null || origin.isBlank()) {
            throw new IllegalArgumentException("Origin must not be blank.");
        }
        if (destination == null || destination.isBlank()) {
            throw new IllegalArgumentException("Destination must not be blank.");
        }
        if (arrivalTime == null) {
            throw new IllegalArgumentException("Arrival time must be provided.");
        }
        if (buffer == null || buffer.isNegative()) {
            throw new IllegalArgumentException("Buffer must be zero or greater.");
        }
    }
}
