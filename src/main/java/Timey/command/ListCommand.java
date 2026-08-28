package Timey.command;

import java.util.ArrayList;

import Timey.model.TimeyModel;

/** Lists saved commute timings. */
public final class ListCommand extends Command {
    @Override
    public CommandResult execute(TimeyModel model) {
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
}
