package de.hhu.stups.plues.ui.components;

import static javafx.concurrent.Worker.State.RUNNING;
import static javafx.concurrent.Worker.State.SUCCEEDED;

import com.google.inject.Inject;

import de.hhu.stups.plues.ui.TaskBindings;
import de.hhu.stups.plues.ui.TaskStateColor;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Show the progress of the corresponding {@link #taskProperty task} using a progress indicator and
 * visualize the task's {@link javafx.concurrent.Worker.State} on succeeded by default. The
 * component's size can be set by {@link #sizeProperty()}. The property {@link
 * #showIconOnSucceededProperty} can be set to false to hide the state icon when the task succeeded.
 * When adjusting the {@link #sizeProperty} the {@link #taskProperty} should be set prior to that.
 */
public class TaskProgressIndicator extends StackPane implements Initializable {

  private final ObjectProperty<Task> taskProperty;
  private final BooleanProperty showIconOnSucceededProperty;
  private final DoubleProperty sizeProperty;

  private ResourceBundle resources;

  @FXML
  @SuppressWarnings("unused")
  private ProgressIndicator progressIndicator;
  @FXML
  @SuppressWarnings("unused")
  private VBox boxProgressIndicator;
  @FXML
  @SuppressWarnings("unused")
  private Tooltip taskRunningTooltip;
  @FXML
  @SuppressWarnings("unused")
  private Label taskStateIcon;
  @FXML
  @SuppressWarnings("unused")
  private Tooltip taskStateIconTooltip;

  /**
   * Initialize properties and component.
   */
  @Inject
  public TaskProgressIndicator(final Inflater inflater) {
    taskProperty = new SimpleObjectProperty<>();
    showIconOnSucceededProperty = new SimpleBooleanProperty(true);
    sizeProperty = new SimpleDoubleProperty(25.0);
    inflater.inflate("components/TaskProgressIndicator", this, this, "tasks");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    this.resources = resources;

    showIconOnSucceededProperty().addListener((observable, oldValue, newValue) ->
        bindTaskStateVisibility(taskProperty.get()));

    prefWidthProperty().bind(sizeProperty);
    prefHeightProperty().bind(sizeProperty);
    taskStateIcon.prefWidthProperty().bind(sizeProperty);
    taskStateIcon.prefHeightProperty().bind(sizeProperty);
    boxProgressIndicator.prefWidthProperty().bind(sizeProperty);
    boxProgressIndicator.prefHeightProperty().bind(sizeProperty);

    taskStateIcon.visibleProperty().bind(visibleProperty());
    boxProgressIndicator.visibleProperty().bind(visibleProperty());

    taskStateIcon.setOnMouseEntered(event -> {
      if (taskStateIconTooltip.getText().isEmpty()) {
        taskStateIconTooltip.hide();
        return;
      }
      final Point2D pos = taskStateIcon.localToScreen(
          taskStateIcon.getLayoutBounds().getMaxX(), taskStateIcon.getLayoutBounds().getMaxY());
      taskStateIconTooltip.show(taskStateIcon, pos.getX(), pos.getY());
    });
    taskStateIcon.setOnMouseExited(event -> taskStateIconTooltip.hide());

    progressIndicator.setOnMouseEntered(event -> {
      final Point2D pos = progressIndicator.localToScreen(
          progressIndicator.getLayoutBounds().getMaxX(),
          progressIndicator.getLayoutBounds().getMaxY());
      taskRunningTooltip.show(progressIndicator, pos.getX(), pos.getY());
    });
    progressIndicator.setOnMouseExited(event -> taskRunningTooltip.hide());

    taskProperty.addListener((observable, oldValue, newValue) -> {
      if (newValue == null) {
        resetTaskState();
        return;
      }
      showTaskState(newValue);
    });

    sizeProperty().addListener((observable, oldValue, newValue) -> {
      if (taskProperty().get() == null) {
        return;
      }
      taskStateIcon.graphicProperty().unbind();
      taskStateIcon.graphicProperty().bind(TaskBindings.getIconBinding(
          Double.toString(newValue.doubleValue()), taskProperty.get()));
    });
  }

  public ObjectProperty<Task> taskProperty() {
    return taskProperty;
  }

  public DoubleProperty sizeProperty() {
    return sizeProperty;
  }

  public BooleanProperty showIconOnSucceededProperty() {
    return showIconOnSucceededProperty;
  }

  private void showTaskState(final Task task) {
    if (task == null) {
      return;
    }
    taskStateIcon.graphicProperty().unbind();
    taskStateIcon.styleProperty().unbind();
    taskStateIconTooltip.textProperty().unbind();

    bindTaskStateVisibility(task);
    taskStateIcon.graphicProperty().bind(
        TaskBindings.getIconBinding(Double.toString(sizeProperty.get()), task));
    taskStateIcon.styleProperty().bind(TaskBindings.getStyleBinding(task));
    taskStateIconTooltip.textProperty().bind(Bindings.createStringBinding(
        () -> getMessageForTask(task), task.stateProperty()));

    progressIndicator.setStyle("-fx-progress-color: " + TaskStateColor.WORKING.getColor());
    progressIndicator.visibleProperty().bind(task.runningProperty());
  }

  private void bindTaskStateVisibility(final Task task) {
    if (task == null) {
      return;
    }
    final BooleanBinding binding = task.stateProperty().isEqualTo(RUNNING).not();
    taskStateIcon.visibleProperty().bind(showIconOnSucceededProperty.get() ? binding
        : binding.and(task.stateProperty().isEqualTo(SUCCEEDED).not()));
  }

  private void resetTaskState() {
    taskStateIcon.styleProperty().unbind();
    taskStateIcon.setStyle("");

    taskStateIcon.graphicProperty().unbind();
    taskStateIcon.setGraphic(null);

    taskStateIconTooltip.textProperty().unbind();
    taskStateIconTooltip.setText("");
  }

  private String getMessageForTask(final Task task) {
    final String msg;
    switch (task.getState()) {
      case RUNNING:
        msg = resources.getString("computation.Running");
        break;
      case CANCELLED:
        msg = resources.getString("computation.Cancelled");
        break;
      case FAILED:
        msg = resources.getString("computation.Failed");
        break;
      case READY:
      case SCHEDULED:
        msg = resources.getString("computation.Waiting");
        break;
      case SUCCEEDED:
        msg = resources.getString("computation.Succeeded");
        break;
      default:
        msg = "";
        break;
    }
    return msg;
  }

  public Label getTaskStateIcon() {
    return taskStateIcon;
  }
}
