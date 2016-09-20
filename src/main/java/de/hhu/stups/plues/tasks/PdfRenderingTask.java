package de.hhu.stups.plues.tasks;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.studienplaene.Renderer;

import javafx.concurrent.Task;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;

public class PdfRenderingTask extends Task<Path> {

  private final Delayed<Store> delayedStore;
  private final Course major;
  private final Course minor;
  private final Delayed<SolverService> delayedSolverService;
  private SolverTask<FeasibilityResult> solverTask;


  /**
   * Create a task for rendering a pdf.
   *
   * @param delayedStore         Store containing necessary data
   * @param major Course major or integrated course
   * @param minor Course minor course, can be null
   */
  @Inject
  protected PdfRenderingTask(final Delayed<Store> delayedStore,
                             final Delayed<SolverService> delayedSolverService,
                             @Assisted("major") final Course major,
                             @Assisted("minor") @Nullable final Course minor,
                             @Assisted final SolverTask<FeasibilityResult> solverTask) {
    this.delayedStore = delayedStore;
    this.delayedSolverService = delayedSolverService;
    this.major = major;
    this.minor = minor;
    this.solverTask = solverTask;
  }

  @Override
  protected Path call() throws Exception {
    updateTitle("Rendering PDF");
    // we have to read from the task here, the future does not provide a result.
    final SolverService solver = delayedSolverService.get();
    assert solver != null;

    updateMessage("Submit Solver");
    updateProgress(20, 100);

    if (this.isCancelled()) {
      return null;
    }
    solver.submit(solverTask);

    updateMessage("Waiting for Solver...");
    updateProgress(40, 100);

    int percentage = 0;
    while (!solverTask.isDone()) {
      percentage = (percentage + 1) % 20 + 40;
      updateProgress(percentage, 100);
      if (solverTask.isCancelled() || this.isCancelled()) {
        updateMessage("Task cancelled");
        break;
      }
      try {
        TimeUnit.MILLISECONDS.sleep(200);
      } catch (final InterruptedException exception) {
        if (solverTask.isCancelled() || this.isCancelled()) {
          break;
        }
      }
    }

    if (this.isCancelled() || solverTask.isCancelled()) {
      this.cancel();
      return null;
    }

    // we have to read from the task here, the future does not provide a result.
    final FeasibilityResult result = solverTask.get();

    final Store store = delayedStore.get();

    updateMessage("Rendering");
    updateProgress(60, 100);

    final Renderer renderer = getRenderer(store, result);

    updateProgress(80, 100);

    final File tmp = File.createTempFile("timetable", ".pdf");
    getTempFile(renderer, tmp);

    updateMessage("Rendering finished");
    updateProgress(100, 100);

    return Paths.get(tmp.getAbsolutePath());
  }

  private Renderer getRenderer(final Store store, final FeasibilityResult result) {
    if (this.minor == null) {
      return new Renderer(store, result.getGroupChoice(), result.getSemesterChoice(),
        result.getModuleChoice(), this.major, "true");
    } else {
      return new Renderer(store, result.getGroupChoice(), result.getSemesterChoice(),
        result.getModuleChoice(), this.major, this.minor, "true");
    }
  }

  @Override
  protected void cancelled() {
    super.cancelled();
    if (solverTask != null) {
      solverTask.cancel(true);
    }
  }

  @Override
  protected void failed() {
    super.failed();
    if (solverTask != null && solverTask.isRunning()) {
      solverTask.cancel(true);
    }
  }

  /**
   * Helper method to get temporary file.
   *
   * @param renderer Renderer object to create file
   * @param temp     Temporary file
   */
  private void getTempFile(final Renderer renderer, final File temp)
      throws IOException, ParserConfigurationException, SAXException {
    try (OutputStream out = new FileOutputStream(temp)) {
      renderer.getResult().writeTo(out);
    } catch (final IOException | ParserConfigurationException | SAXException exc) {
      exc.printStackTrace();
      throw exc;
    }
  }

  public Course getMinor() {
    return minor;
  }

  public Course getMajor() {
    return major;
  }
}
