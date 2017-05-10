package de.hhu.stups.plues.ui.batchgeneration;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.studienplaene.ColorScheme;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.hhu.stups.plues.tasks.PdfRenderingTaskFactory;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

public class CollectPdfRenderingTasksTask extends Task<Set<PdfRenderingTask>> {

  private final SolverService solverService;
  private final PdfRenderingTaskFactory taskFactory;
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final List<Course> majors;
  private final List<Course> minors;
  private final ObjectProperty<ColorScheme> colorSchemeProperty;

  @Inject
  CollectPdfRenderingTasksTask(final Delayed<Store> store,
                               final Delayed<SolverService> solverService,
                               final PdfRenderingTaskFactory factory) {
    this.majors = store.get().getMajors();
    this.minors = store.get().getMinors();
    this.solverService = solverService.get();
    this.taskFactory = factory;
    colorSchemeProperty = new SimpleObjectProperty<>();

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
   */
  private Set<PdfRenderingTask> combineMajorsMinors() {
    final Set<PdfRenderingTask> tasks
        = new HashSet<>(majors.size() * minors.size());

    majors.forEach(majorCourse -> {
      if (!majorCourse.isCombinable()) {
        final PdfRenderingTask task
            = taskFactory.create(majorCourse, null,
            solverService.computeFeasibilityTask(majorCourse), colorSchemeProperty);
        tasks.add(task);
      } else {
        tasks.addAll(collectCourseCombinationTasks(majorCourse));
      }
    });
    return tasks;
  }

  private List<PdfRenderingTask> collectCourseCombinationTasks(final Course majorCourse) {
    return minors.stream()
        .filter(majorCourse::isCombinableWith)
        .map(minorCourse -> taskFactory.create(
            majorCourse, minorCourse,
            solverService.computeFeasibilityTask(majorCourse, minorCourse), colorSchemeProperty))
        .collect(Collectors.toList());
  }

  public ObjectProperty<ColorScheme> colorSchemeProperty() {
    return colorSchemeProperty;
  }

  @Override
  protected void cancelled() {
    logger.info("Generation cancelled.");
  }
}
