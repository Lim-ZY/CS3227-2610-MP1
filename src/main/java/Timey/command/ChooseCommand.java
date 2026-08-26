package Timey.command;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import Timey.domain.alert.DepartureRecommendation;
import Timey.domain.alert.ScheduledDepartureReminder;
import Timey.model.RouteSelectionResult;
import Timey.model.TimeyModel;

/** Selects one route alternative from the pending plan. */
public final class ChooseCommand extends Command {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter REMINDER_TIME_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm");

    private final Integer routeNumber;

    public ChooseCommand(Integer routeNumber) {
        this.routeNumber = routeNumber;
    }

    @Override
    public CommandResult execute(TimeyModel model) {
        RouteSelectionResult result = model.selectRoute(routeNumber);
        return switch (result.status()) {
        case NO_PLAN -> CommandResult.message("Please create a plan before choosing a route.");
        case MISSING_NUMBER -> CommandResult.message("Choose a route by number, for example: choose 1");
        case INVALID_NUMBER -> CommandResult.message("Please choose a route between 1 and "
                + result.alternativeCount() + ".");
        case LEAVE_NOW -> recommendationResult(result.recommendation().orElseThrow(), null, model);
        case REMINDER_SCHEDULED -> recommendationResult(result.recommendation().orElseThrow(),
                result.reminder().orElseThrow(), model);
        };
    }

    private CommandResult recommendationResult(DepartureRecommendation recommendation,
            ScheduledDepartureReminder reminder, TimeyModel model) {
        var messages = new ArrayList<String>();
        messages.add("Great choice! Here is your departure plan:");
        messages.add("");
        messages.add("Chosen route: " + recommendation.routeName());
        messages.add("Total travel time: " + recommendation.travelDuration().toMinutes() + " minutes");
        messages.add("Personal buffer: " + recommendation.buffer().toMinutes() + " minutes");
        messages.add("Recommended departure: " + recommendation.departureTime().format(TIME_FORMAT));
        messages.add("");
        messages.add("Please leave your desk by " + recommendation.departureTime().format(TIME_FORMAT) + ".");
        if (reminder == null) {
            messages.add("You have to leave now to stay on time! Good luck!");
        } else {
            messages.add("Departure reminder automatically set for "
                    + REMINDER_TIME_FORMAT.format(reminder.triggerAt().atZone(model.getClock().getZone())) + ".");
        }
        return new CommandResult(messages);
    }
}
