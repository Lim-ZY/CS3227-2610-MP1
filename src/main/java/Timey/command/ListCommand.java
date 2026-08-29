package Timey.command;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import Timey.model.TimeyModel;

/** Lists saved commute timings or future saved plans. */
public final class ListCommand extends Command {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-uuuu");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmm");

    /** The collection to list. */
    public enum ListType {
        SAVED,
        PLANS
    }

    private final ListType listType;

    public ListCommand(ListType listType) {
        this.listType = listType;
    }

    @Override
    public CommandResult execute(TimeyModel model) {
        if (listType == ListType.PLANS) {
            return listPlans(model);
        }
        return listSavedTimings(model);
    }

    private CommandResult listSavedTimings(TimeyModel model) {
        var timings = model.getFixedCommutes();
        if (timings.isEmpty()) {
            return CommandResult.message("You have no saved timings.");
        }
        var messages = new ArrayList<String>();
        messages.add("Saved timings:");
        for (int index = 0; index < timings.size(); index++) {
            var timing = timings.get(index);
            messages.add((index + 1) + ". " + timing.origin() + " → " + timing.destination() + " — "
                    + timing.duration().toMinutes() + " minutes");
        }
        return new CommandResult(messages);
    }

    private CommandResult listPlans(TimeyModel model) {
        model.pruneExpiredPlans();
        var plans = model.getSavedPlans();
        if (plans.isEmpty()) {
            return CommandResult.message("You have no saved plans.");
        }
        var messages = new ArrayList<String>();
        messages.add("Saved plans:");
        for (int index = 0; index < plans.size(); index++) {
            var plan = plans.get(index);
            messages.add((index + 1) + ". " + DATE_FORMAT.format(plan.date()) + " | "
                    + TIME_FORMAT.format(plan.arrivalTime()) + " | " + plan.origin() + " → " + plan.destination()
                    + " | leave by " + TIME_FORMAT.format(plan.leaveBy()));
        }
        return new CommandResult(messages);
    }
}
