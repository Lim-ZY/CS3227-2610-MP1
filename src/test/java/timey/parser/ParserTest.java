package timey.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ParserTest {
    private final Parser parser = new Parser(new PlanCommandParser());

    @Test
    void parse_planCommand_returnsValidatedPlanAction() {
        var command = assertInstanceOf(timey.command.PlanCommand.class,
                parser.parse("plan /from \"COM3\" /to \"VivoCity\" /by 1830"));

        assertEquals("COM3", command.getOrigin());
    }

    @Test
    void parse_addCommand_returnsValidatedFixedTimingAction() {
        var command = assertInstanceOf(timey.command.AddCommand.class,
                parser.parse("add /from COM3 /to VivoCity /dur 1h30m"));

        assertEquals(java.time.Duration.ofMinutes(90), command.getDuration());
    }

    @Test
    void parse_numberedCommands_returnsNumberWhenPresent() {
        assertEquals(2, assertInstanceOf(timey.command.ChooseCommand.class,
                parser.parse("choose 2")).getRouteNumber());
        assertEquals(1, assertInstanceOf(timey.command.CancelCommand.class,
                parser.parse("cancel 1")).getReminderNumber());
        assertEquals(1, assertInstanceOf(timey.command.RemoveCommand.class,
                parser.parse("rm 1")).getTimingNumber());
    }

    @Test
    void parse_malformedNumberedCommand_returnsActionWithoutNumber() {
        assertNull(assertInstanceOf(timey.command.ChooseCommand.class,
                parser.parse("choose one")).getRouteNumber());
        assertNull(assertInstanceOf(timey.command.CancelCommand.class,
                parser.parse("cancel 1 2")).getReminderNumber());
        assertNull(assertInstanceOf(timey.command.RemoveCommand.class,
                parser.parse("rm one")).getTimingNumber());
    }

    @Test
    void parse_nonArgumentCommands_classifiesActions() {
        assertInstanceOf(timey.command.ThanksCommand.class, parser.parse("thx"));
        assertInstanceOf(timey.command.HelpCommand.class, parser.parse("help"));
        assertInstanceOf(timey.command.RemindersCommand.class, parser.parse("reminders"));
        assertInstanceOf(timey.command.ListCommand.class, parser.parse("ls saved"));
        assertInstanceOf(timey.command.ListCommand.class, parser.parse("ls plans"));
    }
}
