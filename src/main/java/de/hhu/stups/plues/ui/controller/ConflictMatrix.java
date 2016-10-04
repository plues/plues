package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.batchgeneration.BatchFeasibilityTask;
import de.hhu.stups.plues.ui.batchgeneration.CollectFeasibilityTasksTask;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ConflictMatrix extends GridPane implements Initializable {

  private final Delayed<SolverService> delayedSolverService;
  private final ExecutorService executor;
  private ObservableMap<String, Boolean> courseCombinationResults;

  private final BooleanProperty solverProperty;
  private final BooleanProperty feasibilityCheckRunning;
  private List<Course> courses;
  private List<Course> majorCourses;
  private List<Course> minorCourses;
  private List<Course> standaloneCourses;
  private Set<SolverTask<Boolean>> checkFeasibilityTasks = new HashSet<>();

  private Task<Set<SolverTask<Boolean>>> prepareFeasibilityCheck;
  private BatchFeasibilityTask executeFeasibilityCheck;
  private Set<String> impossibleCourses;
  private ResourceBundle resources;

  @FXML
  @SuppressWarnings("unused")
  private GridPane gridPaneCombinable;
  @FXML
  @SuppressWarnings("unused")
  private GridPane gridPaneStandalone;
  @FXML
  @SuppressWarnings("unused")
  private HBox boxLegend;
  @FXML
  @SuppressWarnings("unused")
  private ScrollPane scrollPaneCombinable;
  @FXML
  @SuppressWarnings("unused")
  private ScrollPane scrollPaneStandalone;
  @FXML
  @SuppressWarnings("unused")
  private Label lbCombinableCourses;
  @FXML
  @SuppressWarnings("unused")
  private Label lbStandaloneCourses;
  @FXML
  @SuppressWarnings("unused")
  private Label lbHeader;
  @FXML
  @SuppressWarnings("unused")
  private Label lbLegendSuccess;
  @FXML
  @SuppressWarnings("unused")
  private Label lbLegendFailure;
  @FXML
  @SuppressWarnings("unused")
  private Label lbLegendInfeasible;
  @FXML
  @SuppressWarnings("unused")
  private Label lbLegendImpossible;
  @FXML
  @SuppressWarnings("unused")
  private Button btCheckAll;
  @FXML
  @SuppressWarnings("unused")
  private Button btCancelCheckAll;
  @FXML
  @SuppressWarnings("unused")
  private Pane paneLegendSuccess;
  @FXML
  @SuppressWarnings("unused")
  private Pane paneLegendFailure;
  @FXML
  @SuppressWarnings("unused")
  private Pane paneLegendImpossible;
  @FXML
  @SuppressWarnings("unused")
  private Pane paneLegendInfeasible;

  /**
   * This view presents a matrix of all possible combinations of combinable major and minor courses
   * and if known their feasibility. Furthermore a list of all standalone courses and if known their
   * feasibility is displayed.
   *
   * @param inflater             The layout inflater.
   * @param delayedStore         The Solver's store.
   * @param delayedSolverService SolverService for usage of ProB solver
   * @param executorService      The executor service to execute tasks.
   */
  @Inject
  public ConflictMatrix(final Inflater inflater, final Delayed<Store> delayedStore,
                        final Delayed<SolverService> delayedSolverService,
                        final ExecutorService executorService) {
    this.delayedSolverService = delayedSolverService;
    this.executor = executorService;
    solverProperty = new SimpleBooleanProperty(false);
    feasibilityCheckRunning = new SimpleBooleanProperty(false);

    delayedStore.whenAvailable(store -> {
      courses = store.getCourses();
      majorCourses = courses.stream()
          .filter(c -> c.isMajor() && c.isCombinable()).collect(Collectors.toList());
      minorCourses = courses.stream()
          .filter(c -> c.isMinor() && c.isCombinable()).collect(Collectors.toList());
      standaloneCourses = courses.stream()
          .filter(c -> !c.isCombinable()).collect(Collectors.toList());
      initializeGridPaneCombinable();
      initializeGridPaneStandalone();
    });

    delayedSolverService.whenAvailable(solverService -> {

      final SolverTask<Set<String>> impossibleCoursesTask = solverService.impossibleCoursesTask();
      impossibleCoursesTask.setOnSucceeded(event -> {
        impossibleCourses = impossibleCoursesTask.getValue();
        highlightImpossibleCourses();
      });
      executor.submit(impossibleCoursesTask);

      courseCombinationResults = solverService.getCourseCombinationResults();
      courseCombinationResults.addListener(getMapChangeListener());
      solverProperty.set(true);
    });

    inflater.inflate("ConflictMatrix", this, this, "conflictMatrix");
  }

  private void highlightImpossibleCourses() {
    List<String> majorCourseStrings = majorCourses.stream()
        .map(Course::getName).collect(Collectors.toList());
    List<String> minorCourseStrings = minorCourses.stream()
        .map(Course::getName).collect(Collectors.toList());
    impossibleCourses.forEach(course -> {
      if (majorCourseStrings.contains(course)) {
        int col = majorCourseStrings.indexOf(course) + 1;
        IntStream.range(0, minorCourses.size())
            .forEach(row -> gridPaneCombinable.add(
                getInfeasibleGridCellPane(majorCourseStrings.get(col - 1)), col, row + 1));
      }
      if (minorCourseStrings.contains(course)) {
        int row = minorCourseStrings.indexOf(course) + 1;
        IntStream.range(0, majorCourses.size())
            .forEach(col -> gridPaneCombinable.add(
                getInfeasibleGridCellPane(minorCourseStrings.get(row - 1)), col + 1, row));
      }
    });
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    this.resources = resources;
    final List<Node> components = Arrays.asList(
        gridPaneCombinable, scrollPaneCombinable, scrollPaneStandalone, gridPaneStandalone,
        boxLegend, lbCombinableCourses, lbStandaloneCourses, lbHeader, btCheckAll,
        btCancelCheckAll);
    components.forEach(c -> c.visibleProperty().bind(solverProperty));

    btCheckAll.disableProperty().bind(feasibilityCheckRunning);
    btCancelCheckAll.disableProperty().bind(feasibilityCheckRunning.not());

    // draw small circles for color blind users
    paneLegendSuccess.getChildren().add(new Circle(5, 5, 2));

    paneLegendFailure.getChildren().add(new Circle(5, 5, 2));
    paneLegendFailure.getChildren().add(new Circle(10, 5, 2));

    paneLegendInfeasible.getChildren().add(new Circle(5, 5, 2));
    paneLegendInfeasible.getChildren().add(new Circle(10, 5, 2));
    paneLegendInfeasible.getChildren().add(new Circle(5, 10, 2));

    paneLegendImpossible.getChildren().add(new Circle(5, 5, 2));
    paneLegendImpossible.getChildren().add(new Circle(10, 5, 2));
    paneLegendImpossible.getChildren().add(new Circle(5, 10, 2));
    paneLegendImpossible.getChildren().add(new Circle(10, 10, 2));

    paneLegendSuccess.setId("conflictMatrixLegendSuccess");
    paneLegendFailure.setId("conflictMatrixLegendFailed");
    paneLegendImpossible.setId("conflictMatrixLegendImpossible");
    paneLegendInfeasible.setId("conflictMatrixLegendInfeasible");

    lbLegendSuccess.setTooltip(new Tooltip(lbLegendSuccess.getText()));
    lbLegendFailure.setTooltip(new Tooltip(lbLegendFailure.getText()));
    lbLegendInfeasible.setTooltip(new Tooltip(lbLegendInfeasible.getText()));
    lbLegendImpossible.setTooltip(new Tooltip(lbLegendImpossible.getText()));
  }

  /**
   * Initialize the grid pane for the combinable courses, i.e. fill the first column/row with the
   * major/minor course names and set the default cells.
   */
  private void initializeGridPaneCombinable() {
    IntStream.range(0, majorCourses.size())
        .forEach(index -> gridPaneCombinable.add(
            getDefaultGridCellPane(majorCourses.get(index).getName()), index + 1, 0));
    IntStream.range(0, minorCourses.size())
        .forEach(index -> gridPaneCombinable.add(
            getDefaultGridCellPane(minorCourses.get(index).getName()), 0, index + 1));
    IntStream.range(0, minorCourses.size())
        .forEach(row -> IntStream.range(0, majorCourses.size())
            .forEach(col -> {
              if (majorCourses.get(col).getShortName()
                  .equals(minorCourses.get(row).getShortName())) {
                gridPaneCombinable.add(getImpossibleGridCellPane(), col + 1, row + 1);
              } else {
                gridPaneCombinable.add(getDefaultGridCellPane(""), col + 1, row + 1);
              }
            }));
    gridPaneCombinable.add(getDefaultGridCellPane(""), 0, 0);
  }

  /**
   * Initialize the grid pane presenting the standalone courses.
   */
  private void initializeGridPaneStandalone() {
    IntStream.range(0, standaloneCourses.size())
        .forEach(index -> gridPaneStandalone.add(
            getDefaultGridCellPane(standaloneCourses.get(index).getName()), index, 0));
    IntStream.range(0, standaloneCourses.size())
        .forEach(index -> gridPaneStandalone.add(
            getDefaultGridCellPane(""), index, 1));
  }

  /**
   * Action of button {@link ConflictMatrix#btCheckAll} to check the feasibility of all
   * combinations.
   */
  @FXML
  @SuppressWarnings("unused")
  public void checkAllCombinations() {
    feasibilityCheckRunning.setValue(true);
    prepareFeasibilityCheck = new CollectFeasibilityTasksTask(
        delayedSolverService.get(), majorCourses, minorCourses, standaloneCourses);


    prepareFeasibilityCheck.setOnCancelled(event -> {
      feasibilityCheckRunning.setValue(false);
      checkFeasibilityTasks.clear();
    });

    prepareFeasibilityCheck.setOnFailed(event -> {
      feasibilityCheckRunning.setValue(false);
      checkFeasibilityTasks.clear();
    });

    prepareFeasibilityCheck.setOnSucceeded(event -> {
      checkFeasibilityTasks.addAll(prepareFeasibilityCheck.getValue());
      executeFeasibilityCheck.setTasks(checkFeasibilityTasks);
      executor.submit(executeFeasibilityCheck);
    });

    executeFeasibilityCheck = new BatchFeasibilityTask(executor, checkFeasibilityTasks);

    executeFeasibilityCheck.setOnCancelled(event -> {
      checkFeasibilityTasks.forEach(task -> {
        if (!task.isDone()) {
          task.cancel(true);
        }
      });
      feasibilityCheckRunning.setValue(false);
      checkFeasibilityTasks.clear();
    });

    executeFeasibilityCheck.setOnFailed(event -> {
      feasibilityCheckRunning.setValue(false);
      checkFeasibilityTasks.clear();
    });

    executeFeasibilityCheck.setOnSucceeded(event -> {
      feasibilityCheckRunning.setValue(false);
      checkFeasibilityTasks.clear();
    });

    executor.submit(prepareFeasibilityCheck);
  }

  /**
   * Action of button {@link ConflictMatrix#btCancelCheckAll} to cancel the batch feasibility
   * check.
   */
  @FXML
  @SuppressWarnings("unused")
  public void cancelCheckAll() {
    if (prepareFeasibilityCheck.isRunning()) {
      prepareFeasibilityCheck.cancel(true);
    }
    if (executeFeasibilityCheck.isRunning()) {
      executeFeasibilityCheck.cancel(true);
    }
    feasibilityCheckRunning.setValue(false);
    checkFeasibilityTasks.clear();
  }

  /**
   * Create a default grid pane cell.
   *
   * @param courseName The course's name to display or empty for default empty cells.
   * @return Return a pane.
   */
  private Pane getDefaultGridCellPane(String courseName) {
    Pane pane = new Pane();
    pane.setId("conflictMatrixCellDefault");
    pane.setPrefHeight(25.0);

    Label label;
    if (!courseName.isEmpty()) {
      label = new Label("  " + courseName + "  ");
      String fullName = courses.stream()
          .filter(c -> c.getName().equals(courseName))
          .collect(Collectors.toList()).get(0).getFullName();
      Tooltip tooltip = new Tooltip(fullName);
      label.setTooltip(tooltip);
    } else {
      label = new Label();
    }
    pane.getChildren().add(label);

    return pane;
  }

  /**
   * Create a grid pane cell for impossible combinations where major and minor courses are "equal".
   *
   * @return Return a pane.
   */
  private Pane getImpossibleGridCellPane() {
    Pane pane = new Pane();

    pane.getChildren().add(new Circle(5, 5, 2));
    pane.getChildren().add(new Circle(10, 5, 2));
    pane.getChildren().add(new Circle(5, 10, 2));
    pane.getChildren().add(new Circle(10, 10, 2));

    pane.setId("conflictMatrixCellImpossible");
    pane.setPrefHeight(25.0);

    Label label = new Label();
    label.prefWidthProperty().bind(pane.widthProperty());
    label.prefHeightProperty().bind(pane.heightProperty());
    Tooltip tooltip = new Tooltip(resources.getString("impossibleCombination"));
    label.setTooltip(tooltip);
    pane.getChildren().add(label);

    return pane;
  }

  /**
   * Create a grid pane cell for statically known impossible combinations of courses.
   *
   * @return Return a pane.
   */
  private Pane getInfeasibleGridCellPane(String courseName) {
    Pane pane = new Pane();

    pane.getChildren().add(new Circle(5, 5, 2));
    pane.getChildren().add(new Circle(10, 5, 2));
    pane.getChildren().add(new Circle(5, 10, 2));

    pane.setId("conflictMatrixCellInfeasible");
    pane.setPrefHeight(25.0);

    Label label = new Label();
    label.prefWidthProperty().bind(pane.widthProperty());
    label.prefHeightProperty().bind(pane.heightProperty());
    Tooltip tooltip = new Tooltip(resources.getString("staticallyInfeasible1") + " "
        + courseName + " " + resources.getString("staticallyInfeasible2"));
    label.setTooltip(tooltip);
    pane.getChildren().add(label);

    return pane;
  }

  /**
   * Create an active grid pane cell, i.e. the feasibility is known and the pane's background color
   * is set according to the result. Furthermore a tooltip with major and minor course name is
   * added.
   *
   * @param result      True if the combination of major and minor course is feasible.
   * @param courseNames An optional array of the major and minor course names.
   */
  private Pane getActiveGridCellPane(Boolean result, String... courseNames) {
    Pane pane = new Pane();

    final String paneId;
    pane.getChildren().add(new Circle(5, 5, 2));
    if (!result) {
      pane.getChildren().add(new Circle(10, 5, 2));
      paneId = "conflictMatrixCellFailed";
    } else {
      paneId = "conflictMatrixCellSuccess";
    }
    pane.setId(paneId);
    if (courseNames.length != 0) {
      Label label = new Label();
      label.prefWidthProperty().bind(pane.widthProperty());
      label.prefHeightProperty().bind(pane.heightProperty());
      Tooltip tooltip = new Tooltip(resources.getString("major") + " " + courseNames[0] + "\n"
          + resources.getString("minor") + " " + courseNames[1]);
      label.setTooltip(tooltip);
      pane.getChildren().add(label);
    }
    pane.setPrefHeight(25.0);

    return pane;
  }

  /**
   * Add a result to the conflict matrix of combinable courses for given key and result. Define the
   * column and row according to the major and minor name by searching their position in the
   * specific list of courses.
   *
   * @param key    The string of major and minor course name split by a semicolon.
   * @param result True if the combination is feasible otherwise false.
   */
  private void gridPaneCombinableAddElm(String key, Boolean result) {
    final String majorName = key.split(";")[0];
    final String minorName = key.split(";")[1];

    final int row = minorCourses.stream().map(Course::getName)
        .collect(Collectors.toList()).indexOf(minorName) + 1;
    final int col = majorCourses.stream().map(Course::getName)
        .collect(Collectors.toList()).indexOf(majorName) + 1;

    if (!impossibleCourses.contains(majorName) && !impossibleCourses.contains(minorName)) {
      Platform.runLater(() -> gridPaneCombinable.add(
          getActiveGridCellPane(result, majorName, minorName), col, row));
    }
  }

  /**
   * Add a result to the list of standalone courses for given key and result.
   *
   * @param key    The major course's name.
   * @param result True if the course is feasible otherwise false.
   */
  private void gridPaneStandaloneAddElm(String key, Boolean result) {
    final String majorName = key.split(";")[0];
    final int col = standaloneCourses.stream().map(Course::getName)
        .collect(Collectors.toList()).indexOf(majorName);

    Platform.runLater(() -> gridPaneStandalone.add(getActiveGridCellPane(result), col, 1));
  }

  private MapChangeListener<String, Boolean> getMapChangeListener() {
    return change -> {
      if (change.wasAdded()) {
        if (change.getKey().split(";").length == 2) {
          gridPaneCombinableAddElm(change.getKey(), change.getValueAdded());
        } else {
          gridPaneStandaloneAddElm(change.getKey(), change.getValueAdded());
        }
      } else {
        // discard all if a session has been moved
        gridPaneCombinable.getChildren().clear();
      }
    };
  }
}
