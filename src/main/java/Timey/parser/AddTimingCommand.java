package Timey.parser;

import java.time.Duration;

/** A validated request to save a fixed commute duration. */
public record AddTimingCommand(String origin, String destination, Duration duration) {
    public AddTimingCommand {
        if (origin == null || origin.isBlank()) {
            throw new IllegalArgumentException("Origin must not be blank.");
        }
        if (destination == null || destination.isBlank()) {
            throw new IllegalArgumentException("Destination must not be blank.");
        }
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("Duration must be greater than zero.");
        }
    }
}
