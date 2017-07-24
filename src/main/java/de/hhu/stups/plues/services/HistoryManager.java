package de.hhu.stups.plues.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.ObservableStore;
import de.hhu.stups.plues.data.entities.Log;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The history manager to undo or redo session move operations. The history is disabled if a session
 * is currently dragged but not dropped yet.
 */
@Singleton
public class HistoryManager {

  private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();

  private final Delayed<SolverService> delayedSolverService;
  private final Delayed<ObservableStore> delayedStore;
  private final ListProperty<Log> undoSessionMoveHistory;
  private final ListProperty<Log> redoSessionMoveHistory;
  private final BooleanProperty historyEnabledProperty;

  /**
   * Create the history manager and inject the necessary components. Clear the {@link
   * #undoSessionMoveHistory} when the changes are saved by the user.
   */
  @Inject
  public HistoryManager(final Delayed<SolverService> delayedSolverService,
                        final Delayed<ObservableStore> delayedStore,
                        final UiDataService uiDataService) {
    this.delayedSolverService = delayedSolverService;
    this.delayedStore = delayedStore;

    undoSessionMoveHistory = new SimpleListProperty<>(FXCollections.observableArrayList());
    redoSessionMoveHistory = new SimpleListProperty<>(FXCollections.observableArrayList());
    historyEnabledProperty = new SimpleBooleanProperty(true);

    // clear the history when the current state of the store is saved
    uiDataService.lastSavedDateProperty().addListener((observable, oldValue, newValue) ->
        undoSessionMoveHistory.clear());
  }

  /**
   * Push a {@link Log} on the {@link #undoSessionMoveHistory}.
   */
  public void push(final Log log) {
    push(log, undoSessionMoveHistory);
  }

  /**
   * Push a {@link Log} on a specific history stack which is either {@link #undoSessionMoveHistory}
   * or {@link #redoSessionMoveHistory}.
   */
  private void push(final Log log,
                    final ListProperty<Log> sessionMoveHistory) {
    sessionMoveHistory.add(sessionMoveHistory.size(), log);
  }

  /**
   * Pop from a specific history stack.
   */
  private Log pop(final ListProperty<Log> sessionMoveHistory) {
    final int lastIndex = sessionMoveHistory.size() - 1;
    final Log log = sessionMoveHistory.get(lastIndex);
    sessionMoveHistory.remove(lastIndex);
    return log;
  }

  /**
   * Undo the last move operation using the {@link #undoSessionMoveHistory}. This operation is
   * disabled when a session is currently dragged since we cannot control the order of a dropped
   * session and undoing a session move operation at nearly the same time.
   */
  public void undoLastMoveOperation() {
    if (!undoSessionMoveHistory.isEmpty() && historyEnabledProperty.get()) {
      historyEnabledProperty().set(false);
      final Log currentLog = pop(undoSessionMoveHistory);
      delayedSolverService.whenAvailable(SolverService::undoLastMoveOperation);
      delayedStore.whenAvailable(observableStore ->
          observableStore.undoLastMoveOperation(currentLog));
      push(currentLog, redoSessionMoveHistory);
    }
  }

  /**
   * Redo the last move operation using the {@link #redoSessionMoveHistory}. This operation can be
   * disabled (see {@link #undoLastMoveOperation()}.
   */
  public void redoLastMoveOperation() {
    if (!redoSessionMoveHistory.isEmpty() && historyEnabledProperty().get()) {
      historyEnabledProperty().set(false);
      final Log currentLog = pop(redoSessionMoveHistory);
      delayedSolverService.whenAvailable(SolverService::redoLastMoveOperation);
      delayedStore.whenAvailable(observableStore ->
          observableStore.redoLastMoveOperation(currentLog));
      push(currentLog);
    }
  }

  /**
   * Undo all move operations.
   */
  public void undoAllMoveOperations() {
    EXECUTOR_SERVICE.execute(this::undoAllMoveOperationsLoop);
  }

  private void undoAllMoveOperationsLoop() {
    while (!undoSessionMoveHistory.isEmpty()) {
      Platform.runLater(this::undoLastMoveOperation);
      try {
        TimeUnit.MILLISECONDS.sleep(100);
      } catch (final InterruptedException interruptedException) {
        final Logger logger = LoggerFactory.getLogger(getClass());
        logger.error("Undoing all session move operations has been interrupted.",
            interruptedException);
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * We want to disable the undo operation when a session is dragged but not dropped yet.
   */
  public BooleanProperty historyEnabledProperty() {
    return historyEnabledProperty;
  }

  public ReadOnlyBooleanProperty undoHistoryEmptyProperty() {
    return undoSessionMoveHistory.emptyProperty();
  }

  public ReadOnlyBooleanProperty redoHistoryEmptyProperty() {
    return redoSessionMoveHistory.emptyProperty();
  }

  /**
   * Clear the {@link #redoSessionMoveHistory}. Mainly when a session has been moved <b>by the
   * user</b>, not when we moved a session via undo.
   */
  public void clearRedoHistory() {
    redoSessionMoveHistory.clear();
  }
}
