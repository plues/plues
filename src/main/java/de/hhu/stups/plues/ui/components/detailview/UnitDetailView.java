package de.hhu.stups.plues.ui.components.detailview;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Provider;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.data.entities.Unit;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ListBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

public class UnitDetailView extends VBox implements Initializable {

  private final ObjectProperty<Unit> unitProperty;
  private final Provider<AbstractUnitDetailView> abstractUnitDetailViewProvider;
  private Provider<SessionDetailView> sessionDetailViewProvider;

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

  /**
   * Constructor to create unitDetailView.
   * @param inflater Inflater to handle fxml and lang files.
   * @param abstractUnitDetailViewProvider Provider for abstractUnitDetailView for navigation
   * @param sessionDetailViewProvider Provider for sessionDetailView for navigation
   */
  @Inject
  public UnitDetailView(final Inflater inflater,
                        final Provider<AbstractUnitDetailView> abstractUnitDetailViewProvider,
                        final Provider<SessionDetailView> sessionDetailViewProvider) {
    this.unitProperty = new SimpleObjectProperty<>();
    this.abstractUnitDetailViewProvider = abstractUnitDetailViewProvider;
    this.sessionDetailViewProvider = sessionDetailViewProvider;

    inflater.inflate("components/detailview/UnitDetailView", this, this, "detailView", "Column");
  }

  public void setUnit(Unit unit) {
    this.unitProperty.set(unit);
  }

  public String getTitle() {
    return unitProperty.get().getTitle();
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
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
        Unit unit = unitProperty.get();
        if (unit == null) {
          return "";
        }

        return Joiner.on(", ").join(unit.getSemesters());
      }
    });

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

        Set<Session> sessions = unit.getGroups().stream()
            .flatMap(group -> group.getSessions().stream()).collect(Collectors.toSet());
        return FXCollections.observableArrayList(sessions);
      }
    });

    abstractUnitTableView.setOnMouseClicked(DetailViewHelper.getAbstractUnitMouseHandler(
        abstractUnitTableView, abstractUnitDetailViewProvider));
    sessionTableView.setOnMouseClicked(DetailViewHelper.getSessionMouseHandler(
        sessionTableView, sessionDetailViewProvider));
  }
}
