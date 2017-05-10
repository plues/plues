package de.hhu.stups.plues.ui.components.timetable;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;

import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;

/**
 * Moving a session affects the database state, thus, waiting tasks. When the user moves a session
 * while there are running tasks we display a warning with several options to choose from.
 */
public class MoveSessionDialog extends GridPane {

  private final ListeningExecutorService executorService;
  private final UiDataService uiDataService;

  /**
   * Instantiate the executor service and the ui data service.
   */
  @Inject
  public MoveSessionDialog(final Inflater inflater,
                           final UiDataService uiDataService,
                           final ListeningExecutorService executorService) {
    this.executorService = executorService;
    this.uiDataService = uiDataService;

    inflater.inflate("components/timetable/MoveSessionDialog", this, this, "moveSessionDialog");
  }

  /**
   * Move the session although there are running tasks.
   */
  @FXML
  @SuppressWarnings({"unused", "WeakerAccess"})
  public void moveSession() {
    final SolverTask<Void> moveSessionTask = uiDataService.moveSessionTaskProperty().get();
    if (moveSessionTask != null) {
      //noinspection ResultOfMethodCallIgnored
      executorService.submit(moveSessionTask);
      uiDataService.moveSessionTaskProperty().set(null);
    }
  }

  /**
   * Cancel all running tasks and move the session.
   */
  @FXML
  @SuppressWarnings("unused")
  public void cancelAndMoveSession() {
    uiDataService.cancelAllTasksProperty().set(true);
    moveSession();
  }

  /**
   * Cancel moving the session.
   */
  @FXML
  @SuppressWarnings("unused")
  public void cancelMoveSession() {
    uiDataService.moveSessionTaskProperty().set(null);
  }
}
