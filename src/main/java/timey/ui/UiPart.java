package timey.ui;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.URL;

import javafx.fxml.FXMLLoader;

/**
 * A distinct FXML-backed JavaFX UI component with a root object of type {@code T}.
 *
 * <p>The concrete subclass is used as the FXML controller, so its FXML file must not declare an
 * {@code fx:controller} attribute.</p>
 */
public abstract class UiPart<T> {
    /** Resource folder containing FXML views for the dashboard. */
    public static final String FXML_FILE_FOLDER = "/timey/ui/dashboard/view/";

    private final FXMLLoader fxmlLoader = new FXMLLoader();

    /** Loads an FXML view by its file name from {@link #FXML_FILE_FOLDER}. */
    protected UiPart(String fxmlFileName) {
        this(getFxmlFileUrl(fxmlFileName));
    }

    /** Loads an FXML view from {@code fxmlFileUrl}. */
    protected UiPart(URL fxmlFileUrl) {
        loadFxmlFile(fxmlFileUrl, null);
    }

    /** Loads an {@code fx:root} FXML view by file name with the supplied root object. */
    protected UiPart(String fxmlFileName, T root) {
        this(getFxmlFileUrl(fxmlFileName), root);
    }

    /** Loads an {@code fx:root} FXML view from {@code fxmlFileUrl} with the supplied root object. */
    protected UiPart(URL fxmlFileUrl, T root) {
        loadFxmlFile(fxmlFileUrl, root);
    }

    /** Returns the root object loaded from the FXML view. */
    @SuppressWarnings("unchecked")
    public T getRoot() {
        return (T) fxmlLoader.getRoot();
    }

    private void loadFxmlFile(URL fxmlFileUrl, T root) {
        fxmlLoader.setLocation(requireNonNull(fxmlFileUrl));
        fxmlLoader.setController(this);
        fxmlLoader.setRoot(root);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private static URL getFxmlFileUrl(String fxmlFileName) {
        requireNonNull(fxmlFileName);
        return requireNonNull(UiPart.class.getResource(FXML_FILE_FOLDER + fxmlFileName));
    }
}
