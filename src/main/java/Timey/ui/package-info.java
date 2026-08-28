/**
 * Presentation-layer types shared by Timey's console and JavaFX entry points.
 *
 * <p>{@link Timey.ui.ConsoleUi} owns terminal input and output, while
 * {@link Timey.ui.CommandLineApp} coordinates a command-session workflow without knowing which
 * presentation renders its state. {@link Timey.ui.DashboardState} is the immutable rendering
 * boundary returned by that workflow.</p>
 *
 * <p>JavaFX dashboard components reside in {@code Timey.ui.dashboard}. Each component extends
 * {@link Timey.ui.UiPart}, owns one FXML view under
 * {@code src/main/resources/Timey/ui/dashboard/view}, and exposes a small rendering API instead
 * of its controls. FXML files must not declare {@code fx:controller}: {@code UiPart} supplies the
 * concrete component as controller. {@code MainWindow} composes those parts and coordinates
 * asynchronous command execution; {@code TimeyDashboardApp} only launches it with dependencies.</p>
 */
package Timey.ui;
