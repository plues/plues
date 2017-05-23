package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.keys.CourseSelection;
import de.hhu.stups.plues.prob.ResultState;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.batchgeneration.BatchFeasibilityTask;
import de.hhu.stups.plues.ui.batchgeneration.CollectFeasibilityTasksTask;
import de.hhu.stups.plues.ui.components.conflictmatrix.CourseGridCell;
import de.hhu.stups.plues.ui.components.conflictmatrix.ResultGridCell;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.SetChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ConflictMatrix extends GridPane implements Initializable {

  private static final String VERTICAL = "vertical";

  private final Delayed<SolverService> delayedSolverService;
  private final ExecutorService executor;
  private final Router router;
  private final BooleanProperty solverProperty;
  private final BooleanProperty feasibilityCheckRunning;
  private final LongProperty impossibleCoursesAmount;
  private final MapProperty<CourseSelection, ResultState> results;
  private final Map<CourseSelection, ResultGridCell> cellMap;
  private final List<Course> courses;
  private final List<Course> combinableMajorCourses;
  private final List<Course> combinableMinorCourses;
  private final List<Course> standaloneCourses;
  private final Set<SolverTask<Boolean>> checkFeasibilityTasks = new HashSet<>();

  private final Set<Course> impossibleCourses;
  private ResourceBundle resources;
  private Task<Set<SolverTask<Boolean>>> prepareFeasibilityCheck;

  private BatchFeasibilityTask executeFeasibilityCheck;
  @FXML
  @SuppressWarnings("unused")
  private Accordion accordionConflictMatrices;
  @FXML
  @SuppressWarnings("unused")
  private TitledPane titledPaneCombinableCourses;
  @FXML
  @SuppressWarnings("unused")
  private TitledPane titledPaneStandaloneCourses;
  @FXML
  @SuppressWarnings("unused")
  private TitledPane titledPaneSingleCourses;
  @FXML
  @SuppressWarnings("unused")
  private GridPane gridPaneCombinable;
  @FXML
  @SuppressWarnings("unused")
  private GridPane gridPaneStandalone;
  @FXML
  @SuppressWarnings("unused")
  private GridPane gridPaneSingleCourses;
  @FXML
  @SuppressWarnings("unused")
  private Label lbFeasibleCourseAmount;
  @FXML
  @SuppressWarnings("unused")
  private Label lbInfeasibleCourseAmount;
  @FXML
  @SuppressWarnings("unused")
  private Label lblImpossibleCoursesAmount;
  @FXML
  @SuppressWarnings("unused")
  private Label lbTimeoutCourseAmount;
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
  private Pane paneLegendTimeout;
  @FXML
  @SuppressWarnings("unused")
  private Pane paneLegendImpossible;
  @FXML
  @SuppressWarnings("unused")
  private Pane paneLegendInfeasible;

  /**
   * This view presents a matrix of all possible combinations of combinable major and minor courses
   * and if known their feasibility. Furthermore a list of all standalone courses as well as a list
   * of all single courses and if known their feasibility is displayed. Each cell is represented by
   * a {@link ResultGridCell}.
   */
  @Inject
  public ConflictMatrix(final Inflater inflater,
                        final Delayed<Store> delayedStore,
                        final Delayed<SolverService> delayedSolverService,
                        final UiDataService uiDataService,
                        final ExecutorService executorService,
                        final Router router) {
    this.delayedSolverService = delayedSolverService;
    this.executor = executorService;
    this.router = router;

    solverProperty = new SimpleBooleanProperty(false);
    results = new SimpleMapProperty<>(FXCollections.emptyObservableMap());

    feasibilityCheckRunning = new SimpleBooleanProperty(false);
    courses = new ArrayList<>();
    combinableMajorCourses = new ArrayList<>();
    combinableMinorCourses = new ArrayList<>();
    standaloneCourses = new ArrayList<>();
    impossibleCourses = new HashSet<>();

    impossibleCoursesAmount = new SimpleLongProperty(0L);

    cellMap = new HashMap<>();

    delayedStore.whenAvailable(store -> {
      courses.addAll(store.getCourses().stream()
          .sorted(Comparator.comparing(Course::getShortName)).collect(Collectors.toList()));
      standaloneCourses.addAll(courses.stream()
          .filter(c -> !c.isCombinable()).collect(Collectors.toList()));

      combinableMajorCourses.addAll(store.getMajors().stream()
          .filter(Course::isCombinable).collect(Collectors.toList()));
      combinableMinorCourses.addAll(store.getMinors().stream()
          .filter(Course::isCombinable).collect(Collectors.toList()));
      setInitialGridPaneVisibility();
    });

    uiDataService.impossibleCoursesProperty().addListener(
        (SetChangeListener<? super Course>) change -> {
          impossibleCourses.addAll(change.getSet());
          highlightImpossibleCourses();
        });

    delayedSolverService.whenAvailable(solverService -> {
      results.bind(solverService.courseSelectionResultsProperty());
      results.addListener(getCourseResultChangeListener());
      solverProperty.set(true);
    });

    inflater.inflate("ConflictMatrix", this, this, "conflictMatrix");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    this.resources = resources;
    initializeStats();

    btCheckAll.disableProperty().bind(feasibilityCheckRunning.or(solverProperty.not()));
    btCancelCheckAll.disableProperty().bind(feasibilityCheckRunning.not());

    // draw small circles to distinguish between each cell independent from its color
    paneLegendSuccess.getChildren().add(new Circle(5, 5, 2));
    paneLegendFailure.getChildren().addAll(new Circle(5, 5, 2), new Circle(10, 5, 2));
    paneLegendTimeout.getChildren().addAll(new Circle(5, 5, 2), new Circle(10, 5, 2),
        new Circle(5, 10, 2));
    paneLegendInfeasible.getChildren().addAll(new Circle(5, 5, 2), new Circle(10, 5, 2),
        new Circle(5, 10, 2), new Circle(10, 10, 2));
    paneLegendImpossible.getChildren().addAll(new Circle(5, 5, 2), new Circle(10, 5, 2),
        new Circle(5, 10, 2), new Circle(10, 10, 2), new Circle(5, 15, 2));

  }

  private void initializeStats() {
    // for counting we only consider results for course combinations and standalone courses
    // single courses are ignored.
    lbTimeoutCourseAmount.textProperty().bind(
        Bindings.createStringBinding(() -> String.valueOf(
            results.entrySet().parallelStream()
                .filter(entry -> entry.getKey().isCurriculum()
                    && entry.getValue().timedOut()).count()), results));

    lbFeasibleCourseAmount.textProperty().bind(
        Bindings.createStringBinding(() -> String.valueOf(
            results.entrySet().parallelStream()
                .filter(entry -> entry.getKey().isCurriculum()
                    && entry.getValue().succeeded()).count()), results));

    lbInfeasibleCourseAmount.textProperty().bind(
        Bindings.createStringBinding(() -> String.valueOf(
            results.entrySet().parallelStream()
                .filter(entry -> {
                  CourseSelection cs = entry.getKey();
                  ResultState result = entry.getValue();

                  return cs.isCurriculum()
                      && cs.getCourses().stream().noneMatch(impossibleCourses::contains)
                      && result.failed();
                }).count()), results));

    lblImpossibleCoursesAmount.textProperty().bind(Bindings.convert(impossibleCoursesAmount));
  }

  /**
   * Initialize and set the visibility of the grid panes according to the current data.
   */
  private void setInitialGridPaneVisibility() {
    if (!standaloneCourses.isEmpty()) {
      initializeGridPaneStandalone();
      accordionConflictMatrices.setExpandedPane(titledPaneStandaloneCourses);
    } else {
      accordionConflictMatrices.getPanes().remove(titledPaneStandaloneCourses);
    }

    if (!combinableMajorCourses.isEmpty() && !combinableMinorCourses.isEmpty()) {
      initializeGridPaneCombinable();
      accordionConflictMatrices.setExpandedPane(titledPaneCombinableCourses);
    } else {
      accordionConflictMatrices.getPanes().remove(titledPaneCombinableCourses);
      accordionConflictMatrices.getPanes().remove(titledPaneSingleCourses);
    }

    initializeGridPaneSingleCourse();
    highlightImpossibleCombinations();
  }

  /**
   * Highlight the impossible courses, i.e. courses that are statically known to be infeasible.
   */
  @SuppressWarnings("unused")
  private void highlightImpossibleCourses() {
    final Set<Course> majorCourses = new HashSet<>(combinableMajorCourses);
    final Set<Course> minorCourses = new HashSet<>(combinableMinorCourses);

    impossibleCourses.forEach(impossibleCourse -> {
      if (majorCourses.contains(impossibleCourse)) {
        combinableMinorCourses.forEach(minorCourse -> cellMap
            .get(new CourseSelection(impossibleCourse, minorCourse))
            .setResultState(ResultState.IMPOSSIBLE));
      }

      if (minorCourses.contains(impossibleCourse)) {
        combinableMajorCourses.forEach(majorCourse -> cellMap
            .get(new CourseSelection(majorCourse, impossibleCourse))
            .setResultState(ResultState.IMPOSSIBLE));
      }

      cellMap.get(new CourseSelection(impossibleCourse))
          .setResultState(ResultState.IMPOSSIBLE);
    });

    impossibleCoursesAmount.set(cellMap.entrySet().stream().filter(entry ->
        entry.getKey().isCurriculum()
            && entry.getValue().getResultState() != null
            && entry.getValue().getResultState().isImpossible()).count());
  }

  /**
   * Highlight impossible combinations using each major courses' given list of minor courses.
   */
  private void highlightImpossibleCombinations() {
    IntStream.range(0, combinableMinorCourses.size()).forEach(row -> {
      final Course minorCourse = combinableMinorCourses.get(row);
      highlightImpossibleCombinationsForGivenMinor(minorCourse);
    });
  }

  private void highlightImpossibleCombinationsForGivenMinor(final Course minorCourse) {
    IntStream.range(0, combinableMajorCourses.size()).forEach(col -> {
      final Course majorCourse = combinableMajorCourses.get(col);
      if (!majorCourse.getMinorCourses().contains(minorCourse)) {
        cellMap.get(new CourseSelection(majorCourse, minorCourse))
            .setResultState(ResultState.IMPOSSIBLE_COMBINATION);
      }
    });
  }

  private void initializeGridPaneCombinable() {
    final DoubleProperty heightProperty = new SimpleDoubleProperty();
    final DoubleProperty widthProperty = new SimpleDoubleProperty();
    initializeMinorCourseNames(heightProperty);
    initializeMajorCourseNames(widthProperty);
    // add legend like cell at position (0,0)
    gridPaneCombinable.add(getLegendGridCell(heightProperty, widthProperty), 0, 0);
    initializeResultGridCells();
  }

  private void initializeMinorCourseNames(final DoubleProperty heightProperty) {
    IntStream.range(0, combinableMinorCourses.size()).forEach(index -> {
      final Course course = combinableMinorCourses.get(index);
      final CourseGridCell courseGridCell = new CourseGridCell(course.getKey(),
          course.getFullName(), VERTICAL);
      gridPaneCombinable.add(courseGridCell, index + 1, 0);
      // get the height property of a minor names row..
      if (index == combinableMinorCourses.size() - 1) {
        heightProperty.bind(courseGridCell.heightProperty());
      }
    });
  }

  private void initializeMajorCourseNames(final DoubleProperty widthProperty) {
    IntStream.range(0, combinableMajorCourses.size()).forEach(index -> {
      final Course course = combinableMajorCourses.get(index);
      final CourseGridCell courseGridCell = new CourseGridCell(course.getKey(),
          course.getFullName(), "");
      gridPaneCombinable.add(courseGridCell, 0, index + 1);
      // ..and the width property of a major names column
      if (index == combinableMajorCourses.size() - 1) {
        widthProperty.bind(courseGridCell.widthProperty());
      }
    });
  }

  private void initializeResultGridCells() {
    IntStream.range(0, combinableMinorCourses.size()).forEach(col ->
        IntStream.range(0, combinableMajorCourses.size()).forEach(row -> {
          final Course majorCourse = combinableMajorCourses.get(row);
          final Course minorCourse = combinableMinorCourses.get(col);
          final ResultGridCell gridCell = new ResultGridCell(ResultState.UNKNOWN, majorCourse,
              minorCourse);
          gridCell.setRouter(router);
          cellMap.put(new CourseSelection(majorCourse, minorCourse), gridCell);
          gridPaneCombinable.add(gridCell, col + 1, row + 1);
        }));
  }

  /**
   * Create a grid cell for position (0,0) in the conflict matrix yielding a short description on
   * the column and row content like major/minor.
   */
  private Pane getLegendGridCell(final ReadOnlyDoubleProperty heightProperty,
                                 final ReadOnlyDoubleProperty widthProperty) {
    final HBox hBox = new HBox();
    hBox.prefHeightProperty().bind(heightProperty);
    hBox.prefWidthProperty().bind(widthProperty);
    hBox.getStyleClass().addAll("matrix-cell", "windowPaddingTiny");
    final Label minorLabel = new Label(resources.getString("minor"));
    minorLabel.setRotate(270.0);
    minorLabel.prefHeightProperty().bind(widthProperty.multiply(0.15));
    minorLabel.prefWidthProperty().bind(heightProperty.subtract(25.0));
    final Group group = new Group(minorLabel);
    final Label majorLabel = new Label(resources.getString("major"));
    majorLabel.setAlignment(Pos.BOTTOM_CENTER);
    majorLabel.prefHeightProperty().bind(heightProperty);
    majorLabel.prefWidthProperty().bind(widthProperty.multiply(0.85));
    hBox.getChildren().addAll(majorLabel, group);
    return hBox;
  }

  private void initializeGridPaneStandalone() {
    initGridPane(standaloneCourses, gridPaneStandalone, cellMap);
  }

  private void initGridPane(final List<Course> courses,
                            final GridPane gridPane,
                            final Map<CourseSelection, ResultGridCell> cellMap) {
    gridPane.addColumn(0, courses.stream()
        .map(course -> new CourseGridCell(course.getKey(), course.getFullName(), ""))
        .collect(Collectors.toList()).toArray(new Node[] {}));

    IntStream.range(0, courses.size()).forEach(index -> {
      final Course course = courses.get(index);
      final ResultGridCell gridCell = new ResultGridCell(ResultState.UNKNOWN, course);
      gridCell.setRouter(router);
      cellMap.put(new CourseSelection(course), gridCell);
      gridPane.add(gridCell, 1, index);
    });
  }

  private void initializeGridPaneSingleCourse() {
    final ArrayList<Course> singleCourses = new ArrayList<>();

    singleCourses.addAll(combinableMajorCourses);
    singleCourses.addAll(combinableMinorCourses);

    initGridPane(singleCourses, gridPaneSingleCourses, cellMap);
  }

  /**
   * Action of button {@link ConflictMatrix#btCheckAll} to check the feasibility of all
   * combinations.
   */
  @FXML
  @SuppressWarnings("unused")
  public void checkAll() {
    feasibilityCheckRunning.setValue(true);
    setPrepareFeasibilityCheck();
    setExecuteFeasibilityCheck();
    executor.submit(prepareFeasibilityCheck);
  }

  /**
   * Collect all feasibility tasks using {@link CollectFeasibilityTasksTask} and set {@link
   * #prepareFeasibilityCheck} as well as its listener.
   */
  private void setPrepareFeasibilityCheck() {
    prepareFeasibilityCheck = new CollectFeasibilityTasksTask(
        delayedSolverService.get(), combinableMajorCourses,
        combinableMinorCourses, standaloneCourses, results, impossibleCourses);

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
  }

  /**
   * Create the {@link BatchFeasibilityTask} and set {@link #executeFeasibilityCheck} as well as its
   * listener.
   */
  private void setExecuteFeasibilityCheck() {
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

  private MapChangeListener<CourseSelection, ResultState> getCourseResultChangeListener() {
    return change -> {
      final ResultGridCell cell = cellMap.get(change.getKey());

      if (cell.getResultState() == ResultState.IMPOSSIBLE) {
        return;
      }

      if (change.wasAdded()) {
        cell.setResultState(change.getValueAdded());
      } else {
        cell.setResultState(ResultState.UNKNOWN);
      }
    };
  }
}
