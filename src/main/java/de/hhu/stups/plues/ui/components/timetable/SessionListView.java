package de.hhu.stups.plues.ui.components.timetable;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.sessions.SessionFacade;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;

import javafx.beans.property.ListProperty;
import javafx.scene.control.ListView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import java.util.Optional;
import javax.annotation.Nullable;

public class SessionListView extends ListView<SessionFacade> {
  private final SessionFacade.Slot slot;
  private final Delayed<Store> delayedStore;
  private final Delayed<SolverService> delayedSolver;
  private ListProperty<SessionFacade> sessions;

  /**
   * Custom implementation of ListView for sessions.
   * @param slot the time slot identifying this session list
   */
  public SessionListView(SessionFacade.Slot slot,
      Delayed<Store> delayedStore, Delayed<SolverService> delayedSolver) {
    super();

    this.slot = slot;
    this.delayedStore = delayedStore;
    this.delayedSolver = delayedSolver;

    setCellFactory(param -> new SessionCell());

    initEvents();
  }

  private void initEvents() {
    setOnDragOver(this::draggedOver);
    setOnDragEntered(this::dragEntered);
    setOnDragExited(this::dragExited);
    setOnDragDropped(this::dropped);
  }

  private boolean isValidTarget(DragEvent event) {
    return event.getDragboard().hasString()
      && !getItems().stream().anyMatch(sessionFacade ->
        String.valueOf(sessionFacade.getSession().getId()).equals(event.getDragboard().getString()))
      && event.getGestureSource() != this;
  }

  private void draggedOver(DragEvent event) {
    if (isValidTarget(event)) {
      event.acceptTransferModes(TransferMode.MOVE);
    }

    event.consume();
  }

  private void dragEntered(DragEvent event) {
    if (isValidTarget(event)) {
      getStyleClass().add("dragged-over");
    }
  }

  private void dragExited(DragEvent event) {
    if (isValidTarget(event)) {
      getStyleClass().remove("dragged-over");
    }
  }

  private void dropped(DragEvent event) {
    boolean success = false;

    Dragboard dragboard = event.getDragboard();


    if (isValidTarget(event)) {
      success = true;
      final int sessionId = Integer.parseInt(dragboard.getString());

      delayedSolver.whenAvailable(solver -> {
        SolverTask<Void> moveSession = solver.moveSession(sessionId, slot);
        Futures.addCallback(solver.submit(moveSession), new FutureCallback<Void>() {
          @Override
          public void onSuccess(@Nullable Void result) {
            delayedStore.whenAvailable(
                store -> store.moveSession(getSessionFacadeById(sessionId), slot));
          }

          @Override
          public void onFailure(@Nullable Throwable throwable) {
            // TODO: show error message
          }
        });
      });
    }

    event.setDropCompleted(success);
    event.consume();
  }

  private SessionFacade getSessionFacadeById(int sessionId) {
    Optional<SessionFacade> session = sessions.stream()
        .filter(facade -> facade.getSession().getId() == sessionId)
        .findFirst();
    return session.isPresent() ? session.get() : null;
  }

  public void setSessions(ListProperty<SessionFacade> sessions) {
    this.sessions = sessions;
  }
}
