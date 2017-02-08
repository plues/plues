package de.hhu.stups.plues.ui.components.detailview;

import com.google.common.base.Joiner;
import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.data.entities.Unit;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.ui.layout.Inflater;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ListBinding;
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
import java.util.stream.Collectors;

public class UnitDetailView extends VBox implements Initializable {

  private final ObjectProperty<Unit> unitProperty;
  private final Router router;

  @FXML
  @SuppressWarnings("unused")
  private Label key;
  @FXML
  @SuppressWarnings("unused")
  private Label title;
  @FXML
  @SuppressWarnings("unused")
  private Label semesters;
  @FXML
  @SuppressWarnings("unused")
  private TableView<AbstractUnit> abstractUnitTableView;
  @FXML
  @SuppressWarnings("unused")
  private TableView<Session> sessionTableView;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<AbstractUnit, String> tableColumnAbstractUnitKey;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<AbstractUnit, String> tableColumnAbstractUnitTitle;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Session, String> tableColumnSessionId;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Session, String> tableColumnSessionDay;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Session, String> tableColumnSessionTime;

  /**
   * Constructor to create unitDetailView.
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
    key.textProperty().bind(Bindings.selectString(unitProperty, "key"));

    title.textProperty().bind(Bindings.selectString(unitProperty, "title"));

    semesters.textProperty().bind(Bindings.createStringBinding(() -> {
      final Unit unit = unitProperty.get();
      if (unit == null) {
        return "";
      }

      return Joiner.on(", ").join(unit.getSemesters());
    }, unitProperty));

    tableColumnSessionDay.setCellValueFactory(param
        -> new ReadOnlyObjectWrapper<>(resources.getString(param.getValue().getDay())));

    tableColumnSessionDay.setCellFactory(param -> new SessionStringTableCell());

    tableColumnSessionTime.setCellValueFactory(param
        -> new ReadOnlyObjectWrapper<>(String.valueOf(6 + param.getValue().getTime() * 2) + ":30"));

    tableColumnSessionTime.setCellFactory(param -> new SessionStringTableCell());

    bindTableColumnsWidth();

    abstractUnitTableView.itemsProperty().bind(new AbstractUnitTableItemsBinding());
    sessionTableView.itemsProperty().bind(new SessionTableItemsBinding());

    abstractUnitTableView.setOnMouseClicked(DetailViewHelper.getAbstractUnitMouseHandler(
        abstractUnitTableView, router));
    sessionTableView.setOnMouseClicked(DetailViewHelper.getSessionMouseHandler(
        sessionTableView, router));

    tableColumnSessionId.setCellValueFactory(param
        -> new ReadOnlyObjectWrapper<>(String.valueOf(param.getValue().getGroup().getId())));
  }

  private void bindTableColumnsWidth() {
    tableColumnAbstractUnitKey.prefWidthProperty().bind(
        abstractUnitTableView.widthProperty().multiply(0.25));
    tableColumnAbstractUnitTitle.prefWidthProperty().bind(
        abstractUnitTableView.widthProperty().multiply(0.71));

    tableColumnSessionId.prefWidthProperty().bind(
        sessionTableView.widthProperty().multiply(0.2));
    tableColumnSessionDay.prefWidthProperty().bind(
        sessionTableView.widthProperty().multiply(0.38));
    tableColumnSessionTime.prefWidthProperty().bind(
        sessionTableView.widthProperty().multiply(0.38));
  }

  private static class SessionStringTableCell extends TableCell<Session, String> {
    @Override
    protected void updateItem(final String day, final boolean empty) {
      super.updateItem(day, empty);
      if (day == null || empty) {
        setText(null);
        return;
      }

      setText(day);
    }
  }

  private class SessionTableItemsBinding extends ListBinding<Session> {
    private SessionTableItemsBinding() {
      bind(unitProperty);
    }

    @Override
    protected ObservableList<Session> computeValue() {
      final Unit unit = unitProperty.get();
      if (unit == null) {
        return FXCollections.emptyObservableList();
      }

      return unit.getGroups().stream()
          .flatMap(group -> group.getSessions().stream())
          .collect(
              Collectors.collectingAndThen(Collectors.toList(), FXCollections::observableList));
    }
  }

  private class AbstractUnitTableItemsBinding extends ListBinding<AbstractUnit> {
    private AbstractUnitTableItemsBinding() {
      bind(unitProperty);
    }

    @Override
    protected ObservableList<AbstractUnit> computeValue() {
      final Unit unit = unitProperty.get();
      if (unit == null) {
        return FXCollections.emptyObservableList();
      }

      return FXCollections.observableArrayList(unit.getAbstractUnits());
    }
  }
}
