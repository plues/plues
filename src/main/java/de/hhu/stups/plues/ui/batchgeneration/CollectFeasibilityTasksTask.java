package de.hhu.stups.plues.ui.batchgeneration;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.keys.MajorMinorKey;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;

import javafx.collections.ObservableMap;
import javafx.concurrent.Task;

import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;


public class CollectFeasibilityTasksTask extends Task<Set<SolverTask<Boolean>>> {

  private final ResourceBundle resources;
  private SolverService solverService;
  private final List<Course> majorCourses;
  private final List<Course> minorCourses;
  private final List<Course> standaloneCourses;
  private final Set<String> impossibleCourses;
  private ObservableMap<MajorMinorKey, Boolean> courseCombinationResults;

  /**
   * Create tasks for each combination of major and minor course as well as for each standalone
   * course to check their feasibility. Return a set of check feasibility solver tasks. To increase
   * the performance a task is only added to the set if the observable map {@link
   * SolverService#courseCombinationResults} does not contain a result evaluated as true for the
   * combination and there is no impossible course given.
   */
  public CollectFeasibilityTasksTask(SolverService solverService, List<Course> majorCourses,
                                     List<Course> minorCourses, List<Course> standaloneCourses,
                                     ObservableMap<MajorMinorKey, Boolean>
                                         courseCombinationResults,
                                     Set<String> impossibleCourses) {
    this.solverService = solverService;
    this.majorCourses = majorCourses;
    this.minorCourses = minorCourses;
    this.standaloneCourses = standaloneCourses;
    this.courseCombinationResults = courseCombinationResults;
    this.impossibleCourses = impossibleCourses;
    this.resources = ResourceBundle.getBundle("lang.conflictMatrix");
  }

  @Override
  protected Set<SolverTask<Boolean>> call() throws Exception {
    updateTitle(resources.getString("preparing"));
    final Set<SolverTask<Boolean>> feasibilityTasks = new HashSet<>();
    for (final Course majorCourse : majorCourses) {
      if (!majorCourse.isCombinable() && notCheckedYet(
          new MajorMinorKey(majorCourse.getName(), null))) {
        feasibilityTasks.add(solverService.checkFeasibilityTask(majorCourse));
      } else {
        minorCourses.forEach(minorCourse -> {
          if (!majorCourse.getShortName().equals(minorCourse.getShortName())
              && notCheckedYet(new MajorMinorKey(majorCourse.getName(), minorCourse.getName()))) {
            feasibilityTasks.add(solverService.checkFeasibilityTask(majorCourse, minorCourse));
          }
        });
      }
    }
    standaloneCourses.forEach(
        course -> {
          if (notCheckedYet(new MajorMinorKey(course.getName(), null))) {
            feasibilityTasks.add(solverService.checkFeasibilityTask(course));
          }
        });
    return feasibilityTasks;
  }

  /**
   * Check if the feasibility of a combination of courses or a standalone course has already been
   * computed or contains an impossible course. The results are stored in {@link
   * SolverService#courseCombinationResults}. Furthermore, check that the computed result in the
   * cache is true, because the user could have cancelled a task that is feasible normally.
   *
   * @param majorMinorKey The key of the courses.
   * @return Return false if {@link SolverService#courseCombinationResults} contains the key and the
   *         stored result is true or the key contains an impossible course, otherwise return true.
   */
  private boolean notCheckedYet(MajorMinorKey majorMinorKey) {
    return (!courseCombinationResults.containsKey(majorMinorKey)
        || !courseCombinationResults.get(majorMinorKey))
        && !(impossibleCourses.contains(majorMinorKey.getMajor())
        || impossibleCourses.contains(majorMinorKey.getMinor()));
  }

}
