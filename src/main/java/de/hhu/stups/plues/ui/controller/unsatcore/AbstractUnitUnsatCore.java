package de.hhu.stups.plues.ui.controller.unsatcore;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.ModuleAbstractUnitSemester;
import de.hhu.stups.plues.data.entities.ModuleAbstractUnitType;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.components.detailview.DetailViewHelper;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

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
  private final ListProperty<AbstractUnit> abstractUnits;
  private final Router router;

  @FXML
  @SuppressWarnings("unused")
  private TableView<AbstractUnit> abstractUnitsTable;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<AbstractUnit, Map<Module, List<Integer>>> abstractUnitModuleSemester;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<AbstractUnit, Map<Module, Character>> abstractUnitModuleType;
  @FXML
  @SuppressWarnings("unused")
  private UnsatCoreButtonBar unsatCoreButtonBar;

  /**
   * Default constructor.
   */
  @Inject
  public AbstractUnitUnsatCore(final Inflater inflater,
                               final Router router) {
    this.router = router;

    modules = new SimpleListProperty<>(FXCollections.emptyObservableList());
    abstractUnits = new SimpleListProperty<>(FXCollections.emptyObservableList());

    inflater.inflate("components/unsatcore/AbstractUnitUnsatCore",
        this, this, "unsatCore", "Column");
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    this.visibleProperty().bind(abstractUnits.emptyProperty().not());

    abstractUnitsTable.itemsProperty().bind(abstractUnits);
    abstractUnitsTable.setOnMouseClicked(DetailViewHelper.getAbstractUnitMouseHandler(
        abstractUnitsTable, router));

    abstractUnitModuleSemester.setCellValueFactory(param -> {
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
    abstractUnitModuleSemester.setCellFactory(param ->
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

    abstractUnitModuleType.setCellValueFactory(param -> {
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
    abstractUnitModuleType.setCellFactory(param ->
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

  void resetTaskState() {
    unsatCoreButtonBar.resetTaskState();
  }

  public void setAbstractUnits(final ObservableList<AbstractUnit> abstractUnits) {
    this.abstractUnits.set(abstractUnits);
  }

  ListProperty<AbstractUnit> getAbstractUnits() {
    return abstractUnits;
  }

  void configureButton(final String text,
                       final BooleanBinding binding,
                       final EventHandler<MouseEvent> eventHandler) {
    unsatCoreButtonBar.configureButton(text, binding, eventHandler);
  }

  void showTaskState(final SolverTask task, final ResourceBundle resources) {
    unsatCoreButtonBar.showTaskState(task, resources);
  }
}
