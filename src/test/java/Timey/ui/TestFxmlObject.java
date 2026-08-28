package Timey.ui;

import java.util.Objects;

import javafx.beans.DefaultProperty;

/** Test-only FXML object that does not require the JavaFX toolkit. */
@DefaultProperty("text")
public class TestFxmlObject {
    private String text;

    public TestFxmlObject() {
    }

    public TestFxmlObject(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TestFxmlObject)) {
            return false;
        }
        return Objects.equals(text, ((TestFxmlObject) other).text);
    }
}
