package de.hhu.stups.plues.ui.components.timetable;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.ObservableStore;
import de.hhu.stups.plues.services.HistoryManager;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.tasks.SolverTask;

import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import java.util.Optional;
import java.util.ResourceBundle;

public class SessionListView extends ListView<SessionFacade> {
  private final SessionFacade.Slot slot;
  private final Delayed<ObservableStore> delayedStore;
  private final Delayed<SolverService> delayedSolverService;
  private final UiDataService uiDataService;
  private final HistoryManager historyManager;
  private final ListeningExecutorService executorService;
  private ListProperty<SessionFacade> sessions;

  /**
   * Custom implementation of ListView for sessions.
   *
   * @param slot                 the time slot identifying this session list
   * @param delayedStore         Store to save new session info after moving
   * @param delayedSolverService Solver to find out if moving a session is valid
   * @param uiDataService        a stupid data container to dump any kind of data in it
   */
  @Inject
  public SessionListView(@Assisted final SessionFacade.Slot slot,
                         final Delayed<ObservableStore> delayedStore,
                         final Delayed<SolverService> delayedSolverService,
                         final Provider<SessionCell> cellProvider,
                         final UiDataService uiDataService,
                         final ListeningExecutorService executorService,
                         final HistoryManager historyManager) {
    this.slot = slot;
    this.delayedStore = delayedStore;
    this.delayedSolverService = delayedSolverService;
    this.uiDataService = uiDataService;
    this.executorService = executorService;
    this.historyManager = historyManager;

    setCellFactory(param -> cellProvider.get());

    initEvents();
    setupConflictHighlight();
  }

  private void setupConflictHighlight() {
    this.uiDataService.conflictMarkedSessionsProperty()
        .addListener((observable, oldValue, newValue) -> computeStyleClass());
    itemsProperty()
        .addListener(observable -> computeStyleClass());
  }

  private void computeStyleClass() {
    getStyleClass().remove("red-border");

    if (hasSessionIdsIn(this.uiDataService.conflictMarkedSessionsProperty())) {
      getStyleClass().add("red-border");
    }
  }

  @SuppressWarnings("unused")
  private boolean hasSessionIdsIn(final ObservableList<Integer> ids) {
    return ids.stream().anyMatch(
        conflictedId -> getItems().stream().anyMatch(
            (SessionFacade session) -> session.getSession().getId() == conflictedId));
  }

  private void initEvents() {
    setOnDragOver(this::draggedOver);
    setOnDragEntered(this::dragEntered);
    setOnDragExited(this::dragExited);
    setOnDragDropped(this::dropped);
  }

  private boolean isValidTarget(final DragEvent event) {
    return event.getDragboard().hasString()
        && event.getGestureSource() != this
        && getItems().stream().noneMatch(sessionFacade ->
        String.valueOf(sessionFacade.getId()).equals(event.getDragboard().getString()));
  }

  @SuppressWarnings("unused")
  private void draggedOver(final DragEvent event) {
    if (isValidTarget(event)) {
      event.acceptTransferModes(TransferMode.MOVE);
    }
    event.consume();
  }

  @SuppressWarnings("unused")
  private void dragEntered(final DragEvent event) {
    if (isValidTarget(event)) {
      uiDataService.moveSessionTaskProperty().set(null);
      getStyleClass().add("dragged-over");
      historyManager.historyEnabledProperty().set(false);
    }
  }

  @SuppressWarnings("unused")
  private void dragExited(final DragEvent event) {
    if (isValidTarget(event)) {
      getStyleClass().remove("dragged-over");
      historyManager.historyEnabledProperty().set(true);
    }
  }

  @SuppressWarnings( {"unused", "ResultOfMethodCallIgnored"})
  private void dropped(final DragEvent event) {
    boolean success = false;

    final Dragboard dragboard = event.getDragboard();


    if (isValidTarget(event)) {
      success = true;
      final int sessionId = Integer.parseInt(dragboard.getString());

      delayedSolverService.whenAvailable(solver -> {
        final SolverTask<Void> moveSession = solver.moveSessionTask(sessionId, slot.getDayString(),
            slot.getTime().toString());
        moveSession.setOnSucceeded(moveSessionEvent -> moveSucceededHandler(sessionId));
        moveSession.setOnFailed(moveSessionEvent -> moveFailedHandler());
        moveSession.setOnCancelled(moveSessionEvent -> moveCancelledHandler());
        if (uiDataService.runningTasksProperty().greaterThan(1).get()) {
          // set the property to give a warning in the timetable when more than one task is
          // running instead of executing the move session task right here
          uiDataService.moveSessionTaskProperty().set(moveSession);
          return;
        }
        executorService.submit(moveSession);
      });
    }

    // clear the redo history when a session is moved BY THE USER, we do this right here since we
    // also have undo changes to the history but we don't want to delete the redo history for
    // those actions
    historyManager.clearRedoHistory();

    event.setDropCompleted(success);
    event.consume();
  }

  private void moveSucceededHandler(final int sessionId) {
    delayedStore.whenAvailable(store -> {
      store.moveSession(sessionId, slot.getDayString(), slot.getTime());
      Optional<SessionFacade> optionalSessionFacade =
          sessions.stream().filter(sessionFacade -> sessionFacade.getId() == sessionId).findFirst();
      optionalSessionFacade.ifPresent(sessionFacade -> sessionFacade.setSlot(slot));
      historyManager.push(store.getLastLogEntry());
    });
  }

  private void moveCancelledHandler() {
    Platform.runLater(() -> {
      final ResourceBundle bundle = ResourceBundle.getBundle("lang.timetable");
      final Alert alert = new Alert(Alert.AlertType.WARNING);
      alert.setTitle(bundle.getString("moveCancelledTitle"));
      alert.setHeaderText(bundle.getString("moveCancelledHeader"));
      alert.setContentText(bundle.getString("moveCancelledContent"));

      alert.showAndWait();
    });
  }

  private void moveFailedHandler() {
    Platform.runLater(() -> {
      final ResourceBundle bundle = ResourceBundle.getBundle("lang.timetable");
      final Alert alert = new Alert(Alert.AlertType.ERROR);
      alert.setTitle(bundle.getString("moveFailedTitle"));
      alert.setHeaderText(bundle.getString("moveFailedHeader"));
      alert.setContentText(bundle.getString("moveFailedContent"));

      alert.showAndWait();
    });
  }

  public void setSessions(final ListProperty<SessionFacade> sessions) {
    this.sessions = sessions;
  }
}
