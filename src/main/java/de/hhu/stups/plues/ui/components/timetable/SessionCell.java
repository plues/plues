package de.hhu.stups.plues.ui.components.timetable;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.sessions.SessionFacade;
import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.services.UiDataService;

import de.hhu.stups.plues.ui.layout.Inflater;
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
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

class SessionCell extends ListCell<SessionFacade> implements Initializable {

  private final Router router;
  private final Delayed<SolverService> delayedSolverService;

  private final UiDataService uiDataService;

  private final HBox content = new HBox();

  private volatile boolean solverIsLoaded = false;

  @FXML
  private Label sessionCellText;

  @FXML
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

    sessionCellText.textProperty().bind(new StringBinding() {
      {
        bind(itemProperty());
        bind(uiDataService.sessionDisplayFormatProperty());
      }

      @Override
      protected String computeValue() {
        if (getItem() == null) {
          return null;
        }

        return displayText(getItem());
      }
    });
  }

  private void setConflictedStyleClass(List<Integer> sessionIDs) {
    getStyleClass().remove("conflicted-session");

    if (getItem() != null && sessionIDs.contains(getItem().getId())) {
      getStyleClass().add("conflicted-session");
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
    final ClipboardContent content = new ClipboardContent();
    content.putString(String.valueOf(getItem().getId()));
    dragboard.setContent(content);
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
      setGraphic(null);
    }
  }

  private String displayText(final SessionFacade sessionFacade) {
    final String representation;

    if ("name".equals(uiDataService.sessionDisplayFormatProperty().get())) {
      representation = sessionFacade.toString();
    } else if ("key".equals(uiDataService.sessionDisplayFormatProperty().get())) {
      final String unitKeys = sessionFacade.getAbstractUnitKeys().stream()
          .map(this::trimUnitKey).collect(Collectors.joining(", "));
      // display session title if there are no abstract units
      representation = unitKeys.isEmpty() ? sessionFacade.toString() : unitKeys;
    } else {
      representation = String.format("%s/%d", sessionFacade.getUnitKey(),
          sessionFacade.getGroupId());
    }
    return representation;
  }

  /**
   * Adapt a unit key to be displayed within the timetable view, i.e. remove the key's prefix for
   * WiWi data like 'W-WiWi' or 'W-Wichem' and for all other data remove the first letter in the
   * key, e.g. 'P-..'.
   */
  private String trimUnitKey(final String unitKey) {
    final List<String> splitedKey = Arrays.asList(unitKey.split("-"));
    if ("w".equalsIgnoreCase(splitedKey.get(0))) {
      return splitedKey.subList(2, splitedKey.size()).stream()
          .collect(Collectors.joining("-"));
    } else {
      return splitedKey.subList(1, splitedKey.size()).stream()
          .collect(Collectors.joining("-"));
    }
  }
}
