package de.hhu.stups.plues.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.ObservableStore;
import de.hhu.stups.plues.data.entities.Log;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

/**
 * The history manager to undo or redo session move operations. The history is disabled if a session
 * is currently dragged but not dropped yet.
 */
@Singleton
public class HistoryManager {

  private final Delayed<SolverService> delayedSolverService;
  private final Delayed<ObservableStore> delayedStore;
  private final ListProperty<Log> undoSessionMoveHistory;
  private final ListProperty<Log> redoSessionMoveHistory;
  private final BooleanProperty historyEnabledProperty;
  private final UiDataService uiDataService;

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
    this.uiDataService = uiDataService;

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
    undoSessionMoveHistory.add(undoSessionMoveHistory.size(), log);
  }

  /**
   * Undo the last move operation using the {@link #undoSessionMoveHistory}. This operation is
   * disabled when a session is currently dragged since we cannot control the order of a dropped
   * session and undoing a session move operation at nearly the same time.
   */
  public void undoLastMoveOperation() {
    if (!undoSessionMoveHistory.isEmpty() && historyEnabledProperty.get()) {
      delayedSolverService.whenAvailable(SolverService::undoLastMoveOperation);
      final int lastIndex = undoSessionMoveHistory.size() - 1;
      final Log currentLog = undoSessionMoveHistory.get(lastIndex);
      delayedStore.whenAvailable(observableStore ->
          observableStore.undoLastMoveOperation(currentLog));
      uiDataService.highlightSessionProperty().set(currentLog.getSession());
      undoSessionMoveHistory.remove(lastIndex);
      redoSessionMoveHistory.add(redoSessionMoveHistory.size(), currentLog);
    }
  }

  /**
   * Redo the last move operation using the {@link #redoSessionMoveHistory}. This operation can be
   * disabled (see {@link #undoLastMoveOperation()}.
   */
  public void redoLastMoveOperation() {
    if (!redoSessionMoveHistory.isEmpty() && historyEnabledProperty().get()) {
      delayedSolverService.whenAvailable(SolverService::redoLastMoveOperation);
      final int lastIndex = redoSessionMoveHistory.size() - 1;
      final Log currentLog = redoSessionMoveHistory.get(lastIndex);
      delayedStore.whenAvailable(observableStore ->
          observableStore.redoLastMoveOperation(currentLog));
      uiDataService.highlightSessionProperty().set(currentLog.getSession());
      redoSessionMoveHistory.remove(lastIndex);
      push(currentLog);
    }
  }

  /**
   * Undo all move operations.
   */
  public void undoAllMoveOperations() {
    while (!undoSessionMoveHistory.isEmpty()) {
      undoLastMoveOperation();
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
