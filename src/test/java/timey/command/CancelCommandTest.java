package timey.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import timey.TestTimeyModelFactory;
import timey.infrastructure.transit.InMemoryFixedCommuteStore;

class CancelCommandTest {
    @Test
    void execute_withoutReminderNumber_explainsRequiredArgument() {
        var result = new CancelCommand(null).execute(TestTimeyModelFactory.create(new InMemoryFixedCommuteStore()));

        assertEquals(java.util.List.of("Cancel a reminder by number, for example: cancel 1"), result.messages());
    }

    @Test
    void execute_withInactiveReminderNumber_reportsNoMatchingReminder() {
        var result = new CancelCommand(1).execute(TestTimeyModelFactory.create(new InMemoryFixedCommuteStore()));

        assertEquals(java.util.List.of("No active departure reminder numbered 1."), result.messages());
    }
}
