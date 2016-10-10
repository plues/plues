package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.Unit;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
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
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

class Reports extends VBox implements Initializable {

  private final BooleanProperty solverProperty = new SimpleBooleanProperty(false);
  private final List<AbstractUnit> abstractUnitsWithoutUnits;
  private final List<Unit> units;
  private final List<AbstractUnit> abstractUnits;
  private final List<Course> courses;
  private final Set<String> impossibleCourses;
  private final List<Module> mandatoryModules;
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
  private TableView<TableViewTuple> tableViewImpossibleCourses;
  @FXML
  @SuppressWarnings("unused")
  private TableView<TableViewTuple> tableViewAbstractUnits;
  @FXML
  @SuppressWarnings("unused")
  private TableView<TableViewTuple> tableViewAbstractUnitsWithUnits;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<TableViewTuple, String> tableColumnCourseName;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<TableViewTuple, String> tableColumnCourseFullName;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<TableViewTuple, String> tableColumnAbstractKey;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<TableViewTuple, String> tableColumnAbstractTitle;
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
    impossibleCourses = new HashSet<>();
    courses = new ArrayList<>();
    units = new ArrayList<>();
    abstractUnits = new ArrayList<>();
    abstractUnitsWithoutUnits = new ArrayList<>();
    mandatoryModules = new ArrayList<>();

    delayedStore.whenAvailable(store -> {
      courses.addAll(store.getCourses());
      units.addAll(store.getUnits());
      mandatoryModules.addAll(store.getModules());
      abstractUnits.addAll(store.getAbstractUnits());
      groupAmount = store.getGroups().size();
      sessionAmount = store.getSessions().size();
      abstractUnitsWithoutUnits.addAll(abstractUnits.stream()
          .filter(abstractUnit -> abstractUnit.getUnits().isEmpty()).collect(Collectors.toList()));
    });

    delayedSolverService.whenAvailable(solverService -> {
      final SolverTask<Set<String>> impossibleCoursesTask = solverService.impossibleCoursesTask();
      impossibleCoursesTask.setOnSucceeded(event -> {
        impossibleCourses.addAll(impossibleCoursesTask.getValue());
        tableViewImpossibleCourses.getItems().addAll(
            impossibleCourses.stream()
                .map(course -> new TableViewTuple(course, getFullName(course)))
                .collect(Collectors.toList()));
        solverProperty.set(true);
        lbImpossibleCoursesAmount.setText(String.valueOf(impossibleCourses.size()));
      });
      executor.submit(impossibleCoursesTask);
    });

    inflater.inflate("Reports", this, this, "reports");
  }

  /**
   * Get the courses full name for a given course name.
   *
   * @param courseName The course's name.
   * @return Return the course's full name.
   */
  private String getFullName(String courseName) {
    return courses.stream()
        .filter(course -> course.getName().equals(courseName))
        .collect(Collectors.toList()).get(0).getFullName();
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    paneHeader.visibleProperty().bind(solverProperty);
    paneImpossibleCourses.visibleProperty().bind(solverProperty);
    paneAbstractUnits.visibleProperty().bind(solverProperty);
    paneAbstractUnitsWithUnits.visibleProperty().bind(solverProperty);
    paneMandatoryModules.visibleProperty().bind(solverProperty);
    paneQuasiMandatoryModules.visibleProperty().bind(solverProperty);

    final String listStyle = "batchListView";
    tableViewImpossibleCourses.setId(listStyle);
    tableViewAbstractUnits.setId(listStyle);
    tableViewAbstractUnitsWithUnits.setId(listStyle);
    listViewCourses.setId(listStyle);
    listViewMandatoryModules.setId(listStyle);
    listViewQuasiCourses.setId(listStyle);
    listViewQuasiMandatoryModules.setId(listStyle);

    tableColumnCourseName.setCellValueFactory(new PropertyValueFactory<>("firstCol"));
    tableColumnCourseFullName.setCellValueFactory(new PropertyValueFactory<>("secondCol"));

    tableColumnAbstractKey.setCellValueFactory(new PropertyValueFactory<>("firstCol"));
    tableColumnAbstractTitle.setCellValueFactory(new PropertyValueFactory<>("secondCol"));

    listViewCourses.getSelectionModel().selectedItemProperty()
        .addListener((observable, oldValue, newValue) -> {
          listViewMandatoryModules.getItems().clear();
          showMandatoryModulesOfCourse(listViewCourses.getSelectionModel().getSelectedItem());
        });

    // Todo: add listener to show quasi-mandatory modules

    // Todo: initialize table view to display abstract units with units that have no semester in common

    tableViewAbstractUnits.getItems()
        .addAll(abstractUnitsWithoutUnits.stream()
            .map(unit -> new TableViewTuple(unit.getKey(), unit.getTitle()))
            .collect(Collectors.toList()));
    listViewCourses.getItems()
        .addAll(courses.stream().map(Course::getName).collect(Collectors.toList()));
    listViewQuasiCourses.getItems()
        .addAll(courses.stream().map(Course::getName).collect(Collectors.toList()));

    listViewCourses.getSelectionModel().select(0);
    listViewQuasiCourses.getSelectionModel().select(0);

    lbCourseAmount.setText(String.valueOf(courses.size()));
    lbUnitAmount.setText(String.valueOf(units.size()));
    lbAbstractUnitAmount.setText(String.valueOf(abstractUnits.size()));
    lbGroupAmount.setText(String.valueOf(groupAmount));
    lbSessionAmount.setText(String.valueOf(sessionAmount));

    paneAccordion.setExpandedPane(paneImpossibleCourses);
  }

  /**
   * Update {@link Reports#listViewMandatoryModules} to show the mandatory modules of the currently
   * selected course in {@link Reports#listViewCourses}.
   *
   * @param selectedCourse The currently selected course within {@link Reports#listViewCourses}.
   */
  @SuppressWarnings("unused")
  private void showMandatoryModulesOfCourse(String selectedCourse) {
    mandatoryModules.forEach(module -> {
      if (module.getCourses().stream()
          .map(Course::getName).collect(Collectors.toList())
          .contains(selectedCourse)) {
        listViewMandatoryModules.getItems().add(module.getTitle());
      }
    });
  }

  /**
   * Wrap two strings to a tuple to use within the table views.
   */
  public static class TableViewTuple {
    private SimpleStringProperty firstCol;
    private SimpleStringProperty secondCol;

    TableViewTuple(String first, String second) {
      this.firstCol = new SimpleStringProperty(first);
      this.secondCol = new SimpleStringProperty(second);
    }

    @SuppressWarnings("unused")
    public String getFirstCol() {
      return this.firstCol.get();
    }

    @SuppressWarnings("unused")
    public String getSecondCol() {
      return this.secondCol.get();
    }
  }
}