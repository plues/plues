package de.hhu.stups.plues.ui.controller.unsatcore;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.ui.components.detailview.DetailViewHelper;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class SessionUnsatCore extends VBox implements Initializable {

  private final ListProperty<Session> sessions;
  private final Router router;

  @FXML
  @SuppressWarnings("unused")
  private TableView<Session> sessionsTable;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Session, String> sessionDayColumn;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Session, Integer> sessionTimeColumn;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Session, String> sessionUnitKeyColumn;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Session, String> sessionUnitTitleColumn;

  /**
   * Default constructor.
   */
  @Inject
  public SessionUnsatCore(final Inflater inflater, final Router router) {
    sessions = new SimpleListProperty<>(FXCollections.emptyObservableList());
    this.router = router;

    inflater.inflate("components/unsatcore/SessionUnsatCore",
        this, this, "unsatCore", "Column", "Days");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    sessionsTable.itemsProperty().bind(sessions);
    sessionsTable.setOnMouseClicked(DetailViewHelper.getSessionMouseHandler(
        sessionsTable, router));
    sessionDayColumn.setCellFactory(param -> new TableCell<Session, String>() {
      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
          setText("");
          return;
        }

        setText(resources.getString(item));
      }
    });
    sessionTimeColumn.setCellFactory(param -> new TableCell<Session, Integer>() {
      @Override
      protected void updateItem(Integer item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
          setText("");
          return;
        }

        setText(String.valueOf(6 + item * 2) + ":30");
      }
    });
    sessionUnitKeyColumn.setCellValueFactory(param
        -> Bindings.selectString(param, "value", "group", "unit", "key"));
    sessionUnitTitleColumn.setCellValueFactory(param
        -> Bindings.selectString(param, "value", "group", "unit", "title"));
  }

  public void setSessions(final ObservableList<Session> sessions) {
    this.sessions.set(sessions);
  }

  public ObservableList<Session> getSessions() {
    return sessions.get();
  }

  ListProperty<Session> getSessionProperty() {
    return sessions;
  }
}
