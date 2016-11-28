package de.hhu.stups.plues.ui.batchgeneration;

import de.hhu.stups.plues.tasks.PdfRenderingTask;
import javafx.concurrent.Task;

import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BatchPdfRenderingTask extends Task<Collection<PdfRenderingTask>> {
  private final Collection<PdfRenderingTask> tasks;
  private final ExecutorService executor;
  private final ResourceBundle resources;

  /**
   * Constructor to create task for batch pdf rendering.
   * @param executorService Executor service to submit task
   * @param tasks A collection of all single rendering tasks
   */
  public BatchPdfRenderingTask(final ExecutorService executorService,
                               final Collection<PdfRenderingTask> tasks) {
    this.executor = executorService;
    this.tasks = tasks;
    this.resources = ResourceBundle.getBundle("lang.tasks");
  }

  @Override
  protected Collection<PdfRenderingTask> call() throws Exception {
    updateTitle(resources.getString("batchGen"));
    final List<Future<?>> futurePool
        = tasks.stream().map(executor::submit).collect(Collectors.toList());

    final long totalTasks = futurePool.size();

    long finishedTasks;
    do {
      finishedTasks = futurePool.stream().filter(Future::isDone).count();
      updateProgress(finishedTasks, totalTasks);

      if (isCancelled()) {
        updateMessage(resources.getString("cancelled"));
        break;
      }

      sleep();
    }
    while (totalTasks != finishedTasks);

    return tasks;
  }

  private void sleep() throws InterruptedException {
    // to check the interrupted exception for cancellation!
    try {
      TimeUnit.MILLISECONDS.sleep(250);
    } catch (final InterruptedException interrupted) {
      if (isCancelled()) {
        updateMessage(resources.getString("cancelled"));
        throw interrupted;
      }
    }
  }

  @Override
  protected void cancelled() {
    this.tasks.forEach(Task::cancel);
  }
}
