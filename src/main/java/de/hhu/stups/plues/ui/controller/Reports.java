package de.hhu.stups.plues.ui.controller;

import com.google.common.base.Joiner;
import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Unit;
import de.hhu.stups.plues.prob.ReportData;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.components.reports.ImpossibleAbstractUnitsInModule;
import de.hhu.stups.plues.ui.components.reports.ImpossibleCourses;
import de.hhu.stups.plues.ui.components.reports.IncompleteModules;
import de.hhu.stups.plues.ui.components.reports.MandatoryModules;
import de.hhu.stups.plues.ui.components.reports.QuasiMandatoryModuleAbstractUnits;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

class Reports extends VBox implements Initializable {

  private final List<AbstractUnit> abstractUnitsWithoutUnits;
  private final List<AbstractUnit> abstractUnits;
  private final Properties properties;
  private Store store;
  private int groupAmount;
  private int sessionAmount;
  private int courseAmount;
  private int unitAmount;

  @FXML
  @SuppressWarnings("unused")
  private Label lbCourseAmount;
  @FXML
  @SuppressWarnings("unused")
  private Label lbUnitAmount;
  @FXML
  @SuppressWarnings("unused")
  private Label lbImpossibleCoursesAmount;
  @FXML
  @SuppressWarnings("unused")
  private Label lbAbstractUnitAmount;
  @FXML
  @SuppressWarnings("unused")
  private Label lbGroupAmount;
  @FXML
  @SuppressWarnings("unused")
  private Label lbSessionAmount;
  @FXML
  @SuppressWarnings("unused")
  private Label lbModelVersion;
  @FXML
  @SuppressWarnings("unused")
  private TableView<AbstractUnit> tableViewAbstractUnits;
  @FXML
  @SuppressWarnings("unused")
  private TableView<TableRowTriple<String>> tableViewAbstractUnitsWithUnits;
  @FXML
  @SuppressWarnings("unused")
  private TableView<Unit> tableViewRedundantUnitGroups;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<TableRowPair<String>, String> tableColumnAbstractKey;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<TableRowPair<String>, String> tableColumnAbstractTitle;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<TableRowTriple<String>, String> tableColumnAbstractUnit;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<TableRowTriple<String>, String> tableColumnUnit;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<TableRowTriple<String>, String> tableColumnSemesters;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<TableRowTriple<String>, String> tableColumnUnitSemesters;

  @FXML
  @SuppressWarnings("unused")
  private TableColumn<TableRowPair<String>, String> tableColumnRedundantUnitKey;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<TableRowPair<String>, String> tableColumnRedundantUnit;

  @FXML
  @SuppressWarnings("unused")
  private IncompleteModules incompleteModules;
  @FXML
  @SuppressWarnings("unused")
  private ImpossibleAbstractUnitsInModule impossibleAbstractUnitsInModule;
  @FXML
  @SuppressWarnings("unused")
  private ImpossibleCourses impossibleCourses;
  @FXML
  @SuppressWarnings("unused")
  private MandatoryModules mandatoryModules;
  @FXML
  @SuppressWarnings("unused")
  private QuasiMandatoryModuleAbstractUnits quasiMandatoryModuleAbstractUnits;

  /**
   * Reports view to present several reports and information about the loaded data, statistics,
   * etc.
   */
  @Inject
  public Reports(final Inflater inflater, final Delayed<Store> delayedStore,
                 final Delayed<SolverService> delayedSolverService,
                 final ExecutorService executor,
                 final Properties properties) {
    abstractUnits = new ArrayList<>();
    abstractUnitsWithoutUnits = new ArrayList<>();

    this.properties = properties;

    delayedStore.whenAvailable(localStore -> {
      this.store = localStore;
      abstractUnits.addAll(store.getAbstractUnits());
      abstractUnitsWithoutUnits.addAll(store.getAbstractUnitsWithoutUnits());
      groupAmount = store.getGroups().size();
      sessionAmount = store.getSessions().size();
      courseAmount = store.getCourses().size();
      unitAmount = store.getUnits().size();
    });

    delayedSolverService.whenAvailable(solverService -> {
      final SolverTask<ReportData> reportDataTask = solverService.collectReportDataTask();
      reportDataTask.setOnSucceeded(event -> displayReportData(reportDataTask.getValue()));
      executor.submit(reportDataTask);
    });

    inflater.inflate("Reports", this, this, "reports");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    final String listStyle = "batchListView";
    tableViewAbstractUnits.setId(listStyle);
    tableViewAbstractUnitsWithUnits.setId(listStyle);

    final String first = "first";
    final String second = "second";
    final String third = "third";

    tableColumnAbstractKey.setCellValueFactory(new PropertyValueFactory<>("key"));
    tableColumnAbstractTitle.setCellValueFactory(new PropertyValueFactory<>("title"));

    tableColumnAbstractUnit.setCellValueFactory(new PropertyValueFactory<>(first));
    tableColumnUnit.setCellValueFactory(new PropertyValueFactory<>(second));
    tableColumnSemesters.setCellValueFactory(new PropertyValueFactory<>(third));

    lbCourseAmount.setText(String.valueOf(courseAmount));
    lbUnitAmount.setText(String.valueOf(unitAmount));
    lbAbstractUnitAmount.setText(String.valueOf(abstractUnits.size()));
    lbGroupAmount.setText(String.valueOf(groupAmount));
    lbSessionAmount.setText(String.valueOf(sessionAmount));
    lbModelVersion.setText(String.valueOf(properties.get("model_version")));
  }

  /**
   * Initialize the list and table views that receive their data from {@link ReportData}.
   *
   * @param reportData The {@link ReportData report data} object.
   */
  @SuppressWarnings("unused")
  private void displayReportData(final ReportData reportData) {
    incompleteModules.setData(reportData.getIncompleteModules());
    impossibleAbstractUnitsInModule.setData(reportData.getImpossibleAbstractUnitsInModule()
        .entrySet().stream().collect(Collectors.toMap(
          entry -> store.getModuleById(entry.getKey()),
          entry -> entry.getValue().stream().map(
              store::getAbstractUnitById).collect(Collectors.toSet()))));
    impossibleCourses.setData(reportData.getImpossibleCourses());
    mandatoryModules.setData(reportData.getMandatoryModules()
        .entrySet().stream().collect(Collectors.toMap(
          entry -> store.getCourseByKey(entry.getKey()),
          entry -> entry.getValue().stream().map(
              store::getModuleById).collect(Collectors.toSet()))));
    quasiMandatoryModuleAbstractUnits.setData(reportData.getQuasiMandatoryModuleAbstractUnits()
        .entrySet().stream().collect(Collectors.toMap(
          entry -> store.getModuleById(entry.getKey()),
          entry -> entry.getValue().stream().map(
              store::getAbstractUnitById).collect(Collectors.toSet()))));


    lbImpossibleCoursesAmount.setText(String.valueOf(reportData.getImpossibleCourses().size()));

    tableViewAbstractUnits.getItems().addAll(abstractUnitsWithoutUnits);

    // get abstract units with units that have no semesters in common
    for (AbstractUnit abstractUnit : abstractUnits) {
      abstractUnit.getUnits().stream()
          .filter(unit -> abstractUnit
              .getModuleAbstractUnitSemesters().stream()
              .filter(moduleAbstractUnitSemester ->
                  unit.getSemesters().contains(moduleAbstractUnitSemester.getSemester()))
              .collect(Collectors.toList()).isEmpty())
          .forEach(unit ->
              tableViewAbstractUnitsWithUnits.getItems()
                  .add(new TableRowTriple<>(
                      abstractUnit.getTitle(),
                      unit.getTitle(),
                      Joiner.on(",").join(unit.getSemesters()))));
    }

    final Map<Integer, Set<Pair<Integer>>> redundantUnitGroups =
        reportData.getRedundantUnitGroups();

    final List<Unit> redundantUnits = redundantUnitGroups.keySet().stream()
        .map(store::getUnitById)
        .collect(Collectors.toList());
    tableViewRedundantUnitGroups.getItems().addAll(redundantUnits);
  }

  public static final class TableRowPair<T> {
    private final T second;
    private final T first;

    /**
     * An object to obtain two values of the same type to use within a table view.
     */
    TableRowPair(final T first, final T second) {
      this.first = first;
      this.second = second;
    }

    @Override
    public boolean equals(final Object other) {
      if (this == other) {
        return true;
      }
      if (other == null || getClass() != other.getClass()) {
        return false;
      }
      final TableRowPair<?> pair = (TableRowPair<?>) other;
      return Objects.equals(second, pair.second)
          && Objects.equals(first, pair.first);
    }

    @Override
    public int hashCode() {
      return Objects.hash(second, first);
    }

    @SuppressWarnings("unused")
    public T getFirst() {
      return first;
    }

    @SuppressWarnings("unused")
    public T getSecond() {
      return second;
    }
  }

  public static final class TableRowTriple<T> {
    private final T first;
    private final T second;
    private final T third;

    /**
     * An object to obtain three values of the same type to use within a table view.
     */
    TableRowTriple(final T first, final T second, final T third) {
      this.first = first;
      this.second = second;
      this.third = third;
    }

    @Override
    public boolean equals(final Object other) {
      if (this == other) {
        return true;
      }
      if (other == null || getClass() != other.getClass()) {
        return false;
      }
      TableRowTriple<?> triple = (TableRowTriple<?>) other;
      return Objects.equals(first, triple.first)
          && Objects.equals(second, triple.second)
          && Objects.equals(third, triple.third);
    }

    @Override
    public int hashCode() {
      return Objects.hash(first, second, third);
    }

    @SuppressWarnings("unused")
    public T getFirst() {
      return this.first;
    }

    @SuppressWarnings("unused")
    public T getSecond() {
      return this.second;
    }

    @SuppressWarnings("unused")
    public T getThird() {
      return this.third;
    }

  }

}
