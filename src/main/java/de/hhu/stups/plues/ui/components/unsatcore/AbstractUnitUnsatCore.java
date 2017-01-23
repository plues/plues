package de.hhu.stups.plues.ui.components.unsatcore;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.ModuleAbstractUnitSemester;
import de.hhu.stups.plues.data.entities.ModuleAbstractUnitType;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.ui.components.detailview.DetailViewHelper;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AbstractUnitUnsatCore extends VBox implements Initializable {

  private final ListProperty<Module> modules;
  private final ListProperty<AbstractUnit> abstractUnitsProperty;
  private final Router router;

  @FXML
  @SuppressWarnings("unused")
  private TableView<AbstractUnit> abstractUnitsTable;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<AbstractUnit, String> tableColumnAbstractUnitKey;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<AbstractUnit, String> tableColumnAbstractUnitTitle;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<AbstractUnit, Map<Module, List<Integer>>>
      tableColumnAbstractUnitModuleSemester;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<AbstractUnit, Map<Module, Character>> tableColumnAbstractUnitModuleType;
  @FXML
  @SuppressWarnings("unused")
  private UnsatCoreButtonBar unsatCoreButtonBar;
  @FXML
  @SuppressWarnings("unused")
  private Text txtExplanation;

  /**
   * Default constructor.
   */
  @Inject
  public AbstractUnitUnsatCore(final Inflater inflater,
                               final Router router) {
    this.router = router;

    modules = new SimpleListProperty<>(FXCollections.emptyObservableList());
    abstractUnitsProperty = new SimpleListProperty<>(FXCollections.emptyObservableList());

    inflater.inflate("components/unsatcore/AbstractUnitUnsatCore",
        this, this, "unsatCore", "Column");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    txtExplanation.wrappingWidthProperty().bind(widthProperty().subtract(150));

    abstractUnitsTable.itemsProperty().bind(abstractUnitsProperty);
    abstractUnitsTable.setOnMouseClicked(DetailViewHelper.getAbstractUnitMouseHandler(
        abstractUnitsTable, router));

    setAbstractUnitModuleSemesterFactories();
    setAbstractUnitModuleTypeFactories();

    unsatCoreButtonBar.setText(resources.getString("button.unsatCoreGroups"));

    bindTableColumnsWidth();
  }

  private void bindTableColumnsWidth() {
    tableColumnAbstractUnitKey.prefWidthProperty().bind(
        abstractUnitsTable.widthProperty().multiply(0.15));
    tableColumnAbstractUnitTitle.prefWidthProperty().bind(
        abstractUnitsTable.widthProperty().multiply(0.59));
    tableColumnAbstractUnitModuleSemester.prefWidthProperty().bind(
        abstractUnitsTable.widthProperty().multiply(0.15));
    tableColumnAbstractUnitModuleType.prefWidthProperty().bind(
        abstractUnitsTable.widthProperty().multiply(0.07));
  }

  private void setAbstractUnitModuleSemesterFactories() {
    tableColumnAbstractUnitModuleSemester.setCellValueFactory(param -> {
      final Set<ModuleAbstractUnitSemester> maus =
          param.getValue().getModuleAbstractUnitSemesters();

      // filter ModuleAbstractUnitSemester by those modules in the current unsat core
      final Stream<ModuleAbstractUnitSemester> filtered = maus.stream().filter(
          moduleAbstractUnitSemester ->
              this.modules.contains(moduleAbstractUnitSemester.getModule()));

      // group entries by module and map to the corresponding semesters as a list
      final Map<Module, List<Integer>> result = filtered.collect(
          Collectors.groupingBy(
              ModuleAbstractUnitSemester::getModule,
              Collectors.mapping(ModuleAbstractUnitSemester::getSemester, Collectors.toList())));
      return new ReadOnlyObjectWrapper<>(result);
    });

    tableColumnAbstractUnitModuleSemester.setCellFactory(param ->
        new TableCell<AbstractUnit, Map<Module, List<Integer>>>() {
          @Override
          protected void updateItem(final Map<Module, List<Integer>> item, final boolean empty) {
            super.updateItem(item, empty);
            if (item == null || empty) {
              setText(null);
              return;
            }
            final String prefix = getPrefix(item.entrySet());
            setText(item.entrySet().stream()
                .map(e -> String.format("%s%s: %s",
                    prefix,
                    e.getKey().getPordnr(),
                    e.getValue().stream()
                        .sorted()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","))))
                .collect(Collectors.joining("\n")));
          }
        });
  }

  private void setAbstractUnitModuleTypeFactories() {
    tableColumnAbstractUnitModuleType.setCellValueFactory(param -> {
      final Set<ModuleAbstractUnitType> maus =
          param.getValue().getModuleAbstractUnitTypes();

      // filter ModuleAbstractUnitSemester by those modules in the current unsat core
      final Stream<ModuleAbstractUnitType> filtered = maus.stream().filter(
          moduleAbstractUnitType
              -> this.modules.contains(moduleAbstractUnitType.getModule()));

      // group entries by module and map to the corresponding semesters as a list
      final Map<Module, Character> result = filtered.collect(
          Collectors.toMap(ModuleAbstractUnitType::getModule, ModuleAbstractUnitType::getType));
      return new ReadOnlyObjectWrapper<>(result);
    });

    tableColumnAbstractUnitModuleType.setCellFactory(param ->
        new TableCell<AbstractUnit, Map<Module, Character>>() {
          @Override
          protected void updateItem(final Map<Module, Character> item, final boolean empty) {
            super.updateItem(item, empty);
            if (item == null || empty) {
              setText(null);
              return;
            }
            final String prefix = getPrefix(item.entrySet());
            setText(item.entrySet().stream()
                .map(e -> String.format("%s%s: %s",
                    prefix,
                    e.getKey().getPordnr(),
                    e.getValue()))
                .collect(Collectors.joining("\n")));
          }
        });
  }

  private String getPrefix(final Collection<?> item) {
    if (item.size() > 1) {
      return "• ";
    }
    return "";
  }

  public void resetTaskState() {
    unsatCoreButtonBar.resetTaskState();
  }

  public void setAbstractUnits(final ObservableList<AbstractUnit> abstractUnits) {
    this.abstractUnitsProperty.set(abstractUnits);
  }

  public ListProperty<AbstractUnit> abstractUnitsProperty() {
    return abstractUnitsProperty;
  }

  public List<AbstractUnit> getAbstractUnits() {
    return abstractUnitsProperty.get();
  }

  public UnsatCoreButtonBar getUnsatCoreButtonBar() {
    return unsatCoreButtonBar;
  }

  public void setModules(final ObservableList<Module> modules) {
    this.modules.set(modules);
  }

  public ObservableList<Module> getModules() {
    return modules.get();
  }

  public ListProperty<Module> modulesProperty() {
    return modules;
  }


}
