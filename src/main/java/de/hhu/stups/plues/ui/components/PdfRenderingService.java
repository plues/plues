package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.studienplaene.Renderer;
import de.hhu.stups.plues.tasks.SolverTask;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;

import javax.xml.parsers.ParserConfigurationException;

public class PdfRenderingService extends Service<Path> {

  private final SolverTask<FeasibilityResult> solverTask;
  private final Delayed<Store> delayedStore;
  private ObjectProperty<Course> major = new SimpleObjectProperty<>();
  private ObjectProperty<Course> minor = new SimpleObjectProperty<>();

  /**
   * Create a service for rendering a pdf.
   * @param delayedStore Store containing necessary data
   * @param executor ExecutorService for background tasks
   * @param solverTask Task containing result if feasible
   */
  @Inject
  public PdfRenderingService(Delayed<Store> delayedStore,
                             final ExecutorService executor,
                             @Assisted SolverTask<FeasibilityResult> solverTask) {
    this.solverTask = solverTask;
    this.delayedStore = delayedStore;
    this.setExecutor(executor);
  }

  @Override
  protected Task<Path> createTask() {
    return new Task<Path>() {
      @Override
      protected Path call() throws Exception {
        solverTask.setOnFailed(event -> this.failed());
        solverTask.setOnCancelled(event -> this.cancel());

        final FeasibilityResult result = solverTask.get();
        final Course majorCourse = major.get();
        final Course minorCourse = minor.get();

        final Store store = delayedStore.get();
        final Renderer renderer
            = new Renderer(store, result.getGroupChoice(), result.getSemesterChoice(),
            result.getModuleChoice(), result.getUnitChoice(), majorCourse, "true");

        File tmp = File.createTempFile("timetable", ".pdf");

        getTempFile(renderer, tmp);
        return Paths.get(tmp.getAbsolutePath());
      }
    };
  }

  @Override
  protected void cancelled() {
    super.cancelled();
    this.solverTask.cancel();
  }

  /**
   * Helper method to get temporary file.
   * @param renderer Renderer object to create file
   * @param temp Temporary file
   */
  private void getTempFile(Renderer renderer, File temp)
      throws IOException, ParserConfigurationException, SAXException {
    try (OutputStream out = new FileOutputStream(temp)) {
      renderer.getResult().writeTo(out);
    } catch (final IOException | ParserConfigurationException | SAXException exc) {
      exc.printStackTrace();
      throw exc;
    }
  }

  public ObjectProperty<Course> majorProperty() {
    return major;
  }

  public ObjectProperty<Course> minorProperty() {
    return minor;
  }
}
