package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.keys.CourseKey;
import de.hhu.stups.plues.keys.MajorMinorKey;
import de.hhu.stups.plues.prob.ResultState;
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
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyMapProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.MapChangeListener;
import javafx.collections.SetChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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

  private ReadOnlyMapProperty<MajorMinorKey, ResultState> courseCombinationResults;
  private ReadOnlyMapProperty<CourseKey, ResultState> singleCourseResults;
  private final Map<MajorMinorKey, ResultGridCell> combinableCoursesMap;
  private final Map<CourseKey, ResultGridCell> standaloneCoursesMap;
  private final Map<CourseKey, ResultGridCell> singleCoursesMap;
  private final BooleanProperty solverProperty;
  private final BooleanProperty feasibilityCheckRunning;
  private final List<Course> courses;
  private final List<Course> combinableMajorCourses;
  private final List<Course> combinableMinorCourses;
  private final List<Course> standaloneCourses;
  private final IntegerProperty feasibleCoursesAmount;
  private final IntegerProperty infeasibleCoursesAmount;
  private final IntegerProperty timeoutCoursesAmount;

  private final Set<SolverTask<Boolean>> checkFeasibilityTasks = new HashSet<>();
  private final Set<String> impossibleCourses;
  private Task<Set<SolverTask<Boolean>>> prepareFeasibilityCheck;
  private BatchFeasibilityTask executeFeasibilityCheck;
  private long impossibleCoursesAmount;

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
  private GridPane gridPaneLegend;
  @FXML
  @SuppressWarnings("unused")
  private ScrollPane scrollPaneCombinable;
  @FXML
  @SuppressWarnings("unused")
  private ScrollPane scrollPaneStandalone;
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
  private Label lbLegendTimeout;
  @FXML
  @SuppressWarnings("unused")
  private Label lbLegendInfeasible;
  @FXML
  @SuppressWarnings("unused")
  private Label lbLegendImpossible;
  @FXML
  @SuppressWarnings("unused")
  private Label lbFeasibleCourseAmount;
  @FXML
  @SuppressWarnings("unused")
  private Label lbInfeasibleCourseAmount;
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
   * of all single courses and if known their feasibility is displayed.
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

    feasibleCoursesAmount = new SimpleIntegerProperty(0);
    infeasibleCoursesAmount = new SimpleIntegerProperty(0);
    timeoutCoursesAmount = new SimpleIntegerProperty(0);

    combinableCoursesMap = new HashMap<>();
    standaloneCoursesMap = new HashMap<>();
    singleCoursesMap = new HashMap<>();

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
        (SetChangeListener<? super String>) change -> {
          impossibleCourses.addAll(change.getSet());
          highlightImpossibleCourses();
          infeasibleCoursesAmount.setValue(impossibleCoursesAmount);
        });

    delayedSolverService.whenAvailable(solverService -> {
      courseCombinationResults = solverService.getCourseCombinationResults();
      courseCombinationResults.addListener(getCourseResultChangeListener());

      singleCourseResults = solverService.getSingleCourseResults();
      singleCourseResults.addListener(getSingleCourseResultChangeListener());

      solverProperty.set(true);
    });

    inflater.inflate("ConflictMatrix", this, this, "conflictMatrix");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    lbFeasibleCourseAmount.textProperty().bind(Bindings.convert(feasibleCoursesAmount));
    lbInfeasibleCourseAmount.textProperty().bind(Bindings.convert(infeasibleCoursesAmount));
    lbTimeoutCourseAmount.textProperty().bind(Bindings.convert(timeoutCoursesAmount));

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

  /**
   * Initialize and set the visibility of the grid panes according to the current data.
   */
  private void setInitialGridPaneVisibility() {
    if (!standaloneCourses.isEmpty()) {
      initializeGridPaneStandalone();
      accordionConflictMatrices.setExpandedPane(titledPaneStandaloneCourses);
    }
    if (!combinableMajorCourses.isEmpty() && !combinableMinorCourses.isEmpty()) {
      initializeGridPaneCombinable();
      accordionConflictMatrices.setExpandedPane(titledPaneCombinableCourses);
    }
    initializeGridPaneSingleCourse();
    highlightImpossibleCombinations();
  }

  /**
   * Highlight the impossible courses, i.e. courses that are statically known to be infeasible.
   */
  @SuppressWarnings("unused")
  private void highlightImpossibleCourses() {
    final List<String> majorCourseNames = combinableMajorCourses.stream()
        .map(Course::getName).collect(Collectors.toList());
    final List<String> minorCourseNames = combinableMinorCourses.stream()
        .map(Course::getName).collect(Collectors.toList());
    final List<String> standaloneCourseNames = standaloneCourses.stream()
        .map(Course::getName).collect(Collectors.toList());
    impossibleCourses.forEach(impossibleCourseName -> {
      if (majorCourseNames.contains(impossibleCourseName)) {
        combinableMinorCourses.forEach(minorCourse -> combinableCoursesMap
            .get(new MajorMinorKey(impossibleCourseName, minorCourse.getName()))
            .setResultState(ResultState.IMPOSSIBLE));
      }
      if (minorCourseNames.contains(impossibleCourseName)) {
        combinableMajorCourses.forEach(majorCourse -> combinableCoursesMap
            .get(new MajorMinorKey(majorCourse.getName(), impossibleCourseName))
            .setResultState(ResultState.IMPOSSIBLE));
      }
      if (standaloneCourseNames.contains(impossibleCourseName)) {
        standaloneCoursesMap.get(new CourseKey(impossibleCourseName))
            .setResultState(ResultState.IMPOSSIBLE);
      }
      singleCoursesMap.get(new CourseKey(impossibleCourseName))
          .setResultState(ResultState.IMPOSSIBLE);
    });
    impossibleCoursesAmount = combinableCoursesMap.entrySet().stream().filter(entry ->
        entry.getValue().getResultState() != null && entry.getValue().getResultState()
            .equals(ResultState.IMPOSSIBLE)).count();
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
                combinableCoursesMap.get(
                    new MajorMinorKey(majorCourse.getName(), minorCourse.getName()))
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
              final String majorCourseName = combinableMajorCourses.get(col).getName();
              final String minorCourseName = combinableMinorCourses.get(row).getName();
              final ResultGridCell gridCell = new ResultGridCell(null, majorCourseName,
                  minorCourseName);
              combinableCoursesMap.put(new MajorMinorKey(majorCourseName, minorCourseName),
                  gridCell);
              gridPaneCombinable.add(gridCell, col + 1, row + 1);
            }));
    gridPaneCombinable.add(new CourseGridCell("", "", ""), 0, 0);
  }

  private void initializeGridPaneStandalone() {
    IntStream.range(0, standaloneCourses.size())
        .forEach(index -> {
          final Course course = standaloneCourses.get(index);
          gridPaneStandalone.add(
              new CourseGridCell(course.getKey(), course.getFullName(), ""), 0, index);
        });
    IntStream.range(0, standaloneCourses.size())
        .forEach(index -> {
          final String courseName = standaloneCourses.get(index).getName();
          final ResultGridCell gridCell = new ResultGridCell(null, courseName);
          standaloneCoursesMap.put(new CourseKey(courseName), gridCell);
          gridPaneStandalone.add(gridCell, 1, index);
        });
  }

  private void initializeGridPaneSingleCourse() {
    IntStream.range(0, courses.size())
        .forEach(index -> {
          final Course course = courses.get(index);
          gridPaneSingleCourses.add(
              new CourseGridCell(course.getKey(), course.getFullName(), ""), 0, index);
        });
    IntStream.range(0, courses.size())
        .forEach(index -> {
          final String courseName = courses.get(index).getName();
          final ResultGridCell gridCell = new ResultGridCell(null, courseName);
          singleCoursesMap.put(new CourseKey(courseName), gridCell);
          gridPaneSingleCourses.add(gridCell, 1, index);
        });
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

  @SuppressWarnings("unused")
  private void restoreInitialState() {
    gridPaneCombinable.getChildren().clear();
    gridPaneStandalone.getChildren().clear();
    gridPaneSingleCourses.getChildren().clear();

    initializeGridPaneCombinable();
    initializeGridPaneStandalone();
    initializeGridPaneSingleCourse();
    highlightImpossibleCombinations();
    highlightImpossibleCourses();

    feasibleCoursesAmount.setValue(0);
    infeasibleCoursesAmount.setValue(impossibleCoursesAmount);
    timeoutCoursesAmount.setValue(0);
  }

  private MapChangeListener<MajorMinorKey, ResultState> getCourseResultChangeListener() {
    return change -> {
      if (change.wasAdded()) {
        final MajorMinorKey key = change.getKey();
        if (key.hasMinor() && combinableCoursesMap.get(key).getResultState()
            != ResultState.IMPOSSIBLE) {
          combinableCoursesMap.get(key).setResultState(change.getValueAdded());
        } else {
          final CourseKey courseKey = new CourseKey(key.getMajor());
          if (standaloneCoursesMap.keySet().contains(courseKey)
              && standaloneCoursesMap.get(courseKey).getResultState() != ResultState.IMPOSSIBLE) {
            standaloneCoursesMap.get(courseKey)
                .setResultState(change.getValueAdded());
          }
        }
        Platform.runLater(this::updateCourseStatistics);
      } else {
        // discard all if a session has been moved
        Platform.runLater(this::restoreInitialState);
      }
    };
  }

  private MapChangeListener<CourseKey, ResultState> getSingleCourseResultChangeListener() {
    return change -> {
      if (change.wasAdded() && singleCoursesMap.get(change.getKey()).getResultState()
          != ResultState.IMPOSSIBLE) {
        singleCoursesMap.get(change.getKey()).setResultState(change.getValueAdded());
      }
    };
  }

  @SuppressWarnings("unused")
  private void updateCourseStatistics() {
    feasibleCoursesAmount.setValue(courseCombinationResults.entrySet().stream()
        .filter(entry -> entry.getValue().equals(ResultState.SUCCEEDED)).count());
    infeasibleCoursesAmount.setValue(courseCombinationResults.entrySet().stream()
        .filter(entry -> entry.getValue().equals(ResultState.FAILED)).count()
        + impossibleCoursesAmount);
    timeoutCoursesAmount.setValue(courseCombinationResults.entrySet().stream()
        .filter(entry -> entry.getValue().equals(ResultState.TIMEOUT)).count());
  }
}
