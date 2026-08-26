package Timey.parser;

import Timey.command.AddCommand;
import Timey.command.PlanCommand;

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
    public ParsedCommand parse(String input) {
        String command = input.trim();
        if (command.equalsIgnoreCase("thx")) {
            return ParsedCommand.thanks();
        }
        if (command.startsWith("plan")) {
            return ParsedCommand.plan(planCommandParser.parse(command));
        }
        if (command.startsWith("add")) {
            return ParsedCommand.add(addCommandParser.parse(command));
        }
        if (command.startsWith("choose")) {
            return ParsedCommand.choose(parseNumberArgument(command));
        }
        if (command.equalsIgnoreCase("reminders")) {
            return ParsedCommand.reminders();
        }
        if (command.startsWith("cancel")) {
            return ParsedCommand.cancel(parseNumberArgument(command));
        }
        return ParsedCommand.unknown();
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

    /** A parsed command and any arguments required to execute it. */
    public record ParsedCommand(Type type, PlanCommand plan, AddCommand addCommand, Integer number) {
        public static ParsedCommand thanks() {
            return new ParsedCommand(Type.THANKS, null, null, null);
        }

        public static ParsedCommand plan(PlanCommand plan) {
            return new ParsedCommand(Type.PLAN, plan, null, null);
        }

        public static ParsedCommand add(AddCommand addCommand) {
            return new ParsedCommand(Type.ADD, null, addCommand, null);
        }

        public static ParsedCommand choose(Integer number) {
            return new ParsedCommand(Type.CHOOSE, null, null, number);
        }

        public static ParsedCommand reminders() {
            return new ParsedCommand(Type.REMINDERS, null, null, null);
        }

        public static ParsedCommand cancel(Integer number) {
            return new ParsedCommand(Type.CANCEL, null, null, number);
        }

        public static ParsedCommand unknown() {
            return new ParsedCommand(Type.UNKNOWN, null, null, null);
        }
    }

    /** The actions accepted by the command-line interface. */
    public enum Type {
        THANKS,
        PLAN,
        ADD,
        CHOOSE,
        REMINDERS,
        CANCEL,
        UNKNOWN
    }
}
