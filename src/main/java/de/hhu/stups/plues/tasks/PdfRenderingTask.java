package de.hhu.stups.plues.tasks;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.studienplaene.Renderer;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.hibernate.annotations.common.util.impl.LoggerFactory;
import org.jboss.logging.Logger;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;

public class PdfRenderingTask extends Task<Path> {

  private final Delayed<Store> delayedStore;
  private final Course major;
  private final Course minor;
  private final SolverTask<FeasibilityResult> solverTask;

  private final Logger logger = LoggerFactory.logger(getClass());
  private final ResourceBundle resources;
  private static final ListeningExecutorService EXECUTOR_SERVICE;

  static {
    final ThreadFactory threadFactoryBuilder
        = new ThreadFactoryBuilder().setDaemon(true)
          .setNameFormat("pdfrendering-task-runner-%d").build();

    EXECUTOR_SERVICE = MoreExecutors.listeningDecorator(
      Executors.newSingleThreadExecutor(threadFactoryBuilder));
  }


  /**
   * Create a task for rendering a pdf.
   *
   * @param delayedStore         Store containing necessary data
   * @param major Course major or integrated course
   * @param minor Course minor course, can be null
   */
  @Inject
  protected PdfRenderingTask(final Delayed<Store> delayedStore,
                             @Assisted("major") final Course major,
                             @Assisted("minor") @Nullable final Course minor,
                             @Assisted final SolverTask<FeasibilityResult> solverTask) {
    this.delayedStore = delayedStore;
    this.resources = ResourceBundle.getBundle("lang.tasks");
    this.major = major;
    this.minor = minor;
    this.solverTask = solverTask;
  }

  @Override
  protected Path call() throws Exception {
    updateTitle(this.buildTitle());

    updateMessage(resources.getString("submit"));
    updateProgress(20, 100);

    if (this.isCancelled()) {
      return null;
    }

    solverTask.setOnRunning(event -> this.updateMessage(resources.getString("running")));
    EXECUTOR_SERVICE.submit(solverTask);

    updateMessage(resources.getString("waiting"));
    updateProgress(40, 100);

    runTask();

    if (this.isCancelled() || solverTask.isCancelled()) {
      this.cancel();
      return null;
    }

    // we have to read from the task here, the future does not provide a result.
    final FeasibilityResult result = solverTask.get();

    return renderPdf(result);
  }

  private String buildTitle() {
    String names = major.getKey();
    if (this.minor != null) {
      names += ", " + minor.getKey();
    }
    return String.format(resources.getString("rendering"), names);
  }

  private void runTask() throws InterruptedException {
    int percentage = 0;
    while (!solverTask.isDone()) {
      percentage = (percentage + 1) % 20 + 40;
      updateProgress(percentage, 100);

      if (solverTask.isCancelled() || this.isCancelled()) {
        updateMessage(resources.getString("cancelled"));
        break;
      }

      try {
        TimeUnit.MILLISECONDS.sleep(200);
      } catch (final InterruptedException exception) {

        logger.info("Task interrupted during sleep", exception);

        if (solverTask.isCancelled() || this.isCancelled()) {
          throw exception;
        }
      }
    }
  }

  private Path renderPdf(final FeasibilityResult result)
      throws IOException, ParserConfigurationException, SAXException {
    final Store store = delayedStore.get();

    updateMessage(resources.getString("render"));
    updateProgress(60, 100);

    final Renderer renderer = getRenderer(store, result);

    updateProgress(80, 100);

    final File tmp = getTempFile(renderer);

    updateMessage(resources.getString("finished"));
    updateProgress(100, 100);

    return Paths.get(tmp.getAbsolutePath());
  }

  private Renderer getRenderer(final Store store, final FeasibilityResult result) {
    try {
      return new Renderer(store, result, major, minor);
    } catch (final NullPointerException exc) {
      logger.error("Exception rendering PDF", exc);
      throw exc;
    }
  }

  @Override
  protected void cancelled() {
    super.cancelled();
    if (solverTask != null) {
      Platform.runLater(() -> solverTask.cancel(true));
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
   */
  private File getTempFile(final Renderer renderer)
      throws IOException, ParserConfigurationException, SAXException {

    final File temp = File.createTempFile("timetable", ".pdf");
    temp.deleteOnExit();
    try (OutputStream out = new FileOutputStream(temp)) {
      renderer.getResult().writeTo(out);
    } catch (final IOException | ParserConfigurationException | SAXException exc) {
      logger.error("Exception rendering PDF", exc);
      throw exc;
    }
    return temp;
  }

  public Course getMinor() {
    return minor;
  }

  public Course getMajor() {
    return major;
  }
}
