package de.hhu.stups.plues.ui.batchgeneration;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.keys.CourseSelection;
import de.hhu.stups.plues.prob.ResultState;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;

import javafx.collections.ObservableMap;
import javafx.concurrent.Task;

import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;


public class CollectFeasibilityTasksTask extends Task<Set<SolverTask<Boolean>>> {

  private final ResourceBundle resources;
  private final SolverService solverService;
  private final List<Course> majorCourses;
  private final List<Course> minorCourses;
  private final List<Course> standaloneCourses;
  private final Set<Course> impossibleCourses;
  private final ObservableMap<CourseSelection, ResultState> results;

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
    resources = ResourceBundle.getBundle("lang.conflictMatrix");

    updateTitle(resources.getString("preparing"));
    updateProgress(0, 100);
  }

  @Override
  protected Set<SolverTask<Boolean>> call() throws Exception {
    updateTitle(resources.getString("preparing"));
    final Set<SolverTask<Boolean>> feasibilityTasks = new HashSet<>();
    majorCourses.forEach(majorCourse -> {
      if (!majorCourse.isCombinable() && shouldBeChecked(majorCourse)) {
        feasibilityTasks.add(solverService.checkFeasibilityTask(majorCourse));
      } else {
        feasibilityTasks.addAll(minorCourses.stream()
            .filter(majorCourse::isCombinableWith)
            .filter(minorCourse -> shouldBeChecked(majorCourse, minorCourse))
            .map(minorCourse -> solverService.checkFeasibilityTask(majorCourse, minorCourse))
            .collect(Collectors.toList()));
      }
    });

    feasibilityTasks.addAll(collectTasks(standaloneCourses));

    // also check the results for all single courses
    feasibilityTasks.addAll(collectTasks(majorCourses));
    feasibilityTasks.addAll(collectTasks(minorCourses));

    return feasibilityTasks;
  }

  private List<SolverTask<Boolean>> collectTasks(final List<Course> courses) {
    return courses.stream()
        .filter(this::shouldBeChecked)
        .map(solverService::checkFeasibilityTask)
        .collect(Collectors.toList());
  }

  /**
   * Check if the feasibility of a combination of courses or a standalone course has already been
   * computed or contains an impossible course. The results are stored in {@link
   * SolverService#courseSelectionResults}. Furthermore, check that the computed result in the
   * cache is true, because the user could have cancelled a task that is feasible normally.
   *
   * @param courses The key of the courses.
   * @return Return false if {@link SolverService#courseSelectionResults} contains the key and the
   *         stored result is true or the key contains an impossible course, otherwise return true.
   */
  private boolean shouldBeChecked(final Course ... courses) {
    final CourseSelection courseSelection = new CourseSelection(courses);
    // if course has been successfully checked we do not want to check it again
    return !results.getOrDefault(courseSelection, ResultState.FAILED).succeeded()
        && canBeChecked(courseSelection);

  }

  private boolean canBeChecked(final CourseSelection courseSelection) {
    // if the given selection contains impossible courses we do not bother to check it.
    for (final Course course : courseSelection.getCourses()) {
      if (!impossibleCourses.contains(course)) {
        continue;
      }
      return false;
    }
    return true;
  }
}
