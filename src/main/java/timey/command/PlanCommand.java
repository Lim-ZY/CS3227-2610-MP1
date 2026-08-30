package timey.command;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import timey.domain.transit.RouteAlternative;
import timey.model.TimeyModel;

/** Plans a commute to the requested destination and arrival time. */
public final class PlanCommand extends Command {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Duration OFFLINE_ESTIMATE_DURATION = Duration.ofHours(1);

    private final String origin;
    private final String destination;
    private final LocalTime arrivalTime;
    private final Duration buffer;

    /** Creates a new PlanCommand. */
    public PlanCommand(String origin, String destination, LocalTime arrivalTime, Duration buffer) {
        this.origin = normalizeLocation(origin, "Origin");
        this.destination = normalizeLocation(destination, "Destination");
        if (arrivalTime == null) {
            throw new IllegalArgumentException("Arrival time must be provided.");
        }
        if (buffer == null || buffer.isNegative()) {
            throw new IllegalArgumentException("Buffer must be zero or greater.");
        }
        this.arrivalTime = arrivalTime;
        this.buffer = buffer;
    }

    private static String normalizeLocation(String location, String locationType) {
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException(locationType + " must not be blank.");
        }
        if (location.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(locationType + " must not contain control characters.");
        }
        return location.strip();
    }

    @Override
    public CommandResult execute(TimeyModel model) {
        model.plan(this);
        var messages = new ArrayList<String>();
        messages.add("Got it! I have noted down your plan as follows:");
        messages.add("");
        messages.add("From: " + origin);
        messages.add("To: " + destination);
        messages.add("Target arrival: " + arrivalTime.format(TIME_FORMAT));
        messages.add("Personal buffer: " + buffer.toMinutes() + " minutes");
        messages.add("");
        messages.addAll(model.getPlanningMessages());
        if (model.getPendingPlan().isEmpty()) {
            return new CommandResult(messages);
        }
        messages.add("");
        messages.add("Here are your route alternatives:");
        for (int index = 0; index < model.getPendingAlternatives().size(); index++) {
            RouteAlternative route = model.getPendingAlternatives().get(index);
            if (model.isUsingFallbackEstimate() && route.name().equals("Offline estimate")) {
                messages.add((index + 1) + ". Offline estimate — " + OFFLINE_ESTIMATE_DURATION.toMinutes()
                        + " minutes total");
                continue;
            }
            messages.add((index + 1) + ". " + route.name() + " — " + route.totalDuration().toMinutes()
                    + " minutes total (walk " + route.walkingDuration().toMinutes() + " minutes, transit "
                    + route.transitDuration().toMinutes() + " minutes, " + route.transferCount()
                    + (route.transferCount() == 1 ? " transfer" : " transfers") + ")");
            route.steps().forEach(step -> messages.add("   - " + step.description() + " ("
                    + step.duration().toMinutes() + " minutes)"));
        }
        messages.add("");
        messages.add("Choose a route with: choose 1");
        messages.addAll(model.getRouteSelectionMessages());
        return new CommandResult(messages);
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    public Duration getBuffer() {
        return buffer;
    }
}
