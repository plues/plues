package de.hhu.stups.plues.ui.components.timetable;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.sessions.SessionFacade;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;

import org.controlsfx.control.PopOver;

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
   * @param delayedStore Store to save new session info after moving
   * @param delayedSolver Solver to find out if moving a session is valid
   */
  @Inject
  public SessionListView(@Assisted final SessionFacade.Slot slot,
                         @Assisted final PopOver sessionDetail,
                         final Delayed<Store> delayedStore,
                         final Delayed<SolverService> delayedSolver,
                         final Provider<SessionCell> cellProvider) {
    super();

    this.slot = slot;
    this.delayedStore = delayedStore;
    this.delayedSolver = delayedSolver;

    setCellFactory(param -> {
      final SessionCell sessionCell = cellProvider.get();
      sessionCell.setSlot(slot);
      sessionCell.setSessionDetail(sessionDetail);
      return sessionCell;
    });

    initEvents();
  }

  private void initEvents() {
    setOnDragOver(this::draggedOver);
    setOnDragEntered(this::dragEntered);
    setOnDragExited(this::dragExited);
    setOnDragDropped(this::dropped);
  }

  private boolean isValidTarget(final DragEvent event) {
    return event.getDragboard().hasString()
      && !getItems().stream().anyMatch(sessionFacade ->
        String.valueOf(sessionFacade.getSession().getId()).equals(event.getDragboard().getString()))
      && event.getGestureSource() != this;
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
      getStyleClass().add("dragged-over");
    }
  }

  @SuppressWarnings("unused")
  private void dragExited(final DragEvent event) {
    if (isValidTarget(event)) {
      getStyleClass().remove("dragged-over");
    }
  }

  @SuppressWarnings("unused")
  private void dropped(final DragEvent event) {
    boolean success = false;

    final Dragboard dragboard = event.getDragboard();


    if (isValidTarget(event)) {
      success = true;
      final int sessionId = Integer.parseInt(dragboard.getString());

      delayedSolver.whenAvailable(solver -> {
        final SolverTask<Void> moveSession = solver.moveSession(sessionId, slot);
        Futures.addCallback(solver.submit(moveSession), new FutureCallback<Void>() {
          @Override
          public void onSuccess(@Nullable final Void result) {
            delayedStore.whenAvailable(
                store -> store.moveSession(getSessionFacadeById(sessionId), slot));
          }

          @Override
          public void onFailure(@Nullable final Throwable throwable) {
            // TODO: show error message
          }
        });
      });
    }

    event.setDropCompleted(success);
    event.consume();
  }

  private SessionFacade getSessionFacadeById(final int sessionId) {
    final Optional<SessionFacade> session = sessions.stream()
        .filter(facade -> facade.getSession().getId() == sessionId)
        .findFirst();
    return session.isPresent() ? session.get() : null;
  }

  public void setSessions(final ListProperty<SessionFacade> sessions) {
    this.sessions = sessions;
  }
}
