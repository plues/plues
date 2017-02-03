package de.hhu.stups.plues.ui.batchgeneration;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.hhu.stups.plues.tasks.PdfRenderingTaskFactory;
import de.hhu.stups.plues.tasks.SolverTask;
import javafx.concurrent.Task;
import org.hibernate.annotations.common.util.impl.LoggerFactory;
import org.jboss.logging.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

public class CollectPdfRenderingTasksTask extends Task<Set<PdfRenderingTask>> {
  private final SolverService solverService;
  private final PdfRenderingTaskFactory taskFactory;
  private final Logger logger = LoggerFactory.logger(getClass());
  private final List<Course> majors;
  private final List<Course> minors;

  @Inject
  CollectPdfRenderingTasksTask(final Delayed<Store> store,
                               final Delayed<SolverService> solverService,
                               final PdfRenderingTaskFactory factory) {

    this.majors = store.get().getMajors();
    this.minors = store.get().getMinors();
    this.solverService = solverService.get();
    this.taskFactory = factory;

    updateTitle(ResourceBundle.getBundle("lang.tasks").getString("preparing"));
    updateProgress(0, 100);
  }

  @Override
  protected Set<PdfRenderingTask> call() throws Exception {
    return combineMajorsMinors();
  }

  /**
   * Generate all possible combinations of the given major course and all minor courses (one at
   * a time). If the major course is not combinable just generate a single pdf for this course.
   *
   */
  private Set<PdfRenderingTask> combineMajorsMinors() {
    final Set<PdfRenderingTask> tasks
        = new HashSet<>(majors.size() * minors.size());

    for (final Course majorCourse : majors) {
      SolverTask<FeasibilityResult> solverTask;
      if (!majorCourse.isCombinable()) {
        solverTask = solverService.computeFeasibilityTask(majorCourse);
        final PdfRenderingTask task = taskFactory.create(majorCourse, null, solverTask);
        tasks.add(task);
      } else {
        for (final Course minorCourse : minors) {
          if (!majorCourse.isCombinableWith(minorCourse)) {
            continue;
          }
          solverTask = solverService.computeFeasibilityTask(majorCourse, minorCourse);
          tasks.add(taskFactory.create(majorCourse, minorCourse, solverTask));
        }
      }
    }
    return tasks;
  }

  @Override
  protected void cancelled() {
    logger.info("Generation cancelled.");
  }
}
