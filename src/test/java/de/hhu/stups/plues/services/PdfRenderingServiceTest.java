package de.hhu.stups.plues.services;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.keys.CourseSelection;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.hhu.stups.plues.tasks.PdfRenderingTaskFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class PdfRenderingServiceTest {

  private Delayed<SolverService> delayedSolverService;
  private ExecutorService executorService;
  private PdfRenderingTaskFactory renderingTaskFactory;
  private Course major;
  private PdfRenderingService pdfRenderingService;
  private SolverService solverService;
  private CourseSelection courseSelection;

  /**
   * Test setup.
   */
  @Before
  public void setUp() {
    delayedSolverService = new Delayed<>();
    executorService = mock(ExecutorService.class);
    renderingTaskFactory = mock(PdfRenderingTaskFactory.class);

    major = mock(Course.class);

    pdfRenderingService
        = new PdfRenderingService(delayedSolverService, executorService, renderingTaskFactory);
    //
    solverService = mock(SolverService.class);
    //
    courseSelection = mock(CourseSelection.class);
    when(courseSelection.getMajor()).thenReturn(major);
  }

  @Test
  public void availableProperty() throws Exception {
    final PdfRenderingService pdfRenderingService
        = new PdfRenderingService(delayedSolverService, executorService, renderingTaskFactory);
    assertFalse(pdfRenderingService.availableProperty().getValue());
    delayedSolverService.set(mock(SolverService.class));
    assertTrue(pdfRenderingService.availableProperty().getValue());
  }

  @Test
  public void getTask() throws Exception {
    delayedSolverService.set(solverService);
    //
    pdfRenderingService.getTask(courseSelection);
    verify(solverService, times(1)).computeFeasibilityTask(any());
    verify(solverService, times(0)).computePartialFeasibility(any(), any(), any());
    verify(renderingTaskFactory, times(1)).create(eq(major), any(), any(), any());
  }

  @Test(expected = IllegalStateException.class)
  public void getTaskWithoutSolverService() {
    pdfRenderingService.getTask(courseSelection);
  }

  @Test(expected = IllegalStateException.class)
  public void getTaskPartialTaskWithoutSolverService() throws Exception {
    final Map<Course, List<Module>> moduleChoice = new HashMap<>();
    final Map<Module, List<AbstractUnit>> unitChoice = new HashMap<>();
    //
    pdfRenderingService.getTask(courseSelection, moduleChoice, unitChoice);
  }

  @Test
  public void getTaskPartialTask() throws Exception {
    delayedSolverService.set(solverService);
    //
    final Map<Course, List<Module>> moduleChoice = new HashMap<>();
    final Map<Module, List<AbstractUnit>> unitChoice = new HashMap<>();
    //
    pdfRenderingService.getTask(courseSelection, moduleChoice, unitChoice);
    //
    verify(solverService, times(0)).computeFeasibilityTask(any());
    verify(solverService, times(1))
        .computePartialFeasibility(any(), eq(moduleChoice), eq(unitChoice));
    verify(renderingTaskFactory, times(1))
        .create(eq(major), any(), any(), any());
  }

  @Test
  public void submit() throws Exception {
    final PdfRenderingTask task = mock(PdfRenderingTask.class);
    pdfRenderingService.submit(task);

    verify(executorService, times(1)).submit(task);
  }
}
