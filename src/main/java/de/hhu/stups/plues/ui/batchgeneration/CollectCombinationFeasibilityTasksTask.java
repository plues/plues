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

import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class CollectCombinationFeasibilityTasksTask extends Task<List<SolverTask<Boolean>>> {

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final Delayed<SolverService> solverService;
  private final Course course;
  private final CourseSelectionCollector taskCollector;

  /**
   * Create tasks for each combination of major and minor course.
   */
  @Inject
  public CollectCombinationFeasibilityTasksTask(final Delayed<SolverService> delayedSolverService,
                                                final CourseSelectionCollector taskCollector,
                                                @Assisted  final Course course) {
    this.solverService = delayedSolverService;
    this.course = course;
    this.taskCollector = taskCollector;
    final ResourceBundle resources = ResourceBundle.getBundle("lang.conflictMatrix");

    updateTitle(resources.getString("preparing"));
    updateProgress(0, 100);
  }

  @Override
  protected List<SolverTask<Boolean>> call() throws Exception {
    return taskCollector
        .withCombinations()
        .withoutKnownResults()
        .usingCourses(Collections.singletonList(course))
        .stream()
        .map(this::createTask)
        .collect(Collectors.toList());
  }

  private SolverTask<Boolean> createTask(final CourseSelection courseSelection) {
    if (!courseSelection.isCombination()) {
      throw new IllegalArgumentException("Expected a combination of courses");
    }
    return solverService.get().checkFeasibilityTask(courseSelection.getMajor(),
        courseSelection.getMinor());
  }

  @Override
  protected void failed() {
    super.failed();
    logger.error("failed", this.getException());
  }
}
