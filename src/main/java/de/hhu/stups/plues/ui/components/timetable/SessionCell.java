package de.hhu.stups.plues.ui.components.timetable;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.data.sessions.SessionFacade;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.services.UiDataService;

import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.stage.Stage;

class SessionCell extends ListCell<SessionFacade> {

  private final Provider<DetailView> provider;
  private final Delayed<SolverService> delayedSolverService;

  private final UiDataService uiDataService;

  private SessionFacade.Slot slot;

  private volatile boolean solverIsLoaded = false;

  @Inject
  SessionCell(final Provider<DetailView> detailViewProvider,
              final Delayed<SolverService> delayedSolverService,
              final UiDataService uiDataService) {
    super();

    this.provider = detailViewProvider;
    this.delayedSolverService = delayedSolverService;
    this.uiDataService = uiDataService;

    waitForSolver();

    setOnDragDetected(this::dragItem);
    setOnMousePressed(this::clickItem);

    setupDataService();
  }

  private void setupDataService() {
    this.uiDataService.conflictMarkedSessionsProperty()
        .addListener((observable, oldValue, newValue) -> {
          getStyleClass().remove("conflicted-session");

          if (getItem() != null && newValue.contains(getItem().getSession().getId())) {
            getStyleClass().add("conflicted-session");
          }
        });
  }

  private void waitForSolver() {
    delayedSolverService.whenAvailable(solver -> solverIsLoaded = true);
  }

  @SuppressWarnings("unused")
  private void dragItem(final MouseEvent event) {
    if (getItem() == null || !solverIsLoaded) {
      return;
    }

    final Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
    final ClipboardContent content = new ClipboardContent();
    content.putString(String.valueOf(getItem().getSession().getId()));
    dragboard.setContent(content);
    event.consume();
  }

  @SuppressWarnings("unused")
  private void clickItem(final MouseEvent event) {
    if (getItem() == null || event.getClickCount() < 2) {
      return;
    }
    final Session session = getItem().getSession();

    final DetailView detailView = provider.get();
    detailView.setContent(session, slot);

    final Stage stage = new Stage();
    stage.setTitle(detailView.getTitle());
    stage.setScene(new Scene(detailView));
    stage.show();
  }

  @Override
  protected void updateItem(final SessionFacade session, final boolean empty) {
    super.updateItem(session, empty);

    setText(empty || session == null ? null : session.toString());
  }

  public void setSlot(final SessionFacade.Slot slot) {
    this.slot = slot;
  }
}
