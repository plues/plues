package de.hhu.stups.plues.ui.batchgeneration;

import com.google.inject.Inject;

import de.hhu.stups.plues.tasks.SolverTask;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BatchFeasibilityTask extends Task<Collection<SolverTask<Boolean>>> {
  private final ExecutorService executor;
  private final ResourceBundle resources;
  private Collection<SolverTask<Boolean>> tasks = Collections.emptyList();
  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Constructor, create a new Task to manage batch execution of SolverTasks on a given executor.
   * @param executor ExecutorService
   */
  @Inject
  public BatchFeasibilityTask(final ExecutorService executor) {
    this.executor = executor;
    this.resources = ResourceBundle.getBundle("lang.conflictMatrix");

    updateTitle(resources.getString("checkAllMsg"));
    updateProgress(0, 100);
  }

  @Override
  protected Collection<SolverTask<Boolean>> call() throws Exception {
    updateMessage(resources.getString("waitingForResults"));
    final List<Future<?>> futurePool
        = tasks.stream().map(executor::submit).collect(Collectors.toList());

    final long totalTasks = futurePool.size();

    long finishedTasks;
    do {
      if (isCancelled()) {
        updateMessage(resources.getString("cancelAllMsg"));
        break;
      }

      finishedTasks = futurePool.stream().filter(Future::isDone).count();
      updateProgress(finishedTasks, totalTasks);

      try {
        TimeUnit.MILLISECONDS.sleep(500);
      } catch (final InterruptedException exception) {
        logger.error("BatchFeasibilityTask interrupted during sleep", exception);
        throw exception;
      }

    }
    while (totalTasks != finishedTasks);

    return tasks;
  }

  public void setTasks(final Collection<SolverTask<Boolean>> tasks) {
    this.tasks = tasks;
  }

  @Override
  protected void cancelled() {
    this.tasks.forEach(Task::cancel);
  }

  @Override
  protected void failed() {
    super.failed();
    logger.error("failed", this.getException());
  }
}
