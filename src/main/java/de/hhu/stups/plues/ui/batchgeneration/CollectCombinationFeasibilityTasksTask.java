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

public class CollectCombinationFeasibilityTasksTask extends Task<Set<SolverTask<Boolean>>> {

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final SolverService solverService;
  private final List<CourseSelection> courseSelections;
  private final ObservableMap<CourseSelection, ResultState> results;
  private final Set<Course> impossibleCourses;

  /**
   * Create tasks for each combination of major and minor course.
   */
  public CollectCombinationFeasibilityTasksTask(final SolverService solverService,
                                                final List<CourseSelection> courseSelections,
                                                final ObservableMap<CourseSelection, ResultState>
                                                    results,
                                                final Set<Course> impossibleCourses) {
    this.solverService = solverService;
    this.courseSelections = courseSelections;
    this.results = results;
    this.impossibleCourses = impossibleCourses;
    final ResourceBundle resources = ResourceBundle.getBundle("lang.conflictMatrix");

    updateTitle(resources.getString("preparing"));
    updateProgress(0, 100);
  }

  @Override
  protected Set<SolverTask<Boolean>> call() throws Exception {
    final Set<SolverTask<Boolean>> feasibilityTasks = new HashSet<>();
    final int total = courseSelections.size();
    final int[] count = {0};
    courseSelections.forEach(coursePair -> {
      updateProgress(++count[0], total);
      final Course majorCourse = coursePair.getMajor();
      final Course minorCourse = coursePair.getMinor();
      if (CheckCourseCombination.shouldBeChecked(results, impossibleCourses, majorCourse,
          minorCourse)) {
        feasibilityTasks.add(solverService.checkFeasibilityTask(majorCourse, minorCourse));
      }
    });
    return feasibilityTasks;
  }

  @Override
  protected void failed() {
    super.failed();
    logger.error("failed", this.getException());
  }
}
