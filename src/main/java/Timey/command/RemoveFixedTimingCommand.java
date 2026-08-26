package Timey.command;

import Timey.model.TimeyModel;

/** Removes a saved fixed commute timing by its one-based list number. */
public final class RemoveFixedTimingCommand extends Command {
    private final Integer timingNumber;

    public RemoveFixedTimingCommand(Integer timingNumber) {
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
            return CommandResult.message("No saved fixed timing numbered " + timingNumber + ".");
        }
        var timing = timings.get(timingNumber - 1);
        model.removeFixedCommute(timing);
        return CommandResult.message("Removed fixed timing from " + timing.origin() + " to " + timing.destination() + ".");
    }
}
