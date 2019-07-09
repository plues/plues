package de.hhu.stups.plues.ui.components.unsatcore;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.ui.components.detailview.DetailViewHelper;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class SessionUnsatCore extends VBox {

  private final ListProperty<Session> sessionsProperty;
  private final ListProperty<Course> coursesProperty;
  private final Router router;
  private final UiDataService uiDataService;

  @FXML
  private ResourceBundle resources;

  @FXML
  @SuppressWarnings("unused")
  private Button btHighlightConflicts;
  @FXML
  @SuppressWarnings("unused")
  private TableView<Session> sessionsTable;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Session, String> tableColumnSessionDay;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Session, Integer> tableColumnSessionTime;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Session, String> tableColumnSessionUnitKey;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Session, String> tableColumnSessionUnitTitle;
  @FXML
  @SuppressWarnings("unused")
  private Text txtExplanation;

  /**
   * Default constructor.
   */
  @Inject
  public SessionUnsatCore(final Inflater inflater,
                          final Router router,
                          final UiDataService uiDataService) {
    sessionsProperty = new SimpleListProperty<>(FXCollections.emptyObservableList());
    coursesProperty = new SimpleListProperty<>(FXCollections.emptyObservableList());
    this.router = router;
    this.uiDataService = uiDataService;

    inflater.inflate("components/unsatcore/SessionUnsatCore",
        this, this, "unsatCore", "Column", "Days");
  }

  @FXML
  public void initialize() {
    txtExplanation.wrappingWidthProperty().bind(widthProperty().subtract(300));

    sessionsTable.itemsProperty().bind(sessionsProperty);
    sessionsTable.setOnMouseClicked(DetailViewHelper.getSessionMouseHandler(
        sessionsTable, router));
    tableColumnSessionDay.setCellFactory(param -> new TableCell<Session, String>() {
      @Override
      protected void updateItem(final String item, final boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
          setText("");
          return;
        }

        setText(resources.getString(item));
      }
    });
    tableColumnSessionTime.setCellFactory(param -> new TableCell<Session, Integer>() {
      @Override
      protected void updateItem(final Integer item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
          setText("");
          return;
        }

        setText(String.valueOf(6 + item * 2) + ":30");
      }
    });
    tableColumnSessionUnitKey.setCellValueFactory(param
        -> Bindings.selectString(param, "value", "group", "unit", "key"));
    tableColumnSessionUnitTitle.setCellValueFactory(param
        -> Bindings.selectString(param, "value", "group", "unit", "title"));
  }

  /**
   * Highlight the conflicted sessions in the {@link de.hhu.stups.plues.ui.controller.Timetable}.
   */
  @FXML
  @SuppressWarnings("unused")
  public void highlightInTimetable() {
    uiDataService.setConflictMarkedSessions(FXCollections.observableArrayList(
        sessionsProperty.get().stream().map(Session::getId).collect(Collectors.toList())));
    router.transitionTo(RouteNames.CONFLICT_IN_TIMETABLE, coursesProperty.get().toArray());
  }

  public ListProperty<Course> coursesProperty() {
    return coursesProperty;
  }

  public void setSessions(final ObservableList<Session> sessions) {
    this.sessionsProperty.set(sessions);
  }

  public ObservableList<Session> getSessions() {
    return sessionsProperty.get();
  }

  public ListProperty<Session> sessionProperty() {
    return sessionsProperty;
  }
}
