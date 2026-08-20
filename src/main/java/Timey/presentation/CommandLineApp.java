package Timey.presentation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;

import Timey.command.PlanCommand;
import Timey.command.PlanCommandParser;

/** Interactive command-line presentation for Timey. */
public final class CommandLineApp {
    private static final String DIVIDER = "_______________________________________________________";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final BufferedReader input;
    private final PrintWriter output;
    private final PlanCommandParser planCommandParser;

    public CommandLineApp(BufferedReader input, PrintWriter output) {
        this(input, output, new PlanCommandParser());
    }

    CommandLineApp(BufferedReader input, PrintWriter output, PlanCommandParser planCommandParser) {
        this.input = input;
        this.output = output;
        this.planCommandParser = planCommandParser;
    }

    /** Runs until the user says thanks or standard input closes. */
    public void run() {
        printWelcome();
        try {
            String command;
            while ((command = input.readLine()) != null) {
                handle(command.trim());
                if (command.trim().equalsIgnoreCase("thx")) {
                    return;
                }
            }
        } catch (IOException exception) {
            output.println("I could not read your command. Please restart Timey and try again.");
        }
    }

    private void printWelcome() {
        output.println(DIVIDER);
        output.println("  _______ _                 ");
        output.println(" |__   __(_)                ");
        output.println("    | |   _ _ __ ___   ___  _   _");
        output.println("    | |  | | '_ ` _ \\ / _ \\| | | |");
        output.println("    | |  | | | | | | |  __/| |_| |");
        output.println("    |_|  |_|_| |_| |_|\\___|\\__, |");
        output.println("                              __/ |");
        output.println("                             |___/ ");
        output.println("Hey! I'll help you to be on track today as always!");
        output.println(DIVIDER);
        output.println("Try: plan /from \"COM3\" /to \"VivoCity\" /by 1830 /buf 10m");
    }

    private void handle(String command) {
        if (command.equalsIgnoreCase("thx")) {
            output.println(DIVIDER);
            output.println("Alrighty, hope you'll have a nice day ahead!");
            return;
        }
        if (command.startsWith("plan")) {
            handlePlan(command);
            return;
        }
        output.println("I did not understand that. Try: plan /from \"COM3\" /to \"VivoCity\" /by 1830 /buf 10m");
    }

    private void handlePlan(String command) {
        try {
            PlanCommand plan = planCommandParser.parse(command);
            output.println(DIVIDER);
            output.println("Got it! I have noted down your plan as follows:");
            output.println();
            output.println("From: " + plan.origin());
            output.println("To: " + plan.destination());
            output.println("Target arrival: " + plan.arrivalTime().format(TIME_FORMAT));
            output.println("Personal buffer: " + plan.buffer().toMinutes() + " minutes");
            output.println();
            output.println("Leave it to me!");
        } catch (IllegalArgumentException exception) {
            output.println("I could not create that plan: " + exception.getMessage());
        }
    }
}
