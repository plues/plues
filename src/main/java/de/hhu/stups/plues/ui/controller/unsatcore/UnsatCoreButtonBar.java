package de.hhu.stups.plues.ui.controller.unsatcore;

import com.google.inject.Inject;

import de.hhu.stups.plues.ui.TaskBindings;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.ResourceBundle;

public class UnsatCoreButtonBar extends HBox implements Initializable {

  @FXML
  @SuppressWarnings("unused")
  private Button button;
  @FXML
  @SuppressWarnings("unused")
  private Label taskStateLabel;
  @FXML
  @SuppressWarnings("unused")
  private Label taskStateIcon;

  private StringProperty text = new SimpleStringProperty();
  private ResourceBundle resources;

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
    this.button.textProperty().bind(this.textProperty());
    this.button.disableProperty().bind(this.disabledProperty());
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
    this.button.setOnAction(eventHandler);
  }

  /**
   * Show and set current task state.
   */
  void showTaskState(final Task<?> task) {
    taskStateIcon.graphicProperty().unbind();
    taskStateIcon.styleProperty().unbind();
    taskStateLabel.textProperty().unbind();

    taskStateIcon.graphicProperty().bind(TaskBindings.getIconBinding("25", task));
    taskStateIcon.styleProperty().bind(TaskBindings.getStyleBinding(task));
    taskStateLabel.textProperty().bind(Bindings.createStringBinding(
        () -> getMessageForTask(task), task.stateProperty()));
  }

  void resetTaskState() {
    taskStateIcon.styleProperty().unbind();
    taskStateIcon.setStyle("");

    taskStateIcon.graphicProperty().unbind();
    taskStateIcon.setGraphic(null);

    taskStateLabel.textProperty().unbind();
    taskStateLabel.setText("");
  }

  private String getMessageForTask(final Task<?> task) {
    final String msg;
    switch (task.getState()) {
      case SUCCEEDED:
        msg = "";
        break;
      case CANCELLED:
        msg = resources.getString("task.Cancelled");
        break;
      case FAILED:
        msg = resources.getString("task.Failed");
        break;
      case READY:
      case SCHEDULED:
      case RUNNING:
      default:
        msg = resources.getString("task.Running");
        break;
    }
    return msg;
  }
}
