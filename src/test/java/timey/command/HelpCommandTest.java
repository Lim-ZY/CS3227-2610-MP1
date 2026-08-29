package timey.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import timey.TestTimeyModelFactory;
import timey.infrastructure.transit.InMemoryFixedCommuteStore;

class HelpCommandTest {
    @Test
    void execute_returnsExactHelpOutput() {
        var result = new HelpCommand().execute(TestTimeyModelFactory.create(new InMemoryFixedCommuteStore()));

        assertEquals(List.of(
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
                "ls saved",
                "└─> Lists all saved commute timings.",
                "",
                "ls plans",
                "└─> Lists saved plans whose departure time is still in the future.",
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
                "└─> Ends the current Timey session."), result.messages());
    }
}
