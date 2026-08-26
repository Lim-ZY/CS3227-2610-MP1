package Timey.command;

import java.time.Duration;

import Timey.domain.transit.FixedCommute;
import Timey.model.TimeyModel;

/** Saves a fixed commute duration for an origin and destination. */
public final class AddCommand extends Command {
    private final String origin;
    private final String destination;
    private final Duration duration;

    public AddCommand(String origin, String destination, Duration duration) {
        if (origin == null || origin.isBlank()) {
            throw new IllegalArgumentException("Origin must not be blank.");
        }
        if (destination == null || destination.isBlank()) {
            throw new IllegalArgumentException("Destination must not be blank.");
        }
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("Duration must be greater than zero.");
        }
        this.origin = origin;
        this.destination = destination;
        this.duration = duration;
    }

    @Override
    public CommandResult execute(TimeyModel model) {
        FixedCommute commute = model.saveFixedCommute(origin, destination, duration);
        return new CommandResult(java.util.List.of(
                "Saved fixed timing from " + commute.origin() + " to " + commute.destination() + ": "
                        + commute.duration().toMinutes() + " minutes.",
                "It will appear as a route option in your next matching plan."));
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public Duration getDuration() {
        return duration;
    }
}
