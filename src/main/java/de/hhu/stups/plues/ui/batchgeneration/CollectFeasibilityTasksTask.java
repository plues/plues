package de.hhu.stups.plues.ui.batchgeneration;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.keys.CourseSelection;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;


public class CollectFeasibilityTasksTask extends Task<List<SolverTask<Boolean>>> {

  private final Delayed<SolverService> solverServiceDelayed;
  private final CourseSelectionCollector collector;

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final List<Course> courses;

  /**
   * Create tasks for each combination of major and minor course as well as for each standalone
   * course to check their feasibility. Return a set of check feasibility solver tasks. To increase
   * the performance a task is only added to the set if the observable map {@link
   * SolverService#courseSelectionResults} does not contain a result evaluated as true for the
   * combination and there is no impossible course given.
   */
  @Inject
  public CollectFeasibilityTasksTask(final Delayed<SolverService> serviceDelayed,
                                     final CourseSelectionCollector collector,
                                     @Assisted final List<Course> courses) {
    this.solverServiceDelayed = serviceDelayed;
    this.courses = courses;

    this.collector = collector;
    final ResourceBundle resources = ResourceBundle.getBundle("lang.conflictMatrix");

    updateTitle(resources.getString("preparing"));
    updateProgress(0, 100);
  }

  @Override
  protected List<SolverTask<Boolean>> call() throws Exception {
    return collector
        .usingCourses(courses)
        .withCombinations()
        .withSingleCourses()
        .withoutKnownResults()
        .stream()
        .map(this::createTask)
        .collect(Collectors.toList());
  }

  private SolverTask<Boolean> createTask(final CourseSelection courseSelection) {
    final SolverService solverService = this.solverServiceDelayed.get();
    if (courseSelection.isCombination()) {
      return solverService.checkFeasibilityTask(courseSelection.getMajor(),
          courseSelection.getMinor());
    } else {
      return solverService.checkFeasibilityTask(courseSelection.getCourses().get(0));
    }
  }

  @Override
  protected void failed() {
    super.failed();
    logger.error("failed", this.getException());
  }
}
