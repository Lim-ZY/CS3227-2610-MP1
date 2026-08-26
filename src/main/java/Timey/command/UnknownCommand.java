package Timey.command;

import Timey.model.TimeyModel;

/** Provides usage guidance for unrecognised input. */
public final class UnknownCommand extends Command {
    @Override
    public CommandResult execute(TimeyModel model) {
        return CommandResult.message(
                "I did not understand that. Try: plan /from \"COM3\" /to \"VivoCity\" /by 1830 /buf 10m");
    }
}
