package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Group;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.Unit;
import de.hhu.stups.plues.prob.ReportData;
import de.hhu.stups.plues.prob.report.Pair;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

class Reports extends VBox implements Initializable {

  private final List<AbstractUnit> abstractUnitsWithoutUnits;
  private final List<Unit> units;
  private final List<AbstractUnit> abstractUnits;
  private final List<Course> courses;
  private final List<Module> mandatoryModules;
  private final Map<Integer, Set<Integer>> quasiMandatoryModules;
  private final List<Group> groups;
  private Store store;
  private int groupAmount;
  private int sessionAmount;

  @FXML
  @SuppressWarnings("unused")
  private Accordion paneAccordion;
  @FXML
  @SuppressWarnings("unused")
  private Pane paneHeader;
  @FXML
  @SuppressWarnings("unused")
  private TitledPane paneImpossibleCourses;
  @FXML
  @SuppressWarnings("unused")
  private TitledPane paneMandatoryModules;
  @FXML
  @SuppressWarnings("unused")
  private TitledPane paneAbstractUnits;
  @FXML
  @SuppressWarnings("unused")
  private TitledPane paneAbstractUnitsWithUnits;
  @FXML
  @SuppressWarnings("unused")
  private TitledPane paneQuasiMandatoryModules;
  @FXML
  @SuppressWarnings("unused")
  private TitledPane paneRedundantUnitGroups;
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
  private TableView<TableRowPair<String>> tableViewImpossibleCourses;
  @FXML
  @SuppressWarnings("unused")
  private TableView<TableRowPair<String>> tableViewAbstractUnits;
  @FXML
  @SuppressWarnings("unused")
  private TableView<TableRowPair<String>> tableViewAbstractUnitsWithUnits;
  @FXML
  @SuppressWarnings("unused")
  private TableView<TableRowTriple<String>> tableViewRedundantUnitGroups;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<TableRowPair<String>, String> tableColumnCourseName;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<TableRowPair<String>, String> tableColumnCourseFullName;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<TableRowPair<String>, String> tableColumnAbstractKey;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<TableRowPair<String>, String> tableColumnAbstractTitle;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<TableRowPair<String>, String> tableColumnAbstractUnit;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<TableRowPair<String>, String> tableColumnUnit;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<TableRowPair<String>, String> tableColumnRedundantGroup1;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<TableRowPair<String>, String> tableColumnRedundantGroup2;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<TableRowPair<String>, String> tableColumnRedundantUnit;
  @FXML
  @SuppressWarnings("unused")
  private ListView<String> listViewCourses;
  @FXML
  @SuppressWarnings("unused")
  private ListView<String> listViewMandatoryModules;
  @FXML
  @SuppressWarnings("unused")
  private ListView<String> listViewQuasiCourses;
  @FXML
  @SuppressWarnings("unused")
  private ListView<String> listViewQuasiMandatoryModules;

  /**
   * Reports view to present several reports and information about the loaded data, statistics,
   * etc.
   */
  @Inject
  public Reports(final Inflater inflater, final Delayed<Store> delayedStore,
                 final Delayed<SolverService> delayedSolverService,
                 final ExecutorService executor) {
    courses = new ArrayList<>();
    units = new ArrayList<>();
    abstractUnits = new ArrayList<>();
    abstractUnitsWithoutUnits = new ArrayList<>();
    mandatoryModules = new ArrayList<>();
    groups = new ArrayList<>();
    quasiMandatoryModules = new HashMap<>();

    delayedStore.whenAvailable(localStore -> {
      this.store = localStore;
      groups.addAll(store.getGroups());
      courses.addAll(store.getCourses());
      units.addAll(store.getUnits());
      mandatoryModules.addAll(store.getModules());
      abstractUnits.addAll(store.getAbstractUnits());
      abstractUnitsWithoutUnits.addAll(store.getAbstractUnitsWithoutUnits());
      groupAmount = groups.size();
      sessionAmount = store.getSessions().size();
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
    tableViewImpossibleCourses.setId(listStyle);
    tableViewAbstractUnits.setId(listStyle);
    tableViewAbstractUnitsWithUnits.setId(listStyle);
    listViewCourses.setId(listStyle);
    listViewMandatoryModules.setId(listStyle);
    listViewQuasiCourses.setId(listStyle);
    listViewQuasiMandatoryModules.setId(listStyle);

    final String first = "first";
    final String second = "second";
    tableColumnCourseName.setCellValueFactory(new PropertyValueFactory<>(first));
    tableColumnCourseFullName.setCellValueFactory(new PropertyValueFactory<>(second));

    tableColumnAbstractKey.setCellValueFactory(new PropertyValueFactory<>(first));
    tableColumnAbstractTitle.setCellValueFactory(new PropertyValueFactory<>(second));

    tableColumnAbstractUnit.setCellValueFactory(new PropertyValueFactory<>(first));
    tableColumnUnit.setCellValueFactory(new PropertyValueFactory<>(second));

    tableColumnRedundantGroup1.setCellValueFactory(new PropertyValueFactory<>(first));
    tableColumnRedundantGroup2.setCellValueFactory(new PropertyValueFactory<>(second));
    tableColumnRedundantUnit.setCellValueFactory(new PropertyValueFactory<>("third"));

    // add listener to update the (quasi-) mandatory list views according to the selected course
    listViewCourses.getSelectionModel().selectedItemProperty()
        .addListener((observable, oldValue, newValue) -> {
          listViewMandatoryModules.getItems().clear();
          showMandatoryModulesOfCourse(listViewCourses.getSelectionModel().getSelectedItem());
        });
    listViewQuasiCourses.getSelectionModel().selectedItemProperty()
        .addListener((observable, oldValue, newValue) -> {
          listViewQuasiMandatoryModules.getItems().clear();
          showQuasiMandatoryModulesOfCourse(
              listViewQuasiCourses.getSelectionModel().getSelectedItem());
        });

    lbCourseAmount.setText(String.valueOf(courses.size()));
    lbUnitAmount.setText(String.valueOf(units.size()));
    lbAbstractUnitAmount.setText(String.valueOf(abstractUnits.size()));
    lbGroupAmount.setText(String.valueOf(groupAmount));
    lbSessionAmount.setText(String.valueOf(sessionAmount));

    paneAccordion.setExpandedPane(paneImpossibleCourses);
  }

  /**
   * Initialize the list and table views that receive their data from {@link ReportData}.
   *
   * @param reportData The {@link ReportData report data} object.
   */
  @SuppressWarnings("unused")
  private void displayReportData(final ReportData reportData) {
    tableViewImpossibleCourses.getItems().addAll(reportData.getImpossibleCourses()
        .stream().map(courseName ->
            new TableRowPair<>(courseName, store.getCourseByKey(courseName).getFullName()))
        .collect(Collectors.toList()));
    lbImpossibleCoursesAmount.setText(String.valueOf(reportData.getImpossibleCourses().size()));

    tableViewAbstractUnits.getItems()
        .addAll(abstractUnitsWithoutUnits.stream()
            .map(unit -> new TableRowPair<>(unit.getKey(), unit.getTitle()))
            .collect(Collectors.toList()));

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
                  .add(new TableRowPair<>(abstractUnit.getTitle(), unit.getTitle())));
    }

    quasiMandatoryModules.putAll(reportData.getQuasiMandatoryModuleAbstractUnits());
    listViewCourses.getItems()
        .addAll(courses.stream().map(Course::getName).collect(Collectors.toList()));
    listViewQuasiCourses.getItems()
        .addAll(courses.stream().map(Course::getName).collect(Collectors.toList()));
    listViewCourses.getSelectionModel().select(0);
    listViewQuasiCourses.getSelectionModel().select(0);

    final Map<Integer, Set<Pair<Integer>>> redundantUnitGroups =
        reportData.getRedundantUnitGroups();

    final Set<Unit> redundantUnits = redundantUnitGroups.keySet().stream()
        .map(store::getUnitById).collect(Collectors.toSet());

    redundantUnits.forEach(redundantUnit ->
        redundantUnitGroups.get(redundantUnit.getId())
            .forEach(groupPair ->
                tableViewRedundantUnitGroups.getItems().add(
                    new TableRowTriple<>(
                        groupPair.getFirst().toString(),
                        groupPair.getSecond().toString(),
                        store.getGroupById(groupPair.getFirst()).getUnit().getTitle()))));
  }

  /**
   * Update {@link Reports#listViewQuasiMandatoryModules} to show the quasi-mandatory modules of the
   * currently selected course in {@link Reports#listViewQuasiCourses}.
   *
   * @param selectedCourseName The currently selected course name.
   */
  @SuppressWarnings("unused")
  private void showQuasiMandatoryModulesOfCourse(final String selectedCourseName) {
    final Course selectedCourse = courses.stream()
        .filter(course -> course.getName().equals(selectedCourseName))
        .findFirst().orElse(null);
    final Set<Integer> quasiMandatoryModuleIds =
        (selectedCourse != null) ? quasiMandatoryModules.get(selectedCourse.getId()) : null;

    if (quasiMandatoryModuleIds != null && !quasiMandatoryModuleIds.isEmpty()) {
      abstractUnits.forEach(abstractUnit -> {
        if (quasiMandatoryModuleIds.contains(abstractUnit.getId())) {
          listViewQuasiMandatoryModules.getItems().addAll(abstractUnit.getTitle());
        }
      });
    } else {
      listViewQuasiMandatoryModules.getItems().clear();
    }
  }

  /**
   * Update {@link Reports#listViewMandatoryModules} to show the mandatory modules of the currently
   * selected course in {@link Reports#listViewCourses}.
   *
   * @param selectedCourseName The currently selected course within {@link Reports#listViewCourses}.
   */
  @SuppressWarnings("unused")
  private void showMandatoryModulesOfCourse(final String selectedCourseName) {
    mandatoryModules.forEach(module -> {
      if (module.getCourses().stream()
          .map(Course::getName).collect(Collectors.toList())
          .contains(selectedCourseName)) {
        listViewMandatoryModules.getItems().add(module.getTitle());
      }
    });
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