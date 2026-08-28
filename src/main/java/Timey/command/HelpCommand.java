package Timey.command;

import java.util.List;

import Timey.model.TimeyModel;

/** Displays the commands available in the Timey command-line interface. */
public final class HelpCommand extends Command {
    @Override
    public CommandResult execute(TimeyModel model) {
        return new CommandResult(List.of(
                "I've got your back. Here is the list of commands you can use for me to help you.",
                "",
                "----------------",
                "Plan a commute",
                "----------------",
                "plan /from \"<origin>\" /to \"<destination>\" /by HHmm [/buf Nm]",
                "└─> Plans a commute and shows available route alternatives.",
                "",
                "choose <route-number>",
                "└─> Selects a route and schedules a departure reminder if needed.",
                "    (Only available after using `plan`)",
                "",
                "----------------",
                "Manage reminders",
                "----------------",
                "reminders",
                "└─> Lists all active departure reminders.",
                "",
                "cancel <reminder-number>",
                "└─> Cancels an active departure reminder.",
                "",
                "--------------------",
                "Manage saved timings",
                "--------------------",
                "add /from \"<origin>\" /to \"<destination>\" /dur <duration>",
                "└─> Saves a commute duration for a frequently used route.",
                "",
                "ls",
                "└─> Lists all saved commute timings.",
                "",
                "rm <timing-number>",
                "└─> Removes a saved commute timing.",
                "",
                "----------------",
                "Session",
                "----------------",
                "help",
                "└─> Shows this list of available commands.",
                "",
                "thx",
                "└─> Ends the current Timey session."));
    }
}
