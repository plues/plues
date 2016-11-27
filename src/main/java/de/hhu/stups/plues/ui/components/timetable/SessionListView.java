package de.hhu.stups.plues.ui.components.timetable;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.ObservableStore;
import de.hhu.stups.plues.data.sessions.SessionFacade;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.tasks.SolverTask;

import javafx.beans.property.ListProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.annotation.Nullable;

public class SessionListView extends ListView<SessionFacade> {
  private final SessionFacade.Slot slot;
  private final Delayed<ObservableStore> delayedStore;
  private final Delayed<SolverService> delayedSolverService;
  private final ListeningExecutorService executorService;
  private ListProperty<SessionFacade> sessions;

  private final UiDataService uiDataService;

  /**
   * Custom implementation of ListView for sessions.
   * @param slot the time slot identifying this session list
   * @param delayedStore Store to save new session info after moving
   * @param delayedSolverService Solver to find out if moving a session is valid
   * @param uiDataService a stupid data container to dump any kind of data in it
   */
  @Inject
  public SessionListView(@Assisted final SessionFacade.Slot slot,
                         final Delayed<ObservableStore> delayedStore,
                         final Delayed<SolverService> delayedSolverService,
                         final ListeningExecutorService executorService,
                         final Provider<SessionCell> cellProvider,
                         final UiDataService uiDataService) {
    super();

    this.slot = slot;
    this.delayedStore = delayedStore;
    this.executorService = executorService;
    this.delayedSolverService = delayedSolverService;
    this.uiDataService = uiDataService;

    setCellFactory(param -> cellProvider.get());

    initEvents();
    setupConflictHighlight();
  }

  private void setupConflictHighlight() {
    this.uiDataService.conflictMarkedSessionsProperty()
        .addListener((observable, oldValue, newValue) -> {
          getStyleClass().remove("red-border");

          if (hasSessionIdsIn(this.uiDataService.conflictMarkedSessionsProperty())) {
            getStyleClass().add("red-border");
          }
        });
  }

  private boolean hasSessionIdsIn(ObservableList<Integer> ids) {
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
      && !getItems().stream().anyMatch(sessionFacade ->
        String.valueOf(sessionFacade.getId()).equals(event.getDragboard().getString()))
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

      delayedSolverService.whenAvailable(solver -> {
        final SolverTask<Void> moveSession = solver.moveSession(sessionId, slot);
        final ListenableFuture<Void> future
            = (ListenableFuture<Void>) executorService.submit(moveSession);
        Futures.addCallback(future, new FutureCallback<Void>() {
          @Override
          public void onSuccess(@Nullable final Void result) {
            delayedStore.whenAvailable(store -> {
              store.moveSession(getSessionFacadeById(sessionId), slot);
            });

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
        .filter(facade -> facade.getId() == sessionId)
        .findFirst();
    return session.isPresent() ? session.get() : null;
  }

  public void setSessions(final ListProperty<SessionFacade> sessions) {
    this.sessions = sessions;
  }
}
