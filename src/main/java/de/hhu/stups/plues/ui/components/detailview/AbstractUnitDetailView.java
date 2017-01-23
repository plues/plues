package de.hhu.stups.plues.ui.components.detailview;

import com.google.common.base.Joiner;
import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.ModuleAbstractUnitSemester;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

public class AbstractUnitDetailView extends VBox implements Initializable {

  private final ObjectProperty<AbstractUnit> abstractUnitProperty;
  private final Router router;

  @FXML
  @SuppressWarnings("unused")
  private Label key;
  @FXML
  @SuppressWarnings("unused")
  private Label title;
  @FXML
  @SuppressWarnings("unused")
  private TableView<Unit> tableViewUnits;
  @FXML
  @SuppressWarnings("unused")
  private TableView<Module> tableViewModules;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, String> tableColumnUnitsKey;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, String> tableColumnUnitsTitle;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, String> tableColumnModulesPordnr;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, String> tableColumnModulesTitle;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, String> tableColumnModulesSemesters;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, String> tableColumnModulesType;

  /**
   * Default constructor.
   *
   * @param inflater Inflater to handle fxml and lang
   */
  @Inject
  public AbstractUnitDetailView(final Inflater inflater,
                                final Router router) {
    abstractUnitProperty = new SimpleObjectProperty<>();
    this.router = router;

    inflater.inflate("components/detailview/AbstractUnitDetailView", this, this,
        "detailView", "Column");
  }

  /**
   * Set property for this detail view.
   *
   * @param abstractUnit Unit for property containing displayed data
   */
  public void setAbstractUnit(final AbstractUnit abstractUnit) {
    abstractUnitProperty.set(abstractUnit);
  }

  public String getTitle() {
    return title.getText();
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    key.textProperty().bind(Bindings.when(abstractUnitProperty.isNotNull()).then(
        Bindings.selectString(abstractUnitProperty, "key")).otherwise(""));
    title.textProperty().bind(Bindings.when(abstractUnitProperty.isNotNull()).then(
        Bindings.selectString(abstractUnitProperty, "title")).otherwise(""));

    bindTableColumnsWidth();

    tableViewUnits.itemsProperty().bind(new ListBinding<Unit>() {
      {
        bind(abstractUnitProperty);
      }

      @Override
      protected ObservableList<Unit> computeValue() {
        AbstractUnit abstractUnit = abstractUnitProperty.get();
        if (abstractUnit == null) {
          return FXCollections.emptyObservableList();
        }

        return FXCollections.observableArrayList(abstractUnit.getUnits());
      }
    });

    tableViewModules.itemsProperty().bind(new ListBinding<Module>() {
      {
        bind(abstractUnitProperty);
      }

      @Override
      protected ObservableList<Module> computeValue() {
        final AbstractUnit abstractUnit = abstractUnitProperty.get();
        if (abstractUnit == null) {
          return FXCollections.emptyObservableList();
        }

        return FXCollections.observableArrayList(abstractUnit.getModules());
      }
    });

    tableColumnModulesSemesters.setCellValueFactory(param -> {
      final Set<ModuleAbstractUnitSemester> filteredByAbstractUnit =
          param.getValue().getModuleAbstractUnitSemesters()
              .stream().filter(moduleAbstractUnitSemester ->
              this.abstractUnitProperty.get().equals(moduleAbstractUnitSemester.getAbstractUnit()))
              .collect(Collectors.toSet());
      final Set<ModuleAbstractUnitSemester> filteredByBoth =
          filteredByAbstractUnit.stream().filter(moduleAbstractUnitSemester ->
              this.tableViewModules.getItems().contains(moduleAbstractUnitSemester.getModule()))
              .collect(Collectors.toSet());

      final Set<Integer> semesters = filteredByBoth.stream()
          .map(ModuleAbstractUnitSemester::getSemester).collect(Collectors.toSet());

      return new ReadOnlyObjectWrapper<>(Joiner.on(",").join(semesters));
    });
    tableColumnModulesSemesters.setCellFactory(param -> DetailViewHelper.createTableCell());

    tableColumnModulesType.setCellValueFactory(param -> {
      if (param.getValue().getMandatory()) {
        return new ReadOnlyObjectWrapper<>("m");
      }

      return new ReadOnlyObjectWrapper<>("e");
    });
    tableColumnModulesType.setCellFactory(param -> DetailViewHelper.createTableCell());

    tableViewUnits.setOnMouseClicked(DetailViewHelper.getUnitMouseHandler(
        tableViewUnits, router));
    tableViewModules.setOnMouseClicked(DetailViewHelper.getModuleMouseHandler(
        tableViewModules, router));
  }

  private void bindTableColumnsWidth() {
    tableColumnUnitsKey.prefWidthProperty().bind(
        tableViewUnits.widthProperty().multiply(0.2));
    tableColumnUnitsTitle.prefWidthProperty().bind(
        tableViewUnits.widthProperty().multiply(0.76));

    tableColumnModulesPordnr.prefWidthProperty().bind(
        tableViewUnits.widthProperty().multiply(0.15));
    tableColumnModulesTitle.prefWidthProperty().bind(
        tableViewUnits.widthProperty().multiply(0.59));
    tableColumnModulesSemesters.prefWidthProperty().bind(
        tableViewUnits.widthProperty().multiply(0.15));
    tableColumnModulesType.prefWidthProperty().bind(
        tableViewUnits.widthProperty().multiply(0.07));
  }
}
