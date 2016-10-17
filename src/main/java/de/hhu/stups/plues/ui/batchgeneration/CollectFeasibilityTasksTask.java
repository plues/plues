package de.hhu.stups.plues.ui.batchgeneration;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;

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

  /**
   * Create tasks for each combination of major and minor course as well as for each standalone
   * course to check their feasibility. Return a set of check feasibility solver tasks.
   */
  public CollectFeasibilityTasksTask(SolverService solverService, List<Course> majorCourses,
                                     List<Course> minorCourses, List<Course> standaloneCourses) {
    this.solverService = solverService;
    this.majorCourses = majorCourses;
    this.minorCourses = minorCourses;
    this.standaloneCourses = standaloneCourses;
    this.resources = ResourceBundle.getBundle("lang.conflictMatrix");
  }


  @Override
  protected Set<SolverTask<Boolean>> call() throws Exception {
    updateTitle(resources.getString("preparing"));
    final Set<SolverTask<Boolean>> feasibilityTasks = new HashSet<>();
    for (final Course majorCourse : majorCourses) {
      if (!majorCourse.isCombinable()) {
        feasibilityTasks.add(solverService.checkFeasibilityTask(majorCourse));
      } else {
        minorCourses.forEach(minorCourse -> {
          if (!majorCourse.getShortName().equals(minorCourse.getShortName())) {
            feasibilityTasks.add(solverService.checkFeasibilityTask(majorCourse, minorCourse));
          }
        });
      }
    }
    standaloneCourses.forEach(
        course -> feasibilityTasks.add(solverService.checkFeasibilityTask(course)));
    return feasibilityTasks;
  }

}
