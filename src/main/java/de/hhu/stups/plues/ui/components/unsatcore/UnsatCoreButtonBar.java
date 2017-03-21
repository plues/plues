package de.hhu.stups.plues.ui.components.unsatcore;

import com.google.inject.Inject;

import de.hhu.stups.plues.ui.components.TaskProgressIndicator;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.ResourceBundle;

public class UnsatCoreButtonBar extends HBox implements Initializable {

  private final StringProperty submitTextProperty;
  private final ObjectProperty<Task> taskProperty;
  private final BooleanProperty taskScheduled;

  @FXML
  @SuppressWarnings("unused")
  private Button btSubmitTask;
  @FXML
  @SuppressWarnings("unused")
  private Button btCancelTask;

  @FXML
  @SuppressWarnings("unused")
  private TaskProgressIndicator taskProgressIndicator;

  /**
   * Default constructor.
   */
  @Inject
  public UnsatCoreButtonBar(final Inflater inflater) {
    submitTextProperty = new SimpleStringProperty();
    taskProperty = new SimpleObjectProperty<>();
    taskScheduled = new SimpleBooleanProperty();

    inflater.inflate("components/unsatcore/UnsatCoreButtonBar", this, this, "unsatCore");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    btSubmitTask.textProperty().bind(submitTextProperty);
    btSubmitTask.disableProperty().bind(disabledProperty());

    btSubmitTask.visibleProperty().bind(visibleProperty());
    btCancelTask.visibleProperty().bind(visibleProperty());
    taskProgressIndicator.visibleProperty().bind(visibleProperty());

    taskProgressIndicator.taskProperty().bind(taskProperty);

    taskProperty.addListener((observable, oldValue, newValue) -> {
      if (newValue != null) {
        setTaskScheduled(newValue);
        btCancelTask.disableProperty().bind(taskScheduled.not().or(disableProperty()));
        btSubmitTask.disableProperty().bind(taskScheduled.or(disableProperty()));
      }
    });

    taskProgressIndicator.showIconOnSucceededProperty().set(false);
  }

  public void setSubmitText(final String text) {
    submitTextProperty.set(text);
  }

  public void setOnAction(final EventHandler<ActionEvent> eventHandler) {
    btSubmitTask.setOnAction(eventHandler);
  }

  @FXML
  @SuppressWarnings("unused")
  public void cancelTask() {
    taskProperty.get().cancel(true);
  }

  /**
   * Set the {@link #taskScheduled} true until the corresponding task is done or cancelled.
   */
  private void setTaskScheduled(final Task<?> task) {
    taskScheduled.set(true);
    task.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, event ->
        taskScheduled.set(false));
    task.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, event ->
        taskScheduled.set(false));
    task.addEventHandler(WorkerStateEvent.WORKER_STATE_CANCELLED, event ->
        taskScheduled.set(false));
  }

  public Task getTask() {
    return taskProperty.get();
  }

  @SuppressWarnings("WeakerAccess")
  public Button getSubmitTask() {
    return btSubmitTask;
  }

  @SuppressWarnings("WeakerAccess")
  public Button getCancelTask() {
    return btCancelTask;
  }

  public ObjectProperty<Task> taskProperty() {
    return taskProperty;
  }

  void setShowIconOnSucceeded(final boolean showIconOnSucceeded) {
    taskProgressIndicator.showIconOnSucceededProperty().set(showIconOnSucceeded);
  }

  Button getBtSubmitTask() {
    return btSubmitTask;
  }
}
