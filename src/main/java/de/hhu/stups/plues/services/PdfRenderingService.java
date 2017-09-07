package de.hhu.stups.plues.services;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.keys.CourseSelection;
import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.hhu.stups.plues.tasks.PdfRenderingTaskFactory;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.components.PdfGenerationSettings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

public class PdfRenderingService {

  private final Delayed<SolverService> delayedSolverService;
  private final PdfRenderingTaskFactory renderingTaskFactory;
  private final ExecutorService executor;
  private final SimpleBooleanProperty available = new SimpleBooleanProperty(false);
  private final SimpleObjectProperty<PdfGenerationSettings> pdfGenerationSettingsProperty =
    new SimpleObjectProperty<>();

  /**
   * xxx.
   *
   * @param delayedSolverService SolverService for usage of ProB solver
   */
  @Inject
  public PdfRenderingService(final Delayed<SolverService> delayedSolverService,
                             final ExecutorService executor,
                             final PdfRenderingTaskFactory renderingTaskFactory) {
    this.delayedSolverService = delayedSolverService;
    this.executor = executor;
    this.renderingTaskFactory = renderingTaskFactory;
    this.delayedSolverService.whenAvailable(solverService -> this.available.set(true));
  }

  public ObservableValue<Boolean> availableProperty() {
    return this.available;
  }

  /**
   * Create a PdfRenderingTask for the given course selection.
   *
   * @param courseSelection {@link CourseSelection}
   * @return {@link PdfRenderingTask}
   */
  public PdfRenderingTask getTask(final CourseSelection courseSelection) {
    if (!this.available.get()) {
      throw new IllegalStateException("SolverService is not available!.");
    }
    final SolverService solverService = delayedSolverService.get();
    final SolverTask<FeasibilityResult> solverTask
      = solverService.computeFeasibilityTask(courseSelection.getCourses().toArray(new Course[0]));
    return getPdfRenderingTask(courseSelection, solverTask);
  }

  /**
   * Create a {@link PdfRenderingTask} for a given course selection and a partial selection of
   * modules, and abstract units.
   *
   * @param courseSelection {@link CourseSelection}
   * @param moduleChoice    Map from {@link Course} to a list selected {@link Module} objects that
   *                        are selected for that course.
   * @param unitChoice      Map from {@link Module} objects to a list of selected {@link
   *                        AbstractUnit} objects for that module.
   * @return {@link PdfRenderingTask}
   */
  public PdfRenderingTask getTask(final CourseSelection courseSelection,
                                  final Map<Course, List<Module>> moduleChoice,
                                  final Map<Module, List<AbstractUnit>> unitChoice) {
    if (!this.available.get()) {
      throw new IllegalStateException("SolverService is not available!.");
    }
    final SolverService solverService = delayedSolverService.get();
    //
    final SolverTask<FeasibilityResult> solverTask
      = solverService.computePartialFeasibility(courseSelection.getCourses(),
      moduleChoice, unitChoice);
    //
    return getPdfRenderingTask(courseSelection, solverTask);
  }

  private PdfRenderingTask getPdfRenderingTask(final CourseSelection courseSelection,
                                               final SolverTask<FeasibilityResult> solverTask) {
    final Course major = courseSelection.getMajor();
    Course minor = null;
    if (courseSelection.isCombination()) {
      minor = courseSelection.getMinor();
    }
    return renderingTaskFactory.create(major, minor, solverTask, this.pdfGenerationSettingsProperty);
  }

  public void submit(final PdfRenderingTask task) {
    this.executor.submit(task);
  }

  public ObjectProperty<PdfGenerationSettings> pdfGenerationSettingsProperty() {
    return this.pdfGenerationSettingsProperty;
  }
}
