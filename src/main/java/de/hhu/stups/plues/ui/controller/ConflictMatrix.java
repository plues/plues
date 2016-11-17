package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.keys.MajorMinorKey;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.batchgeneration.BatchFeasibilityTask;
import de.hhu.stups.plues.ui.batchgeneration.CollectFeasibilityTasksTask;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ConflictMatrix extends GridPane implements Initializable {

  private static final String VERTICAL = "vertical";

  private final Delayed<SolverService> delayedSolverService;
  private final ExecutorService executor;

  private ObservableMap<MajorMinorKey, Boolean> courseCombinationResults;
  private final BooleanProperty solverProperty;
  private final BooleanProperty feasibilityCheckRunning;
  private final List<Course> courses;
  private final List<Course> combinableMajorCourses;
  private final List<Course> combinableMinorCourses;
  private final List<Course> standaloneCourses;

  private final Set<SolverTask<Boolean>> checkFeasibilityTasks = new HashSet<>();
  private final Set<String> impossibleCourses;
  private Task<Set<SolverTask<Boolean>>> prepareFeasibilityCheck;
  private BatchFeasibilityTask executeFeasibilityCheck;
  private ResourceBundle resources;
  private Store store;

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
                        final UiDataService uiDataService,
                        final ExecutorService executorService) {
    this.delayedSolverService = delayedSolverService;
    this.executor = executorService;
    solverProperty = new SimpleBooleanProperty(false);
    feasibilityCheckRunning = new SimpleBooleanProperty(false);
    courses = new ArrayList<>();
    combinableMajorCourses = new ArrayList<>();
    combinableMinorCourses = new ArrayList<>();
    standaloneCourses = new ArrayList<>();
    impossibleCourses = new HashSet<>();

    delayedStore.whenAvailable(localStore -> {
      store = localStore;
      courses.addAll(store.getCourses());
      combinableMajorCourses.addAll(courses.stream()
          .filter(c -> c.isMajor() && c.isCombinable()).collect(Collectors.toList()));
      combinableMinorCourses.addAll(courses.stream()
          .filter(c -> c.isMinor() && c.isCombinable()).collect(Collectors.toList()));
      standaloneCourses.addAll(courses.stream()
          .filter(c -> !c.isCombinable()).collect(Collectors.toList()));
      setInitialGridPaneVisibility();
    });


    uiDataService.impossibleCoursesProperty().addListener(
        (SetChangeListener<? super String>) change -> {
          impossibleCourses.addAll(change.getSet());
          highlightImpossibleCourses();
      });

    delayedSolverService.whenAvailable(solverService -> {
      courseCombinationResults = solverService.getCourseCombinationResults();
      courseCombinationResults.addListener(getMapChangeListener());
      solverProperty.set(true);
    });

    inflater.inflate("ConflictMatrix", this, this, "conflictMatrix");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    this.resources = resources;
    final List<Node> components = Arrays.asList(
        gridPaneCombinable, scrollPaneCombinable, scrollPaneStandalone, gridPaneStandalone,
        boxLegend, lbCombinableCourses, lbStandaloneCourses, lbHeader, btCheckAll,
        btCancelCheckAll);
    components.forEach(c -> c.visibleProperty().bind(solverProperty));

    btCheckAll.disableProperty().bind(feasibilityCheckRunning.or(solverProperty.not()));
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
   * Initialize and set the visibility of the grid panes according to the current data.
   */
  private void setInitialGridPaneVisibility() {
    if (combinableMajorCourses.isEmpty() || combinableMinorCourses.isEmpty()) {
      lbCombinableCourses.visibleProperty().bind(new SimpleBooleanProperty(false));
      scrollPaneCombinable.visibleProperty().bind(new SimpleBooleanProperty(false));
      setColumnIndex(scrollPaneStandalone, 0);
      setColumnIndex(lbStandaloneCourses, 0);
      setColumnSpan(lbStandaloneCourses, 2);
      setColumnSpan(scrollPaneStandalone, 2);
      initializeGridPaneStandalone();
    } else if (standaloneCourses.isEmpty()) {
      lbStandaloneCourses.visibleProperty().bind(new SimpleBooleanProperty(false));
      scrollPaneStandalone.visibleProperty().bind(new SimpleBooleanProperty(false));
      setColumnSpan(scrollPaneCombinable, 2);
      setColumnSpan(lbCombinableCourses, 2);
      initializeGridPaneCombinable();
    } else {
      initializeGridPaneStandalone();
      initializeGridPaneCombinable();
    }
  }

  /**
   * Highlight the impossible courses, i.e. courses that are statically known to be infeasible.
   */
  private void highlightImpossibleCourses() {
    final List<String> majorCourseNames = combinableMajorCourses.stream()
        .map(Course::getName).collect(Collectors.toList());
    final List<String> minorCourseNames = combinableMinorCourses.stream()
        .map(Course::getName).collect(Collectors.toList());
    final List<String> standaloneCourseNames = standaloneCourses.stream()
        .map(Course::getName).collect(Collectors.toList());
    impossibleCourses.forEach(course -> {
      if (majorCourseNames.contains(course)) {
        final int col = majorCourseNames.indexOf(course) + 1;
        IntStream.range(0, combinableMinorCourses.size())
            .forEach(row -> gridPaneCombinable.add(
                getInfeasibleGridCellPane(majorCourseNames.get(col - 1)), col, row + 1));
      }
      if (minorCourseNames.contains(course)) {
        final int row = minorCourseNames.indexOf(course) + 1;
        IntStream.range(0, combinableMajorCourses.size())
            .forEach(col -> gridPaneCombinable.add(
                getInfeasibleGridCellPane(minorCourseNames.get(row - 1)), col + 1, row));
      }
      if (standaloneCourseNames.contains(course)) {
        final int col = standaloneCourseNames.indexOf(course);
        gridPaneStandalone.add(getInfeasibleGridCellPane(
            standaloneCourses.get(col).getName()), col, 1);
      }
    });
  }

  /**
   * Initialize the grid pane for the combinable courses, i.e. fill the first column/row with the
   * major/minor course names and set the default cells.
   */
  private void initializeGridPaneCombinable() {
    IntStream.range(0, combinableMajorCourses.size())
        .forEach(index -> gridPaneCombinable.add(
            getDefaultGridCellPane(
                combinableMajorCourses.get(index).getName(), VERTICAL), index + 1, 0));
    IntStream.range(0, combinableMinorCourses.size())
        .forEach(index -> gridPaneCombinable.add(
            getDefaultGridCellPane(combinableMinorCourses.get(index).getName()), 0, index + 1));
    IntStream.range(0, combinableMinorCourses.size())
        .forEach(row -> IntStream.range(0, combinableMajorCourses.size())
            .forEach(col -> {
              if (combinableMajorCourses.get(col).getShortName()
                  .equals(combinableMinorCourses.get(row).getShortName())) {
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
            getDefaultGridCellPane(standaloneCourses.get(index).getName(), VERTICAL), index, 0));
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
        delayedSolverService.get(), combinableMajorCourses,
        combinableMinorCourses, standaloneCourses, courseCombinationResults, impossibleCourses);

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
   * Call {@link ConflictMatrix#getDefaultGridCellPane(String, String) getDefaultGridCellPane} with
   * the default horizontal orientation.
   */
  private Pane getDefaultGridCellPane(final String courseName) {
    return getDefaultGridCellPane(courseName, "");
  }

  /**
   * Create a default grid pane cell.
   *
   * @param courseName  The course's name to display or empty for default empty cells.
   * @param orientation The label's orientation, i.e. "vertical" or anything else for horizontal.
   * @return Return a pane.
   */
  private Pane getDefaultGridCellPane(final String courseName, final String orientation) {
    final Pane pane = new Pane();
    pane.setId("conflictMatrixCellDefault");

    final Label label;
    if (!courseName.isEmpty()) {
      label = new Label("  " + courseName + "  ");
      if (VERTICAL.equals(orientation)) {
        label.setRotate(270.0);
        label.setTranslateY(100.0);
        label.setTranslateX(-70.0);
        label.setPrefWidth(200.0);
      } else {
        pane.setPrefHeight(25.0);
      }
      final Tooltip tooltip = new Tooltip(store.getCourseByKey(courseName).getFullName());
      label.setTooltip(tooltip);
    } else {
      label = new Label();
      pane.setPrefHeight(25.0);
    }
    pane.getChildren().add(new Group(label));

    return pane;
  }

  /**
   * Create a grid pane cell for impossible combinations where major and minor courses are "equal".
   *
   * @return Return a pane.
   */
  private Pane getImpossibleGridCellPane() {
    final Pane pane = new Pane();

    pane.getChildren().add(new Circle(5, 5, 2));
    pane.getChildren().add(new Circle(10, 5, 2));
    pane.getChildren().add(new Circle(5, 10, 2));
    pane.getChildren().add(new Circle(10, 10, 2));

    pane.setId("conflictMatrixCellImpossible");
    pane.setPrefHeight(25.0);

    final Label label = new Label();
    label.prefWidthProperty().bind(pane.widthProperty());
    label.prefHeightProperty().bind(pane.heightProperty());
    final Tooltip tooltip = new Tooltip(resources.getString("impossibleCombination"));
    label.setTooltip(tooltip);
    pane.getChildren().add(label);

    return pane;
  }

  /**
   * Create a grid pane cell for statically known impossible combinations of courses.
   *
   * @return Return a pane.
   */
  private Pane getInfeasibleGridCellPane(final String courseName) {
    final Pane pane = new Pane();

    pane.getChildren().add(new Circle(5, 5, 2));
    pane.getChildren().add(new Circle(10, 5, 2));
    pane.getChildren().add(new Circle(5, 10, 2));

    pane.setId("conflictMatrixCellInfeasible");
    pane.setPrefHeight(25.0);

    final Label label = new Label();
    label.prefWidthProperty().bind(pane.widthProperty());
    label.prefHeightProperty().bind(pane.heightProperty());
    final Tooltip tooltip = new Tooltip(resources.getString("staticallyInfeasible1") + " "
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
  private Pane getActiveGridCellPane(final Boolean result, final String... courseNames) {
    final Pane pane = new Pane();

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
      final Label label = new Label();
      label.prefWidthProperty().bind(pane.widthProperty());
      label.prefHeightProperty().bind(pane.heightProperty());
      final Tooltip tooltip = new Tooltip(resources.getString("major") + " " + courseNames[0] + "\n"
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
   * @param majorName The major course name.
   * @param minorName The minor course name.
   * @param result    True if the combination is feasible otherwise false.
   */
  private void gridPaneCombinableAddElm(final String majorName, final String minorName,
                                        final Boolean result) {
    final int row = combinableMinorCourses.stream().map(Course::getName)
        .collect(Collectors.toList()).indexOf(minorName) + 1;
    final int col = combinableMajorCourses.stream().map(Course::getName)
        .collect(Collectors.toList()).indexOf(majorName) + 1;

    if (!impossibleCourses.contains(majorName) && !impossibleCourses.contains(minorName)) {
      Platform.runLater(() -> gridPaneCombinable.add(
          getActiveGridCellPane(result, majorName, minorName), col, row));
    }
  }

  /**
   * Add a result to the list of standalone courses for given key and result.
   *
   * @param majorName The major course's name.
   * @param result    True if the course is feasible otherwise false.
   */
  private void gridPaneStandaloneAddElm(final String majorName, final Boolean result) {
    final int col = standaloneCourses.stream().map(Course::getName)
        .collect(Collectors.toList()).indexOf(majorName);


    // In {@link de.hhu.stups.plues.ui.components.CheckCourseFeasibility} it is possible to check a
    // single subject's feasibility that is not a standalone course, therefore we check that col
    // does not equal -1 because we don't want to add those partial results to the conflict matrix.
    if (!impossibleCourses.contains(majorName) && col != -1) {
      Platform.runLater(() -> gridPaneStandalone.add(getActiveGridCellPane(result), col, 1));
    }
  }

  private MapChangeListener<MajorMinorKey, Boolean> getMapChangeListener() {
    return change -> {
      if (change.wasAdded()) {
        final MajorMinorKey key = change.getKey();
        if (key.hasMinor()) {
          gridPaneCombinableAddElm(key.getMajor(), key.getMinor(), change.getValueAdded());
        } else {
          gridPaneStandaloneAddElm(key.getMajor(), change.getValueAdded());
        }
      } else {
        // discard all if a session has been moved
        gridPaneCombinable.getChildren().clear();
        gridPaneStandalone.getChildren().clear();
        // Todo: initialize grid panes?
      }
    };
  }
}
