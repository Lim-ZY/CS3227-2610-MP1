package Timey.parser;

import Timey.command.CancelCommand;
import Timey.command.ChooseCommand;
import Timey.command.Command;
import Timey.command.HelpCommand;
import Timey.command.RemindersCommand;
import Timey.command.ListCommand;
import Timey.command.RemoveCommand;
import Timey.command.ThanksCommand;
import Timey.command.UnknownCommand;

/** Parses a complete user command into the action requested by the user. */
public final class Parser {
    private final PlanCommandParser planCommandParser;
    private final AddCommandParser addCommandParser;

    public Parser(PlanCommandParser planCommandParser) {
        this(planCommandParser, new AddCommandParser());
    }

    public Parser(PlanCommandParser planCommandParser, AddCommandParser addCommandParser) {
        this.planCommandParser = planCommandParser;
        this.addCommandParser = addCommandParser;
    }

    /**
     * Parses a user command. Plan commands are fully validated by {@link PlanCommandParser}.
     *
     * @param input command text entered by the user
     * @return the requested command action
     * @throws IllegalArgumentException when a plan command is invalid
     */
    public Command parse(String input) {
        String command = input.trim();
        if (command.equalsIgnoreCase("thx")) {
            return new ThanksCommand();
        }
        if (command.equalsIgnoreCase("help")) {
            return new HelpCommand();
        }
        if (command.startsWith("plan")) {
            return planCommandParser.parse(command);
        }
        if (command.startsWith("add")) {
            return addCommandParser.parse(command);
        }
        if (command.equalsIgnoreCase("ls")) {
            return new ListCommand();
        }
        if (command.equalsIgnoreCase("rm") || command.startsWith("rm ")) {
            return new RemoveCommand(parseNumberArgument(command));
        }
        if (command.startsWith("choose")) {
            return new ChooseCommand(parseNumberArgument(command));
        }
        if (command.equalsIgnoreCase("reminders")) {
            return new RemindersCommand();
        }
        if (command.startsWith("cancel")) {
            return new CancelCommand(parseNumberArgument(command));
        }
        return new UnknownCommand();
    }

    private Integer parseNumberArgument(String command) {
        String[] parts = command.split("\\s+");
        if (parts.length != 2) {
            return null;
        }
        try {
            return Integer.valueOf(parts[1]);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
