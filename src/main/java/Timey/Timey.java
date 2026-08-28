package Timey;

import Timey.ui.ConsoleUi;

/** Entry point for the Timey application. */
public final class Timey {
    private Timey() {
    }

    public static void main(String[] args) {
        ApplicationFactory.createCommandLineApp(new ConsoleUi()).run();
    }
}
