package de.hhu.stups.plues.ui.components;

import static javafx.concurrent.Worker.State.RUNNING;

import com.google.inject.Inject;

import de.hhu.stups.plues.ui.TaskBindings;
import de.hhu.stups.plues.ui.TaskStateColor;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Show the progress of the corresponding {@link #taskProperty task} using a progress indicator and
 * visualize the task's {@link javafx.concurrent.Worker.State} on finished by default. When
 * initializing the component the width and height should be set via {@link #prefWidthProperty()}
 * and {@link #prefHeightProperty()}. The property {@link #showIconOnFinished} can be set to false
 * to only show a running task's progress and hide on finished.
 */
public class TaskProgressIndicator extends StackPane implements Initializable {

  private final ObjectProperty<Task> taskProperty = new SimpleObjectProperty<>();
  private final BooleanProperty showIconOnFinished = new SimpleBooleanProperty(true);

  private ResourceBundle resources;

  @FXML
  @SuppressWarnings("unused")
  private ProgressIndicator progressIndicator;
  @FXML
  @SuppressWarnings("unused")
  private Tooltip taskRunningTooltip;
  @FXML
  @SuppressWarnings("unused")
  private Label taskStateIcon;
  @FXML
  @SuppressWarnings("unused")
  private Tooltip taskStateIconTooltip;

  @Inject
  public TaskProgressIndicator(final Inflater inflater) {
    inflater.inflate("components/TaskProgressIndicator", this, this, "tasks");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    this.resources = resources;

    taskStateIcon.setOnMouseEntered(event -> {
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
  }

  public ObjectProperty<Task> taskProperty() {
    return taskProperty;
  }

  public BooleanProperty showIconOnFinished() {
    return showIconOnFinished;
  }

  private void showTaskState(final Task task) {
    taskStateIcon.graphicProperty().unbind();
    taskStateIcon.styleProperty().unbind();
    taskStateIconTooltip.textProperty().unbind();

    taskStateIcon.visibleProperty().bind(showIconOnFinished.get()
        ? task.stateProperty().isEqualTo(RUNNING).not()
        : new SimpleBooleanProperty(false));
    taskStateIcon.graphicProperty().bind(
        TaskBindings.getIconBinding(Double.toString(prefWidthProperty().get()), task));
    taskStateIcon.styleProperty().bind(TaskBindings.getStyleBinding(task));
    taskStateIconTooltip.textProperty().bind(Bindings.createStringBinding(
        () -> getMessageForTask(task), task.stateProperty()));

    progressIndicator.setStyle("-fx-progress-color: " + TaskStateColor.WORKING.getColor());
    progressIndicator.visibleProperty().bind(task.runningProperty());
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
}
