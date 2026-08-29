package timey.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URL;

import org.junit.jupiter.api.Test;

import javafx.fxml.FXML;

class UiPartTest {
    private static final String INVALID_FILE = "UiPartTest/invalidFile.fxml";
    private static final String MISSING_FILE = "UiPartTest/missingFile.fxml";
    private static final String VALID_FILE = "UiPartTest/validFile.fxml";
    private static final String VALID_FX_ROOT_FILE = "UiPartTest/validFileWithFxRoot.fxml";
    private static final TestFxmlObject EXPECTED_ROOT = new TestFxmlObject("Hello World!");

    @Test
    void constructor_validFile_loadsRootAndInjectsFields() {
        assertEquals(EXPECTED_ROOT, new TestUiPart<TestFxmlObject>(VALID_FILE).getRoot());
    }

    @Test
    void constructor_validFxRootFile_loadsSuppliedRoot() {
        TestFxmlObject root = new TestFxmlObject();

        assertEquals(EXPECTED_ROOT, new TestUiPart<TestFxmlObject>(VALID_FX_ROOT_FILE, root).getRoot());
    }

    @Test
    void constructor_missingFile_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new TestUiPart<Object>(MISSING_FILE));
    }

    @Test
    void constructor_invalidFile_throwsAssertionError() {
        assertThrows(AssertionError.class, () -> new TestUiPart<Object>(INVALID_FILE));
    }

    private static class TestUiPart<T> extends UiPart<T> {
        @FXML
        private TestFxmlObject validFileRoot;

        TestUiPart(String fxmlFileName) {
            super(fxmlFileName);
            assertEquals(EXPECTED_ROOT, validFileRoot);
        }

        TestUiPart(String fxmlFileName, T root) {
            super(fxmlFileName, root);
            assertEquals(EXPECTED_ROOT, validFileRoot);
        }

        TestUiPart(URL fxmlFileUrl) {
            super(fxmlFileUrl);
        }
    }
}
