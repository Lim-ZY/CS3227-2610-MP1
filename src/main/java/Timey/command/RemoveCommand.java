package Timey.command;

import Timey.model.TimeyModel;

/** Removes a saved commute timing by its one-based list number. */
public final class RemoveCommand extends Command {
    private final Integer timingNumber;

    public RemoveCommand(Integer timingNumber) {
        this.timingNumber = timingNumber;
    }

    public Integer getTimingNumber() {
        return timingNumber;
    }

    @Override
    public CommandResult execute(TimeyModel model) {
        if (timingNumber == null) {
            return CommandResult.message("Remove a saved timing by number, for example: rm 1");
        }
        var timings = model.getFixedCommutes();
        if (timingNumber < 1 || timingNumber > timings.size()) {
            return CommandResult.message("No saved timing numbered " + timingNumber + ".");
        }
        var timing = timings.get(timingNumber - 1);
        model.removeFixedCommute(timing);
        return CommandResult.message(
                "Removed saved timing from " + timing.origin() + " to " + timing.destination() + ".");
    }
}
