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
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.SetChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
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
  private final MapProperty<CourseSelection, ResultState> results;

  private final Map<CourseSelection, ResultGridCell> cellMap;

  private final BooleanProperty solverProperty;
  private final BooleanProperty feasibilityCheckRunning;
  private final List<Course> courses;
  private final List<Course> combinableMajorCourses;
  private final List<Course> combinableMinorCourses;
  private final List<Course> standaloneCourses;
  private final Set<SolverTask<Boolean>> checkFeasibilityTasks = new HashSet<>();
  private final Set<Course> impossibleCourses;
  private Task<Set<SolverTask<Boolean>>> prepareFeasibilityCheck;
  private BatchFeasibilityTask executeFeasibilityCheck;
  private final LongProperty impossibleCoursesAmount;

  @FXML
  private Accordion accordionConflictMatrices;
  @FXML
  private TitledPane titledPaneCombinableCourses;
  @FXML
  private TitledPane titledPaneStandaloneCourses;
  @FXML
  private TitledPane titledPaneSingleCourses;
  @FXML
  private GridPane gridPaneCombinable;
  @FXML
  private GridPane gridPaneStandalone;
  @FXML
  private GridPane gridPaneSingleCourses;
  @FXML
  private Label lbFeasibleCourseAmount;
  @FXML
  private Label lbInfeasibleCourseAmount;
  @FXML
  private Label lblImpossibleCoursesAmount;
  @FXML
  private Label lbTimeoutCourseAmount;
  @FXML
  private Button btCheckAll;
  @FXML
  private Button btCancelCheckAll;
  @FXML
  private Pane paneLegendSuccess;
  @FXML
  private Pane paneLegendFailure;
  @FXML
  private Pane paneLegendTimeout;
  @FXML
  private Pane paneLegendImpossible;
  @FXML
  private Pane paneLegendInfeasible;

  /**
   * This view presents a matrix of all possible combinations of combinable major and minor courses
   * and if known their feasibility. Furthermore a list of all standalone courses as well as a list
   * of all single courses and if known their feasibility is displayed. Each cell is represented by
   * a {@link ResultGridCell}.
   */
  @Inject
  public ConflictMatrix(final Inflater inflater, final Delayed<Store> delayedStore,
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
      combinableMajorCourses.addAll(courses.stream()
          .filter(c -> c.isMajor() && c.isCombinable()).collect(Collectors.toList()));
      combinableMinorCourses.addAll(courses.stream()
          .filter(c -> c.isMinor() && c.isCombinable()).collect(Collectors.toList()));
      standaloneCourses.addAll(courses.stream()
          .filter(c -> !c.isCombinable()).collect(Collectors.toList()));
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
              &&  entry.getValue().succeeded()).count()), results));

    lbInfeasibleCourseAmount.textProperty().bind(
        Bindings.createStringBinding( () -> String.valueOf(
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
      titledPaneStandaloneCourses.setVisible(false);
    }

    if (!combinableMajorCourses.isEmpty() && !combinableMinorCourses.isEmpty()) {
      initializeGridPaneCombinable();
      accordionConflictMatrices.setExpandedPane(titledPaneCombinableCourses);
    } else {
      titledPaneCombinableCourses.setVisible(false);
      titledPaneSingleCourses.visibleProperty().bind(titledPaneCombinableCourses.visibleProperty());
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
   * Highlight impossible combinations, i.e. combinations with the same major and minor course.
   */
  private void highlightImpossibleCombinations() {
    IntStream.range(0, combinableMinorCourses.size())
        .forEach(row -> IntStream.range(0, combinableMajorCourses.size())
            .forEach(col -> {
              final Course majorCourse = combinableMajorCourses.get(col);
              final Course minorCourse = combinableMinorCourses.get(row);
              if (majorCourse.getShortName()
                  .equals(minorCourse.getShortName())) {
                cellMap.get(
                    new CourseSelection(majorCourse, minorCourse))
                    .setResultState(ResultState.IMPOSSIBLE_COMBINATION);
              }
            }));
  }

  private void initializeGridPaneCombinable() {
    IntStream.range(0, combinableMajorCourses.size())
        .forEach(index -> {
          final Course course = combinableMajorCourses.get(index);
          gridPaneCombinable.add(new CourseGridCell(course.getKey(), course.getFullName(),
              VERTICAL), index + 1, 0);
        });
    IntStream.range(0, combinableMinorCourses.size())
        .forEach(index -> {
          final Course course = combinableMinorCourses.get(index);
          gridPaneCombinable.add(new CourseGridCell(course.getKey(), course.getFullName(), ""),
              0, index + 1);
        });
    IntStream.range(0, combinableMajorCourses.size()).forEach(col ->
        IntStream.range(0, combinableMinorCourses.size())
            .forEach(row -> {
              final Course majorCourse = combinableMajorCourses.get(col);
              final Course minorCourse = combinableMinorCourses.get(row);
              final ResultGridCell gridCell = new ResultGridCell(null, majorCourse,
                  minorCourse);
              gridCell.setRouter(router);
              cellMap.put(
                  new CourseSelection(majorCourse, minorCourse), gridCell);
              gridPaneCombinable.add(gridCell, col + 1, row + 1);
            }));
    gridPaneCombinable.add(new CourseGridCell("", "", ""), 0, 0);
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
      final ResultGridCell gridCell = new ResultGridCell(null, course);
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

  private MapChangeListener<CourseSelection, ResultState> getCourseResultChangeListener() {
    return change -> {
      final ResultGridCell cell = cellMap.get(change.getKey());

      if (cell.getResultState() == ResultState.IMPOSSIBLE) {
        return;
      }

      if (change.wasAdded()) {
        cell.setResultState(change.getValueAdded());
      } else {
        cell.setResultState(null);
      }
    };
  }
}
