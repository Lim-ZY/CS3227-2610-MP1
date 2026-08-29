package timey.command;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import timey.model.TimeyModel;

/** Displays the departure reminders active in the current Timey session. */
public final class RemindersCommand extends Command {
    private static final DateTimeFormatter REMINDER_TIME_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm");

    @Override
    public CommandResult execute(TimeyModel model) {
        var reminders = model.getScheduledReminders();
        if (reminders.isEmpty()) {
            return CommandResult.message("You have no active departure reminders.");
        }

        var messages = new ArrayList<String>();
        messages.add("Active departure reminders:");
        for (int index = 0; index < reminders.size(); index++) {
            var reminder = reminders.get(index);
            messages.add((index + 1) + ". " + REMINDER_TIME_FORMAT
                    .format(reminder.triggerAt().atZone(model.getClock().getZone())) + " — " + reminder.message());
        }
        return new CommandResult(messages);
    }
}
