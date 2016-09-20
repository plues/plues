package de.hhu.stups.plues.ui.batchgeneration;

import de.hhu.stups.plues.tasks.PdfRenderingTask;
import javafx.concurrent.Task;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BatchPdfRenderingTask extends Task<Collection<PdfRenderingTask>> {
  private final Collection<PdfRenderingTask> tasks;
  private final ExecutorService executor;

  public BatchPdfRenderingTask(final ExecutorService executorService,
                               final Collection<PdfRenderingTask> tasks) {
    this.executor = executorService;
    this.tasks = tasks;
  }

  @Override
  protected Collection<PdfRenderingTask> call() throws Exception {
    updateTitle("Generating all timetables");
    final List<Future<?>> futurePool
        = tasks.stream().map(executor::submit).collect(Collectors.toList());

    boolean workLeft;
    final int totalTasks = futurePool.size();

    do {
      final long finishedTasks = futurePool.stream().filter(Future::isDone).count();
      updateProgress(finishedTasks, totalTasks);

      if (isCancelled()) {
        updateMessage("Cancelled");
        break;
      }
      workLeft = !(totalTasks == finishedTasks);

      sleep();
    }
    while (workLeft);

    return tasks;
  }

  private void sleep() throws InterruptedException {
    // to check the interrupted exception for cancellation!
    try {
      TimeUnit.MILLISECONDS.sleep(250);
    } catch (final InterruptedException interrupted) {
      if (isCancelled()) {
        updateMessage("Cancelled");
        throw interrupted;
      }
    }
  }

  @Override
  protected void cancelled() {
    this.tasks.forEach(Task::cancel);
  }
}
