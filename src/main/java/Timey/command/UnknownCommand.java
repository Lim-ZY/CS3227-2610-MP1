package Timey.command;

import Timey.model.TimeyModel;

/** Provides usage guidance for unrecognised input. */
public final class UnknownCommand extends Command {
    @Override
    public CommandResult execute(TimeyModel model) {
        return CommandResult.message(
                "Sorry I did not understand that... Use `help` for the list of commands I understand.");
    }
}
