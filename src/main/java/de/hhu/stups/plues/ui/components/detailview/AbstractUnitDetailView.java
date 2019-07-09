package de.hhu.stups.plues.ui.components.detailview;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.ModuleAbstractUnitSemester;
import de.hhu.stups.plues.data.entities.ModuleAbstractUnitType;
import de.hhu.stups.plues.data.entities.Unit;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ListBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.util.stream.Collectors;

public class AbstractUnitDetailView extends VBox implements DetailView {

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

  @FXML
  public void initialize() {
    initializeLabels();

    tableViewUnits.itemsProperty().bind(new UnitListBinding(abstractUnitProperty));
    tableViewModules.itemsProperty().bind(new ModuleListBinding(abstractUnitProperty));

    initializeTableColumnModuleSemesters();
    initializeTableColumnModulesTypes();
    initializeEventHandlers();
  }

  private void initializeTableColumnModulesTypes() {
    tableColumnModulesType.setCellValueFactory(param -> {
      final AbstractUnit abstractUnit = this.abstractUnitProperty.get();
      final ObservableList<Module> modules = this.tableViewModules.getItems();
      return  new ReadOnlyStringWrapper(param.getValue().getModuleAbstractUnitTypes().stream()
        .filter(moduleAbstractUnitSemester
            -> abstractUnit.equals(moduleAbstractUnitSemester.getAbstractUnit()))
        .filter(moduleAbstractUnitSemester
            -> modules.contains(moduleAbstractUnitSemester.getModule()))
        .map(ModuleAbstractUnitType::getType)
        .distinct()
        .map(String::valueOf)
        .collect(Collectors.joining(", ")));
    });
    tableColumnModulesType.setCellFactory(param -> DetailViewHelper.createTableCell());
  }

  private void initializeTableColumnModuleSemesters() {
    tableColumnModulesSemesters.setCellValueFactory(param -> {
      final AbstractUnit abstractUnit = this.abstractUnitProperty.get();
      final ObservableList<Module> modules = this.tableViewModules.getItems();
      final String semesters = param.getValue().getModuleAbstractUnitSemesters().stream()
          .filter(moduleAbstractUnitSemester
              -> abstractUnit.equals(moduleAbstractUnitSemester.getAbstractUnit()))
          .filter(moduleAbstractUnitSemester
              -> modules.contains(moduleAbstractUnitSemester.getModule()))
          .map(ModuleAbstractUnitSemester::getSemester)
          .sorted()
          .map(String::valueOf)
          .collect(Collectors.joining(","));
      return new ReadOnlyStringWrapper(semesters);
    });

    tableColumnModulesSemesters.setCellFactory(param -> DetailViewHelper.createTableCell());
  }

  private void initializeEventHandlers() {
    tableViewUnits.setOnMouseClicked(DetailViewHelper.getUnitMouseHandler(
        tableViewUnits, router));
    tableViewModules.setOnMouseClicked(DetailViewHelper.getModuleMouseHandler(
        tableViewModules, router));
  }

  private void initializeLabels() {
    key.textProperty().bind(Bindings.when(abstractUnitProperty.isNotNull())
        .then(Bindings.selectString(abstractUnitProperty, "key"))
        .otherwise(""));

    title.textProperty().bind(Bindings.when(abstractUnitProperty.isNotNull())
        .then(Bindings.selectString(abstractUnitProperty, "title"))
        .otherwise(""));
  }

  private static class UnitListBinding extends ListBinding<Unit> {
    private final ObjectProperty<AbstractUnit> property;

    private UnitListBinding(final ObjectProperty<AbstractUnit> abstractUnitProperty) {
      this.property = abstractUnitProperty;
      bind(property);
    }

    @Override
    protected ObservableList<Unit> computeValue() {
      final AbstractUnit abstractUnit = property.get();
      if (abstractUnit == null) {
        return FXCollections.emptyObservableList();
      }

      return FXCollections.observableArrayList(abstractUnit.getUnits());
    }
  }

  private static class ModuleListBinding extends ListBinding<Module> {
    private final ObjectProperty<AbstractUnit> property;

    private ModuleListBinding(final ObjectProperty<AbstractUnit> abstractUnitProperty) {
      this.property = abstractUnitProperty;
      bind(property);
    }

    @Override
    protected ObservableList<Module> computeValue() {
      final AbstractUnit abstractUnit = property.get();
      if (abstractUnit == null) {
        return FXCollections.emptyObservableList();
      }

      return FXCollections.observableArrayList(abstractUnit.getModules());
    }
  }
}
