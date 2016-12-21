package de.hhu.stups.plues.ui.controller.unsatcore;

import com.google.inject.Inject;

import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.TaskBindings;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
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

  /**
   * Default constructor.
   */
  @Inject
  public UnsatCoreButtonBar(final Inflater inflater) {
    inflater.inflate("components/unsatcore/UnsatCoreButtonBar", this, this, "unsatCore");
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {

  }

  /**
   * Configure button for one unsat core.
   */
  void configureButton(final String text,
                       final BooleanBinding binding,
                       final EventHandler<MouseEvent> eventHandler) {
    button.setText(text);
    button.disableProperty().bind(binding);
    button.setOnMouseClicked(eventHandler);
  }

  /**
   * Show and set current task state.
   */
  void showTaskState(final Task<?> task, final ResourceBundle resources) {
    taskStateIcon.graphicProperty().unbind();
    taskStateIcon.styleProperty().unbind();
    taskStateLabel.textProperty().unbind();

    taskStateIcon.graphicProperty().bind(TaskBindings.getIconBinding("25", task));
    taskStateIcon.styleProperty().bind(TaskBindings.getStyleBinding(task));
    taskStateLabel.textProperty().bind(Bindings.createStringBinding(() -> {
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
    }, task.stateProperty()));
  }


  void resetTaskState() {
    taskStateIcon.styleProperty().unbind();
    taskStateIcon.setStyle("");
    //
    taskStateIcon.graphicProperty().unbind();
    taskStateIcon.setGraphic(null);
    //
    taskStateLabel.textProperty().unbind();
    taskStateLabel.setText("");
  }

}
