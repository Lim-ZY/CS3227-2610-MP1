package timey.command;

import java.time.Duration;

import timey.domain.transit.FixedCommute;
import timey.model.TimeyModel;

/** Saves a fixed commute duration for an origin and destination. */
public final class AddCommand extends Command {
    private final String origin;
    private final String destination;
    private final Duration duration;

    /** Creates a new AddCommand. */
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
        java.util.Optional<FixedCommute> previous = model.findFixedCommute(origin, destination);
        if (previous.isPresent() && previous.orElseThrow().duration().equals(duration)) {
            return new CommandResult(java.util.List.of(
                    "This route has already been saved for you! Do check it out",
                    "using `ls saved`."));
        }
        FixedCommute commute = model.saveFixedCommute(origin, destination, duration);
        String action = previous.isPresent() ? "Changed" : "Saved";
        return new CommandResult(java.util.List.of(
                action + " fixed timing from " + commute.origin() + " to " + commute.destination() + ": "
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
