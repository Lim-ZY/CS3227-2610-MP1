package Timey.ui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import Timey.command.CommandResult;

/** Handles all console input and output for the Timey command-line interface. */
public final class Ui {
    private static final String DIVIDER = "_______________________________________________________";

    private final BufferedReader input;
    private final PrintWriter output;

    /** Creates a UI connected to the standard console streams. */
    public Ui() {
        this(new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)), new PrintWriter(System.out, true));
    }

    public Ui(BufferedReader input, PrintWriter output) {
        this.input = input;
        this.output = output;
    }

    public String readCommand() throws IOException {
        return input.readLine();
    }

    public void printWelcome() {
        println(DIVIDER);
        println("  _______ _                 ");
        println(" |__   __(_)                ");
        println("    | |   _ _ __ ___   ___  _   _");
        println("    | |  | | '_ ` _ \\ / _ \\| | | |");
        println("    | |  | | | | | | |  __/| |_| |");
        println("    |_|  |_|_| |_| |_|\\___|\\__, |");
        println("                              __/ |");
        println("                             |___/ ");
        println("Hey! I'll help you to be on track today as always!");
        println(DIVIDER);
        println("Try: plan /from \"COM3\" /to \"VivoCity\" /by 1830 /buf 10m");
        printPrompt();
    }

    public void printDivider() {
        println(DIVIDER);
        flush();
    }

    public void printPrompt() {
        print("> ");
        flush();
    }

    public void showReadingError() {
        println("I could not read your command. Please restart Timey and try again.");
    }

    /** Displays feedback produced by a command. */
    public void show(CommandResult result) {
        result.messages().forEach(this::println);
    }

    public void println(String message) {
        output.println(message);
    }

    public void println() {
        output.println();
    }

    public void print(String message) {
        output.print(message);
    }

    public void flush() {
        output.flush();
    }
}
