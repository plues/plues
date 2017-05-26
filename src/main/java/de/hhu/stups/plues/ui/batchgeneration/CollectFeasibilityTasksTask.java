package de.hhu.stups.plues.ui.batchgeneration;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.keys.CourseSelection;
import de.hhu.stups.plues.prob.ResultState;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;

import javafx.collections.ObservableMap;
import javafx.concurrent.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;


public class CollectFeasibilityTasksTask extends Task<Set<SolverTask<Boolean>>> {

  private final SolverService solverService;
  private final List<Course> majorCourses;
  private final List<Course> minorCourses;
  private final List<Course> standaloneCourses;
  private final Set<Course> impossibleCourses;
  private final ObservableMap<CourseSelection, ResultState> results;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Create tasks for each combination of major and minor course as well as for each standalone
   * course to check their feasibility. Return a set of check feasibility solver tasks. To increase
   * the performance a task is only added to the set if the observable map {@link
   * SolverService#courseSelectionResults} does not contain a result evaluated as true for the
   * combination and there is no impossible course given.
   */
  public CollectFeasibilityTasksTask(final SolverService solverService,
                                     final List<Course> majorCourses,
                                     final List<Course> minorCourses,
                                     final List<Course> standaloneCourses,
                                     final ObservableMap<CourseSelection, ResultState> results,
                                     final Set<Course> impossibleCourses) {
    this.solverService = solverService;
    this.majorCourses = majorCourses;
    this.minorCourses = minorCourses;
    this.standaloneCourses = standaloneCourses;
    this.results = results;
    this.impossibleCourses = impossibleCourses;
    final ResourceBundle resources = ResourceBundle.getBundle("lang.conflictMatrix");

    updateTitle(resources.getString("preparing"));
    updateProgress(0, 100);
  }

  @Override
  protected Set<SolverTask<Boolean>> call() throws Exception {
    final Set<SolverTask<Boolean>> feasibilityTasks = new HashSet<>();
    final int total = majorCourses.size();
    final int[] count = {0};
    majorCourses.forEach(majorCourse -> {

      updateProgress(++count[0], total);

      if (!majorCourse.isCombinable() && CheckCourseCombination
          .shouldBeChecked(results, impossibleCourses, majorCourse)) {
        feasibilityTasks.add(solverService.checkFeasibilityTask(majorCourse));
      } else {
        feasibilityTasks.addAll(majorCourse.getMinorCourses().stream()
            .filter(minorCourse -> CheckCourseCombination
                .shouldBeChecked(results, impossibleCourses, majorCourse, minorCourse))
            .map(minorCourse -> solverService.checkFeasibilityTask(majorCourse, minorCourse))
            .collect(Collectors.toList()));
      }
    });

    feasibilityTasks.addAll(collectTasks(majorCourses));
    feasibilityTasks.addAll(collectTasks(minorCourses));
    feasibilityTasks.addAll(collectTasks(standaloneCourses));

    return feasibilityTasks;
  }

  private List<SolverTask<Boolean>> collectTasks(final List<Course> courses) {
    return courses.stream()
        .filter(course ->
            CheckCourseCombination.shouldBeChecked(results, impossibleCourses, course))
        .map(solverService::checkFeasibilityTask)
        .collect(Collectors.toList());
  }

  @Override
  protected void failed() {
    super.failed();
    logger.error("failed", this.getException());
  }
}
