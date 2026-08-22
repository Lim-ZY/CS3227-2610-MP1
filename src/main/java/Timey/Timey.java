package Timey;

import Timey.ui.Ui;

/** Entry point for the Timey application. */
public final class Timey {
    private Timey() {
    }

    public static void main(String[] args) {
        ApplicationFactory.createCommandLineApp(new Ui()).run();
    }
}
