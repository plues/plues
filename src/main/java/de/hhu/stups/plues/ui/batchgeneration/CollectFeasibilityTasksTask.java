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
  }

  @Override
  protected Set<SolverTask<Boolean>> call() throws Exception {
    updateTitle(resources.getString("preparing"));
    final Set<SolverTask<Boolean>> feasibilityTasks = new HashSet<>();
    for (final Course majorCourse : majorCourses) {
      if (!majorCourse.isCombinable() && notCheckedYet(
          new CourseSelection(majorCourse))) {
        feasibilityTasks.add(solverService.checkFeasibilityTask(majorCourse));
      } else {
        minorCourses.forEach(minorCourse -> {
          if (!majorCourse.getShortName().equals(minorCourse.getShortName())
              && notCheckedYet(new CourseSelection(majorCourse, minorCourse))) {
            feasibilityTasks.add(solverService.checkFeasibilityTask(majorCourse, minorCourse));
          }
        });
      }
    }
    feasibilityTasks.addAll(standaloneCourses.parallelStream()
        .filter(course -> notCheckedYet(new CourseSelection(course)))
        .map(solverService::checkFeasibilityTask)
        .collect(Collectors.toList()));

    // also check the results for all single courses
    majorCourses.forEach(majorCourse ->
        feasibilityTasks.add(solverService.checkFeasibilityTask(majorCourse)));
    minorCourses.forEach(minorCourse ->
        feasibilityTasks.add(solverService.checkFeasibilityTask(minorCourse)));
    return feasibilityTasks;
  }

  /**
   * Check if the feasibility of a combination of courses or a standalone course has already been
   * computed or contains an impossible course. The results are stored in {@link
   * SolverService#courseSelectionResults}. Furthermore, check that the computed result in the
   * cache is true, because the user could have cancelled a task that is feasible normally.
   *
   * @param courseSelection The key of the courses.
   * @return Return false if {@link SolverService#courseSelectionResults} contains the key and the
   *         stored result is true or the key contains an impossible course, otherwise return true.
   */
  private boolean notCheckedYet(final CourseSelection courseSelection) {
    // if course has been successfully checked we do not want to check it again
    if (results.containsKey(courseSelection)
        && !ResultState.SUCCEEDED.equals(results.get(courseSelection))) {
      return false;
    }
    return shouldBeChecked(courseSelection);
  }

  private boolean shouldBeChecked(final CourseSelection courseSelection) {
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
