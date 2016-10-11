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
  private TableView<Pair<String>> tableViewImpossibleCourses;
  @FXML
  @SuppressWarnings("unused")
  private TableView<Pair<String>> tableViewAbstractUnits;
  @FXML
  @SuppressWarnings("unused")
  private TableView<Pair<String>> tableViewAbstractUnitsWithUnits;
  @FXML
  @SuppressWarnings("unused")
  private TableView<Pair<String>> tableViewRedundantUnitGroups;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Pair<String>, String> tableColumnCourseName;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Pair<String>, String> tableColumnCourseFullName;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Pair<String>, String> tableColumnAbstractKey;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Pair<String>, String> tableColumnAbstractTitle;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Pair<String>, String> tableColumnAbstractUnit;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Pair<String>, String> tableColumnUnit;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Pair<String>, String> tableColumnRedundantUnit;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Pair<String>, String> tableColumnRedundantGroupPair;
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

    delayedStore.whenAvailable(store -> {
      groups.addAll(store.getGroups());
      courses.addAll(store.getCourses());
      units.addAll(store.getUnits());
      mandatoryModules.addAll(store.getModules());
      abstractUnits.addAll(store.getAbstractUnits());
      groupAmount = groups.size();
      sessionAmount = store.getSessions().size();
      abstractUnitsWithoutUnits.addAll(abstractUnits.stream()
          .filter(abstractUnit -> abstractUnit.getUnits().isEmpty()).collect(Collectors.toList()));
    });

    delayedSolverService.whenAvailable(solverService -> {
      final SolverTask<ReportData> reportDataTask = solverService.collectReportDataTask();
      reportDataTask.setOnSucceeded(event -> displayReportData(reportDataTask.getValue()));
      executor.submit(reportDataTask);
    });

    inflater.inflate("Reports", this, this, "reports");
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
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

    tableColumnRedundantUnit.setCellValueFactory(new PropertyValueFactory<>(first));
    tableColumnRedundantGroupPair.setCellValueFactory(new PropertyValueFactory<>(second));

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
  private void displayReportData(ReportData reportData) {
    tableViewImpossibleCourses.getItems().addAll(reportData.getImpossibleCourses()
        .stream().map(courseName ->
            new Pair<>(courseName, getFullName(courseName))).collect(Collectors.toList()));
    lbImpossibleCoursesAmount.setText(String.valueOf(reportData.getImpossibleCourses().size()));

    tableViewAbstractUnits.getItems()
        .addAll(abstractUnitsWithoutUnits.stream()
            .map(unit -> new Pair<>(unit.getKey(), unit.getTitle()))
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
                  .add(new Pair<>(abstractUnit.getTitle(), unit.getTitle())));
    }

    quasiMandatoryModules.putAll(reportData.getQuasiMandatoryModuleAbstractUnits());
    listViewCourses.getItems()
        .addAll(courses.stream().map(Course::getName).collect(Collectors.toList()));
    listViewQuasiCourses.getItems()
        .addAll(courses.stream().map(Course::getName).collect(Collectors.toList()));
    listViewCourses.getSelectionModel().select(0);
    listViewQuasiCourses.getSelectionModel().select(0);

    Map<Integer, Set<Pair<Integer>>> redundantUnitGroups = reportData.getRedundantUnitGroups();

    Set<Unit> redundantUnits = units.stream()
        .filter(unit -> redundantUnitGroups.containsKey(unit.getId()))
        .collect(Collectors.toSet());

    redundantUnits.forEach(redundantUnit ->
        redundantUnitGroups.get(redundantUnit.getId())
            .forEach(groupPair ->
                tableViewRedundantUnitGroups.getItems().add(
                    new Pair<>(redundantUnit.getTitle(), getStringFromPair(groupPair)))));
  }

  /**
   * Combine the unit titles of both groups in the given pair to one string.
   *
   * @param groupPairIds The pair of group ids.
   * @return Return a string of the two group unit names.
   */
  private String getStringFromPair(Pair<Integer> groupPairIds) {
    List<Group> groupPair = groups.stream()
        .filter(group ->
            group.getId() == groupPairIds.getFirst() || group.getId() == groupPairIds.getSecond())
        .collect(Collectors.toList());
    return groupPair.get(0).getUnit().getTitle() + " - " + groupPair.get(1).getUnit().getTitle();
  }

  /**
   * Get the full name for a given course name.
   *
   * @param courseName The course's name.
   * @return Return the course's full name.
   */
  private String getFullName(String courseName) {
    return courses.stream()
        .filter(course -> course.getName().equals(courseName))
        .collect(Collectors.toList()).get(0).getFullName();
  }

  /**
   * Update {@link Reports#listViewQuasiMandatoryModules} to show the quasi-mandatory modules of the
   * currently selected course in {@link Reports#listViewQuasiCourses}.
   *
   * @param selectedCourseName The currently selected course name.
   */
  @SuppressWarnings("unused")
  private void showQuasiMandatoryModulesOfCourse(String selectedCourseName) {
    Course selectedCourse = courses.stream()
        .filter(course -> course.getName().equals(selectedCourseName))
        .collect(Collectors.toList()).get(0);
    Set<Integer> quasiMandatoryModuleIds = quasiMandatoryModules.get(selectedCourse.getId());
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
  private void showMandatoryModulesOfCourse(String selectedCourseName) {
    mandatoryModules.forEach(module -> {
      if (module.getCourses().stream()
          .map(Course::getName).collect(Collectors.toList())
          .contains(selectedCourseName)) {
        listViewMandatoryModules.getItems().add(module.getTitle());
      }
    });
  }
}