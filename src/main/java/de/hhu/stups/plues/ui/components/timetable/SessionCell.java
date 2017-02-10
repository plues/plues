package de.hhu.stups.plues.ui.components.timetable;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.ui.layout.Inflater;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

class SessionCell extends ListCell<SessionFacade> implements Initializable {

  private static final String CONFLICTED_SESSION = "conflicted-session";
  private final Router router;
  private final Delayed<SolverService> delayedSolverService;

  private final UiDataService uiDataService;

  private final HBox content = new HBox();

  private volatile boolean solverIsLoaded = false;

  @FXML
  @SuppressWarnings("unused")
  private Label sessionCellText;

  @FXML
  @SuppressWarnings("unused")
  private Text sessionCellIsTentative;

  @Inject
  SessionCell(final Inflater inflater,
              final Router router,
              final Delayed<SolverService> delayedSolverService,
              final UiDataService uiDataService) {
    super();

    this.router = router;
    this.delayedSolverService = delayedSolverService;
    this.uiDataService = uiDataService;

    waitForSolver();

    inflater.inflate("components/SessionCell", content, this);
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    setOnDragDetected(this::dragItem);
    setOnMousePressed(this::clickItem);

    setupDataService();
  }

  private void setupDataService() {
    uiDataService.conflictMarkedSessionsProperty()
        .addListener((observable, oldValue, newValue) -> setConflictedStyleClass(newValue));

    itemProperty().addListener((observable, oldValue, newValue) -> {
      sessionCellIsTentative.setText("");
      if (newValue != null && newValue.isTentative()) {
        sessionCellIsTentative.setText("T: ");
      }
    });

    sessionCellText.textProperty().bind(Bindings.createStringBinding(() -> {
      if (getItem() == null) {
        return null;
      }
      return displayText(getItem());
    }, itemProperty(), uiDataService.sessionDisplayFormatProperty()));
  }

  private void setConflictedStyleClass(final List<Integer> sessionIDs) {
    getStyleClass().remove(CONFLICTED_SESSION);

    if (getItem() != null && sessionIDs.contains(getItem().getId())) {
      getStyleClass().add(CONFLICTED_SESSION);
    }
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
    final ClipboardContent clipboardContent = new ClipboardContent();
    clipboardContent.putString(String.valueOf(getItem().getId()));
    dragboard.setContent(clipboardContent);
    event.consume();
  }

  @SuppressWarnings("unused")
  private void clickItem(final MouseEvent event) {
    if (getItem() == null || event.getClickCount() < 2) {
      return;
    }

    router.transitionTo(RouteNames.SESSION_DETAIL_VIEW, getItem().getSession());
  }

  @Override
  protected void updateItem(final SessionFacade item, final boolean empty) {
    super.updateItem(item, empty);

    if (!empty && item != null) {
      setConflictedStyleClass(uiDataService.getConflictMarkedSessions());
      setGraphic(content);
    } else {
      removeStyleClasses();
      setGraphic(null);
    }
  }

  private void removeStyleClasses() {
    getStyleClass().remove(CONFLICTED_SESSION);
  }

  @SuppressWarnings("unused")
  private String displayText(final SessionFacade sessionFacade) {
    final SessionDisplayFormat displayFormat = uiDataService.sessionDisplayFormatProperty().get();
    return sessionFacade.displayText(displayFormat);
  }
}
