package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.Unit;
import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.ui.components.detailview.DetailViewHelper;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.ListBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ModuleAbstractUnitUnitSemesterConflicts extends VBox {

  private final SimpleListProperty<Module> modules
      = new SimpleListProperty<>(FXCollections.observableArrayList());
  private final ObservableMap<Module, List<Conflict>> moduleAbstractUnitUnitSemesterConflictsMap
      = FXCollections.observableHashMap();
  private final Router router;
  private boolean manualTableViewSelection = false;

  @FXML
  @SuppressWarnings("unused")
  private TableView<Module> tableViewModules;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, String> tableColumnModulePordnr;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, String> tableColumnModuleTitle;
  @FXML
  @SuppressWarnings("unused")
  private TableView<Conflict> tableViewAbstractUnit;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, String> tableColumnAbstractUnitKey;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, String> tableColumnAbstractUnitTitle;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, String> tableColumnAbstractUnitSemesters;
  @FXML
  @SuppressWarnings("unused")
  private TableView<Conflict> tableViewExplicitUnit;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, String> tableColumnExplicitUnitKey;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, String> tableColumnExplicitUnitTitle;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, String> tableColumnExplicitUnitSemesters;

  @FXML
  @SuppressWarnings("unused")
  private Text txtExplanation;

  /**
   * Default constructor.
   *
   * @param inflater Handle fxml and resources
   */
  @Inject
  public ModuleAbstractUnitUnitSemesterConflicts(final Inflater inflater, final Router router) {
    this.router = router;

    inflater.inflate("components/reports/ModuleAbstractUnitUnitSemesterConflicts",
        this, this, "reports", "Column");
  }

  @FXML
  public void initialize() {
    tableViewModules.itemsProperty().bind(modules);
    tableViewModules.setOnMouseClicked(
        DetailViewHelper.getModuleMouseHandler(tableViewModules, router));

    initSynchronizedTableViews();

    txtExplanation.wrappingWidthProperty().bind(tableViewModules.widthProperty().subtract(25.0));
  }

  private void initSynchronizedTableViews() {
    tableViewAbstractUnit.itemsProperty().bind(
        new ConflictListBinding(tableViewModules.getSelectionModel().selectedItemProperty(),
            moduleAbstractUnitUnitSemesterConflictsMap));
    tableViewAbstractUnit.setOnMouseClicked(
        event -> handleTableColumnUnitClicked(event, tableViewAbstractUnit));

    tableViewExplicitUnit.itemsProperty().bind(
        new ConflictListBinding(tableViewModules.getSelectionModel().selectedItemProperty(),
            moduleAbstractUnitUnitSemesterConflictsMap));
    tableViewExplicitUnit.setOnMouseClicked(
        event -> handleTableColumnUnitClicked(event, tableViewExplicitUnit));

    tableViewAbstractUnit.getSelectionModel().selectedItemProperty()
        .addListener((observable, oldValue, newValue) ->
            tableViewSelectionSync(tableViewExplicitUnit, newValue));

    tableViewExplicitUnit.getSelectionModel().selectedItemProperty()
        .addListener((observable, oldValue, newValue) ->
            tableViewSelectionSync(tableViewAbstractUnit, newValue));

    tableViewAbstractUnit.itemsProperty().addListener((observable, oldValue, newValue) ->
        tableViewScrollSync(tableViewAbstractUnit, tableViewExplicitUnit));

    tableViewExplicitUnit.itemsProperty().addListener((observable, oldValue, newValue) ->
        tableViewScrollSync(tableViewAbstractUnit, tableViewExplicitUnit));

    initTableColumnWidth();
  }

  private void tableViewSelectionSync(final TableView<Conflict> tableView,
                                      final Conflict selectedItem) {
    if (!manualTableViewSelection) {
      manualTableViewSelection = true;
      tableView.getSelectionModel().select(selectedItem);
      return;
    }
    manualTableViewSelection = false;
  }

  private void tableViewScrollSync(final TableView<Conflict> tableView1,
                                   final TableView<Conflict> tableView2) {
    final ScrollBar scrollBar1 = getVerticalScrollbar(tableView1);
    final ScrollBar scrollBar2 = getVerticalScrollbar(tableView2);
    if (scrollBar1 != null && scrollBar2 != null) {
      scrollBar1.valueProperty().bindBidirectional(scrollBar2.valueProperty());
      scrollBar2.valueProperty().bindBidirectional(scrollBar1.valueProperty());
    }
  }

  private ScrollBar getVerticalScrollbar(final TableView<?> tableView) {
    for (Node node : tableView.lookupAll(".scroll-bar")) {
      if (node instanceof ScrollBar) {
        final ScrollBar scrollBar = (ScrollBar) node;
        if (scrollBar.getOrientation().equals(Orientation.VERTICAL)) {
          return scrollBar;
        }
      }
    }
    return null;
  }

  private void initTableColumnWidth() {
    tableViewAbstractUnit.prefWidthProperty().bind(tableViewModules.widthProperty().divide(2));
    tableViewExplicitUnit.prefWidthProperty().bind(tableViewModules.widthProperty().divide(2));

    tableViewAbstractUnit.widthProperty().addListener((observable, oldValue, newValue) -> {
      tableColumnAbstractUnitKey.setPrefWidth(
          tableViewAbstractUnit.widthProperty().multiply(0.15).get());
      tableColumnAbstractUnitTitle.setPrefWidth(
          tableViewAbstractUnit.widthProperty().multiply(0.6).get());
      tableColumnAbstractUnitSemesters.setPrefWidth(
          tableViewAbstractUnit.widthProperty().multiply(0.2).get());
    });

    tableViewExplicitUnit.widthProperty().addListener((observable, oldValue, newValue) -> {
      tableColumnExplicitUnitKey.setPrefWidth(
          tableViewExplicitUnit.widthProperty().multiply(0.15).get());
      tableColumnExplicitUnitTitle.setPrefWidth(
          tableViewExplicitUnit.widthProperty().multiply(0.6).get());
      tableColumnExplicitUnitSemesters.setPrefWidth(
          tableViewExplicitUnit.widthProperty().multiply(0.2).get());
    });
  }

  @SuppressWarnings("unused")
  private void handleTableColumnUnitClicked(final MouseEvent mouseEvent,
                                            final TableView<Conflict> tableView) {
    if (mouseEvent.getClickCount() < 2) {
      return;
    }
    //
    final TableView.TableViewSelectionModel<Conflict> selectionModel
        = tableView.getSelectionModel();

    final Conflict tableEntry = selectionModel.getSelectedItem();

    if (tableEntry == null) {
      return;
    }
    //
    final TableColumn column = selectionModel.getSelectedCells().get(0).getTableColumn();

    if (column.equals(tableColumnAbstractUnitKey)
        || column.equals(tableColumnAbstractUnitSemesters)
        || column.equals(tableColumnAbstractUnitTitle)) {
      router.transitionTo(RouteNames.ABSTRACT_UNIT_DETAIL_VIEW, tableEntry.getAbstractUnit());
    } else if (column.equals(tableColumnExplicitUnitKey)
        || column.equals(tableColumnExplicitUnitSemesters)
        || column.equals(tableColumnExplicitUnitTitle)) {
      router.transitionTo(RouteNames.UNIT_DETAIL_VIEW, tableEntry.getUnit());
    }
  }

  public void setData(final Map<Module, List<Conflict>> moduleAbstractUnitUnitSemesterConflicts) {
    this.moduleAbstractUnitUnitSemesterConflictsMap.putAll(moduleAbstractUnitUnitSemesterConflicts);
    this.modules.setAll(moduleAbstractUnitUnitSemesterConflicts.keySet());
  }

  public static final class Conflict {
    private final Set<Integer> semesters;

    private final AbstractUnit abstractUnit;
    private final Unit unit;

    /**
     * An object to obtain three values of the same type to use within a table view.
     */
    public Conflict(final AbstractUnit abstractUnit, final Unit unit,
                    final Set<Integer> abstractUnitSemesters) {
      this.abstractUnit = abstractUnit;
      this.unit = unit;
      this.semesters = abstractUnitSemesters;
    }

    public AbstractUnit getAbstractUnit() {
      return abstractUnit;
    }

    public Unit getUnit() {
      return unit;
    }

    @SuppressWarnings("unused")
    public String getAbstractUnitTitle() {
      return this.abstractUnit.getTitle();
    }

    @SuppressWarnings("unused")
    public String getAbstractUnitKey() {
      return this.abstractUnit.getKey();
    }

    @SuppressWarnings("unused")
    public String getAbstractUnitSemesters() {
      return semesters.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    @SuppressWarnings("unused")
    public String getUnitSemesters() {
      return unit.getSemesters().stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    @SuppressWarnings("unused")
    public String getUnitKey() {
      return unit.getKey();
    }

    @SuppressWarnings("unused")
    public String getUnitTitle() {
      return this.unit.getTitle();
    }
  }

  private static class ConflictListBinding extends ListBinding<Conflict> {
    private final ReadOnlyObjectProperty<Module> property;
    private final ObservableMap<Module, List<Conflict>> map;

    ConflictListBinding(final ReadOnlyObjectProperty<Module> property,
                        final ObservableMap<Module, List<Conflict>> map) {
      this.property = property;
      this.map = map;
      bind(property);
    }

    @Override
    protected ObservableList<Conflict> computeValue() {
      final Module module = property.get();
      return FXCollections.observableList(map.getOrDefault(module, Collections.emptyList()));
    }
  }
}
