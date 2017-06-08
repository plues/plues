package de.hhu.stups.plues.ui.batchgeneration;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.keys.CourseSelection;
import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.studienplaene.ColorScheme;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.hhu.stups.plues.tasks.PdfRenderingTaskFactory;
import de.hhu.stups.plues.tasks.SolverTask;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

public class CollectPdfRenderingTasksTask extends Task<Set<PdfRenderingTask>> {

  private final SolverService solverService;
  private final CourseSelectionCollector courseSelectionCollector;
  private final PdfRenderingTaskFactory taskFactory;
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final List<Course> majors;
  private final ObjectProperty<ColorScheme> colorSchemeProperty;

  @Inject
  CollectPdfRenderingTasksTask(final Delayed<Store> store,
                               final Delayed<SolverService> solverService,
                               final CourseSelectionCollector courseSelectionCollector,
                               final PdfRenderingTaskFactory factory) {
    this.majors = store.get().getMajors();
    this.solverService = solverService.get();
    this.courseSelectionCollector = courseSelectionCollector;
    this.taskFactory = factory;
    colorSchemeProperty = new SimpleObjectProperty<>();

    updateTitle(ResourceBundle.getBundle("lang.tasks").getString("preparing"));
    updateProgress(0, 100);
  }


  /**
   * Generate all possible combinations of the given major course and all minor courses (one at
   * a time). If the major course is not combinable just generate a single pdf for this course.
   */
  @Override
  protected Set<PdfRenderingTask> call() throws Exception {
    return courseSelectionCollector
      .withCombinations()
      .withStandaloneCourses()
      .usingCourses(majors)
      .stream()
      .map(this::createTask)
      .collect(Collectors.toSet());
  }

  private PdfRenderingTask createTask(final CourseSelection courseSelection) {
    final Course major = courseSelection.getMajor();
    final Course minor;
    final SolverTask<FeasibilityResult> task;
    if (courseSelection.isCombination()) {
      minor = courseSelection.getMinor();
      task = solverService.computeFeasibilityTask(major, minor);
    } else {
      minor = null;
      task = solverService.computeFeasibilityTask(major);
    }
    return taskFactory.create(major, minor, task, colorSchemeProperty);
  }

  public ObjectProperty<ColorScheme> colorSchemeProperty() {
    return colorSchemeProperty;
  }

  @Override
  protected void cancelled() {
    logger.info("Generation cancelled.");
  }
}
