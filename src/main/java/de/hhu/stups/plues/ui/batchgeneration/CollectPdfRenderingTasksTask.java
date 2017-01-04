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
import java.util.stream.Collectors;

public class CollectPdfRenderingTasksTask extends Task<Set<PdfRenderingTask>> {
  private final List<Course> courses;
  private final SolverService solverService;
  private final PdfRenderingTaskFactory taskFactory;
  private final Logger logger = LoggerFactory.logger(getClass());

  @Inject
  CollectPdfRenderingTasksTask(final Delayed<Store> store,
                               final Delayed<SolverService> solverService,
                               final PdfRenderingTaskFactory factory) {

    this.courses = store.get().getCourses();
    this.solverService = solverService.get();
    this.taskFactory = factory;
  }

  @Override
  protected Set<PdfRenderingTask> call() throws Exception {
    updateTitle(ResourceBundle.getBundle("lang.tasks").getString("preparing"));
    final List<Course> majorCourseList = courses.stream()
        .filter(Course::isMajor)
        .collect(Collectors.toList());

    final List<Course> minorCourseList = courses.stream()
        .filter(Course::isMinor)
        .collect(Collectors.toList());

    return combineMajorsMinors(majorCourseList, minorCourseList);
  }

  /**
   * Generate all possible combinations of the given major course and all minor courses (one at
   * a time). If the major course is not combinable just generate a single pdf for this course.
   *
   * @param majorCourseList The list of all major courses.
   * @param minorCourseList The list of all minor courses.
   */
  private Set<PdfRenderingTask> combineMajorsMinors(final List<Course> majorCourseList,
                                                    final List<Course> minorCourseList) {
    final Set<PdfRenderingTask> tasks
        = new HashSet<>(majorCourseList.size() * minorCourseList.size());

    for (final Course majorCourse : majorCourseList) {
      SolverTask<FeasibilityResult> solverTask;
      if (!majorCourse.isCombinable()) {
        solverTask = solverService.computeFeasibilityTask(majorCourse);
        final PdfRenderingTask task = taskFactory.create(majorCourse, null, solverTask);
        tasks.add(task);
      } else {
        for (final Course minorCourse : minorCourseList) {
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
