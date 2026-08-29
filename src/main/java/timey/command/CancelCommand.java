package timey.command;

import timey.model.TimeyModel;

/** Cancels an active departure reminder by its one-based list number. */
public final class CancelCommand extends Command {
    private final Integer reminderNumber;

    public CancelCommand(Integer reminderNumber) {
        this.reminderNumber = reminderNumber;
    }

    public Integer getReminderNumber() {
        return reminderNumber;
    }

    @Override
    public CommandResult execute(TimeyModel model) {
        if (reminderNumber == null) {
            return CommandResult.message("Cancel a reminder by number, for example: cancel 1");
        }
        if (model.cancelReminder(reminderNumber)) {
            return CommandResult.message("Cancelled departure reminder " + reminderNumber + ".");
        }
        return CommandResult.message("No active departure reminder numbered " + reminderNumber + ".");
    }
}
