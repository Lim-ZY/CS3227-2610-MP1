package Timey.command;

import java.util.ArrayList;

import Timey.model.TimeyModel;

/** Lists saved fixed commute timings. */
public final class ListFixedTimingsCommand extends Command {
    @Override
    public CommandResult execute(TimeyModel model) {
        var timings = model.getFixedCommutes();
        if (timings.isEmpty()) {
            return CommandResult.message("You have no saved fixed timings.");
        }
        var messages = new ArrayList<String>();
        messages.add("Saved fixed timings:");
        for (int index = 0; index < timings.size(); index++) {
            var timing = timings.get(index);
            messages.add((index + 1) + ". " + timing.origin() + " → " + timing.destination() + " — "
                    + timing.duration().toMinutes() + " minutes");
        }
        return new CommandResult(messages);
    }
}
