package de.hhu.stups.plues.services;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.keys.CourseSelection;
import de.hhu.stups.plues.prob.ResultState;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.batchgeneration.BatchFeasibilityTask;
import de.hhu.stups.plues.ui.batchgeneration.CollectCombinationFeasibilityTasksTask;
import de.hhu.stups.plues.ui.batchgeneration.CollectFeasibilityTasksTask;
import de.hhu.stups.plues.ui.batchgeneration.CourseSelectionCollector;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;
import javafx.collections.SetChangeListener;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;


public class ConflictMatrixService {
  private final BooleanProperty available = new SimpleBooleanProperty(false);
  private final Delayed<SolverService> delayedSolverService;
  private final BooleanProperty isCheckRunning = new SimpleBooleanProperty(false);
  private final UiDataService uiDataService;
  private final CourseSelectionCollector courseSelectionCollector;
  private final ExecutorService executorService;
  private Task<List<SolverTask<Boolean>>> prepareFeasibilityCheck;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final List<Course> courses = new ArrayList<>();
  private final List<Course> combinableMajorCourses = new ArrayList<>();
  private final List<Course> combinableMinorCourses = new ArrayList<>();
  private final List<Course> standaloneCourses = new ArrayList<>();
  private final Set<Course> impossibleCourses = new HashSet<>();

  public MapProperty<CourseSelection, ResultState> resultsProperty() {
    return results;
  }

  private final MapProperty<CourseSelection, ResultState> results
      = new SimpleMapProperty<>(FXCollections.emptyObservableMap());
  private BatchFeasibilityTask executeFeasibilityCheck;
  private final Set<SolverTask<Boolean>> checkFeasibilityTasks = new HashSet<>();


  /**
   * Service class for ConflictMatrix.
   * Class Provides different checks and state properties required by the conflict matrix.
   * @param delayedSolverService Delayed SolverService object
   * @param delayedStore Delayed Store object
   * @param uiDataService UiDataService object
   * @param executorService Executor
   */
  @Inject
  public ConflictMatrixService(final Delayed<SolverService> delayedSolverService,
                               final Delayed<Store> delayedStore,
                               final UiDataService uiDataService,
                               final CourseSelectionCollector courseSelectionCollector,
                               final ExecutorService executorService) {
    this.delayedSolverService = delayedSolverService;
    this.uiDataService = uiDataService;
    this.courseSelectionCollector = courseSelectionCollector;
    this.executorService = executorService;

    delayedSolverService.whenAvailable(store -> {
      logger.debug("MatrixService Available");
      this.available.set(true);
    });

    delayedSolverService.whenAvailable(solverService
        -> results.bind(solverService.courseSelectionResultsProperty()));

    delayedStore.whenAvailable(store -> {
      courses.addAll(store.getCourses().stream()
          .sorted(Comparator.comparing(Course::getShortName)).collect(Collectors.toList()));
      standaloneCourses.addAll(courses.stream()
          .filter(c -> !c.isCombinable()).collect(Collectors.toList()));

      combinableMajorCourses.addAll(store.getMajors().stream()
          .filter(Course::isCombinable).collect(Collectors.toList()));
      combinableMinorCourses.addAll(store.getMinors().stream()
          .filter(Course::isCombinable).collect(Collectors.toList()));
    });

    uiDataService.impossibleCoursesProperty().addListener(
        (SetChangeListener<? super Course>) change -> impossibleCourses.addAll(change.getSet()));
  }

  public BooleanProperty availableProperty() {
    return this.available;
  }

  public BooleanProperty isCheckRunningProperty() {
    return this.isCheckRunning;
  }

  /**
   * Check all courses and combinations of major and minor in the available data.
   */
  public void checkAll() {
    if (!this.available.get()) {
      throw new IllegalStateException("SolverService is not available!.");
    }
    isCheckRunning.setValue(true);
    setPrepareFeasibilityCheck();
    setExecuteFeasibilityCheck();
    executorService.submit(prepareFeasibilityCheck);
  }

  /**
   * Collect all feasibility tasks using {@link CollectFeasibilityTasksTask} by considering all
   * courses.
   */
  private void setPrepareFeasibilityCheck() {
    prepareFeasibilityCheck = new CollectFeasibilityTasksTask(delayedSolverService.get(),
        courses, courseSelectionCollector);

    setPrepareFeasibilityTaskListener(prepareFeasibilityCheck);
  }

  /**
   * Create the {@link BatchFeasibilityTask} and set {@link #executeFeasibilityCheck} as well as its
   * listener.
   */
  private void setExecuteFeasibilityCheck() {
    executeFeasibilityCheck = new BatchFeasibilityTask(executorService, checkFeasibilityTasks);

    executeFeasibilityCheck.setOnCancelled(event -> {
      checkFeasibilityTasks.forEach(task -> {
        if (!task.isDone()) {
          task.cancel(true);
        }
      });
      isCheckRunning.setValue(false);
      checkFeasibilityTasks.clear();
    });

    executeFeasibilityCheck.setOnFailed(event -> {
      isCheckRunning.setValue(false);
      checkFeasibilityTasks.clear();
    });

    executeFeasibilityCheck.setOnSucceeded(event -> {
      isCheckRunning.setValue(false);
      checkFeasibilityTasks.clear();
    });
  }

  private void setPrepareFeasibilityTaskListener(final Task<List<SolverTask<Boolean>>> task) {
    task.setOnCancelled(event -> {
      isCheckRunning.setValue(false);
      checkFeasibilityTasks.clear();
    });
    task.setOnFailed(event -> {
      isCheckRunning.setValue(false);
      checkFeasibilityTasks.clear();
    });
    task.setOnSucceeded(event -> {
      checkFeasibilityTasks.addAll(prepareFeasibilityCheck.getValue());
      executeFeasibilityCheck.setTasks(checkFeasibilityTasks);
      executorService.submit(executeFeasibilityCheck);
    });
  }

  /**
   * Check all combinations of course with its minors or majors respectively.
   * @param courseToBeCombined Course to be checked
   */
  public void checkAllCombinations(final Course courseToBeCombined) {
    if (!this.available.get()) {
      throw new IllegalStateException("SolverService is not available!.");
    }
    if (!courseToBeCombined.isCombinable()) {
      return;
    }
    setExplicitPrepareFeasibilityCheck(courseToBeCombined);
    isCheckRunning.setValue(true);
    setExecuteFeasibilityCheck();
    executorService.submit(prepareFeasibilityCheck);
  }

  /**
   * Collect all feasibility tasks using {@link CollectCombinationFeasibilityTasksTask} for
   * explicitly given combinations of courses.
   */
  private void setExplicitPrepareFeasibilityCheck(final Course course) {
    prepareFeasibilityCheck
        = new CollectCombinationFeasibilityTasksTask(delayedSolverService.get(), course,
      courseSelectionCollector);
    setPrepareFeasibilityTaskListener(prepareFeasibilityCheck);
  }

  /**
   * Cancel all running tasks managed by this service.
   */
  public void cancelRunningTasks() {
    if (prepareFeasibilityCheck.isRunning()) {
      prepareFeasibilityCheck.cancel(true);
    }
    if (executeFeasibilityCheck.isRunning()) {
      executeFeasibilityCheck.cancel(true);
    }
    isCheckRunning.setValue(false);
    checkFeasibilityTasks.clear();
  }

  public SetProperty<Course> impossibleCoursesProperty() {
    return uiDataService.impossibleCoursesProperty();
  }
}
