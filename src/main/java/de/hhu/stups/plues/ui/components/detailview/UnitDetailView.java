package de.hhu.stups.plues.ui.components.detailview;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Provider;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.data.entities.Unit;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ListBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

public class UnitDetailView extends VBox implements Initializable {

  private final ObjectProperty<Unit> unitProperty;
  private final Router router;

  @FXML
  private Label key;
  @FXML
  private Label title;
  @FXML
  private Label semesters;
  @FXML
  private TableView<AbstractUnit> abstractUnitTableView;
  @FXML
  private TableView<Session> sessionTableView;
  @FXML
  private TableColumn<Session, String> columnDay;
  @FXML
  private TableColumn<Session, String> columnTime;

  /**
   * Constructor to create unitDetailView.
   * @param inflater Inflater to handle fxml and lang files.
   * @param router Router to open window
   */
  @Inject
  public UnitDetailView(final Inflater inflater,
                        final Router router) {
    this.unitProperty = new SimpleObjectProperty<>();
    this.router = router;

    inflater.inflate("components/detailview/UnitDetailView", this, this,
        "detailView", "Column", "Days");
  }

  public void setUnit(final Unit unit) {
    this.unitProperty.set(unit);
  }

  public String getTitle() {
    return unitProperty.get().getTitle();
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    key.textProperty().bind(Bindings.when(unitProperty.isNotNull()).then(
        Bindings.selectString(unitProperty, "key")).otherwise(""));
    title.textProperty().bind(Bindings.when(unitProperty.isNotNull()).then(
        Bindings.selectString(unitProperty, "title")).otherwise(""));
    semesters.textProperty().bind(new StringBinding() {
      {
        bind(unitProperty);
      }

      @Override
      protected String computeValue() {
        final Unit unit = unitProperty.get();
        if (unit == null) {
          return "";
        }

        return Joiner.on(", ").join(unit.getSemesters());
      }
    });

    columnDay.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(
        resources.getString(param.getValue().getDay())));
    columnDay.setCellFactory(param -> new SessionStringTableCell());

    columnTime.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(
        String.valueOf(6 + param.getValue().getTime() * 2) + ":30"));
    columnTime.setCellFactory(param -> new SessionStringTableCell());

    abstractUnitTableView.itemsProperty().bind(new ListBinding<AbstractUnit>() {
      {
        bind(unitProperty);
      }

      @Override
      protected ObservableList<AbstractUnit> computeValue() {
        Unit unit = unitProperty.get();
        if (unit == null) {
          return FXCollections.emptyObservableList();
        }

        return FXCollections.observableArrayList(unit.getAbstractUnits());
      }
    });
    sessionTableView.itemsProperty().bind(new ListBinding<Session>() {
      {
        bind(unitProperty);
      }

      @Override
      protected ObservableList<Session> computeValue() {
        Unit unit = unitProperty.get();
        if (unit == null) {
          return FXCollections.emptyObservableList();
        }

        final Set<Session> sessions = unit.getGroups().stream()
            .flatMap(group -> group.getSessions().stream()).collect(Collectors.toSet());
        return FXCollections.observableArrayList(sessions);
      }
    });

    abstractUnitTableView.setOnMouseClicked(DetailViewHelper.getAbstractUnitMouseHandler(
        abstractUnitTableView, router));
    sessionTableView.setOnMouseClicked(DetailViewHelper.getSessionMouseHandler(
        sessionTableView, router));
  }

  private static class SessionStringTableCell extends TableCell<Session, String> {
    @Override
    protected void updateItem(String day, boolean empty) {
      super.updateItem(day, empty);
      if (day == null || empty) {
        setText(null);
        return;
      }

      setText(day);
    }
  }
}
