package Timey.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import Timey.TestTimeyModelFactory;
import Timey.infrastructure.transit.InMemoryFixedCommuteStore;

class RemindersCommandTest {
    @Test
    void execute_withoutActiveReminders_reportsEmptyReminderList() {
        var result = new RemindersCommand().execute(TestTimeyModelFactory.create(new InMemoryFixedCommuteStore()));

        assertEquals(java.util.List.of("You have no active departure reminders."), result.messages());
    }
}
