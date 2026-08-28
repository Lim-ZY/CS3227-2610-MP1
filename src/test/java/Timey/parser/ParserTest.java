package Timey.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ParserTest {
    private final Parser parser = new Parser(new PlanCommandParser());

    @Test
    void parse_planCommand_returnsValidatedPlanAction() {
        var command = assertInstanceOf(Timey.command.PlanCommand.class,
                parser.parse("plan /from \"COM3\" /to \"VivoCity\" /by 1830"));

        assertEquals("COM3", command.getOrigin());
    }

    @Test
    void parse_addCommand_returnsValidatedFixedTimingAction() {
        var command = assertInstanceOf(Timey.command.AddCommand.class,
                parser.parse("add /from COM3 /to VivoCity /dur 1h30m"));

        assertEquals(java.time.Duration.ofMinutes(90), command.getDuration());
    }

    @Test
    void parse_numberedCommands_returnsNumberWhenPresent() {
        assertEquals(2, assertInstanceOf(Timey.command.ChooseCommand.class,
                parser.parse("choose 2")).getRouteNumber());
        assertEquals(1, assertInstanceOf(Timey.command.CancelCommand.class,
                parser.parse("cancel 1")).getReminderNumber());
        assertEquals(1, assertInstanceOf(Timey.command.RemoveFixedTimingCommand.class,
                parser.parse("rm 1")).getTimingNumber());
    }

    @Test
    void parse_malformedNumberedCommand_returnsActionWithoutNumber() {
        assertNull(assertInstanceOf(Timey.command.ChooseCommand.class,
                parser.parse("choose one")).getRouteNumber());
        assertNull(assertInstanceOf(Timey.command.CancelCommand.class,
                parser.parse("cancel 1 2")).getReminderNumber());
        assertNull(assertInstanceOf(Timey.command.RemoveFixedTimingCommand.class,
                parser.parse("rm one")).getTimingNumber());
    }

    @Test
    void parse_nonArgumentCommands_classifiesActions() {
        assertInstanceOf(Timey.command.ThanksCommand.class, parser.parse("thx"));
        assertInstanceOf(Timey.command.HelpCommand.class, parser.parse("help"));
        assertInstanceOf(Timey.command.RemindersCommand.class, parser.parse("reminders"));
        assertInstanceOf(Timey.command.ListCommand.class, parser.parse("ls"));
    }
}
