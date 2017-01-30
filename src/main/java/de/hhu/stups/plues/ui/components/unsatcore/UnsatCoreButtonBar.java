package de.hhu.stups.plues.ui.components.unsatcore;

import static javafx.concurrent.Worker.State.RUNNING;
import static javafx.concurrent.Worker.State.SUCCEEDED;

import com.google.inject.Inject;

import de.hhu.stups.plues.ui.TaskBindings;
import de.hhu.stups.plues.ui.TaskStateColor;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.ResourceBundle;


public class UnsatCoreButtonBar extends HBox implements Initializable {

  @FXML
  @SuppressWarnings("unused")
  private Button btSubmitTask;
  @FXML
  @SuppressWarnings("unused")
  private Button btCancelTask;
  @FXML
  @SuppressWarnings("unused")
  private Label taskStateIcon;
  @FXML
  @SuppressWarnings("unused")
  private Tooltip taskStateIconTooltip;
  @FXML
  @SuppressWarnings("unused")
  private Tooltip taskRunningTooltip;
  @FXML
  @SuppressWarnings("unused")
  private ProgressIndicator progressIndicator;

  private final StringProperty text = new SimpleStringProperty();
  private ResourceBundle resources;
  private Task<?> task;

  /**
   * Default constructor.
   */
  @Inject
  public UnsatCoreButtonBar(final Inflater inflater) {
    inflater.inflate("components/unsatcore/UnsatCoreButtonBar", this, this, "unsatCore");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    this.resources = resources;
    btSubmitTask.textProperty().bind(textProperty());
    btSubmitTask.disableProperty().bind(disabledProperty());

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
  }

  public String getText() {
    return this.text.get();
  }

  public void setText(final String text) {
    this.text.set(text);
  }

  public StringProperty textProperty() {
    return this.text;
  }

  public void setOnAction(final EventHandler<ActionEvent> eventHandler) {
    btSubmitTask.setOnAction(eventHandler);
  }

  @FXML
  @SuppressWarnings("unused")
  public void cancelTask() {
    task.cancel(true);
  }

  /**
   * Show and set current task state.
   */
  public void showTaskState(final Task<?> task) {
    this.task = task;
    taskStateIcon.graphicProperty().unbind();
    taskStateIcon.styleProperty().unbind();
    taskStateIconTooltip.textProperty().unbind();

    taskStateIcon.visibleProperty().bind(task.stateProperty().isEqualTo(SUCCEEDED).not()
        .and(task.stateProperty().isEqualTo(RUNNING).not()));
    taskStateIcon.graphicProperty().bind(TaskBindings.getIconBinding("25", task));
    taskStateIcon.styleProperty().bind(TaskBindings.getStyleBinding(task));
    taskStateIconTooltip.textProperty().bind(Bindings.createStringBinding(
        () -> getMessageForTask(task), task.stateProperty()));
    btCancelTask.disableProperty().bind(task.runningProperty().not());
    btSubmitTask.disableProperty().bind(task.runningProperty());

    progressIndicator.setStyle("-fx-progress-color: " + TaskStateColor.WORKING.getColor());
    progressIndicator.visibleProperty().bind(task.runningProperty());
  }

  void resetTaskState() {
    taskStateIcon.styleProperty().unbind();
    taskStateIcon.setStyle("");

    taskStateIcon.graphicProperty().unbind();
    taskStateIcon.setGraphic(null);

    taskStateIconTooltip.textProperty().unbind();
    taskStateIconTooltip.setText("");
  }

  public Task getTask() {
    return task;
  }

  @SuppressWarnings("WeakerAccess")
  public Button getSubmitTask() {
    return btSubmitTask;
  }

  @SuppressWarnings("WeakerAccess")
  public Button getCancelTask() {
    return btCancelTask;
  }

  private String getMessageForTask(final Task<?> task) {
    final String msg;
    switch (task.getState()) {
      case RUNNING:
        msg = resources.getString("task.Running");
        break;
      case CANCELLED:
        msg = resources.getString("task.Cancelled");
        break;
      case FAILED:
        msg = resources.getString("task.Failed");
        break;
      case READY:
      case SCHEDULED:
        msg = resources.getString("task.Waiting");
        break;
      case SUCCEEDED:
      default:
        msg = "";
        break;
    }
    return msg;
  }
}
