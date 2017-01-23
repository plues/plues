package de.hhu.stups.plues.ui.batchgeneration;

import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;
import javafx.concurrent.Task;

import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class BatchFeasibilityTask extends Task<Collection<SolverTask<Boolean>>> {
  private final ExecutorService executor;
  private final ResourceBundle resources;
  private Collection<SolverTask<Boolean>> tasks;

  /**
   * Constructor, create a new Task to manage batch execution of SolverTasks on a given executor.
   * @param executor ExecutorService
   * @param tasks Collection of SolverTask objects.
   */
  public BatchFeasibilityTask(final ExecutorService executor,
                              final Collection<SolverTask<Boolean>> tasks) {
    this.executor = executor;
    this.tasks = tasks;
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
      finishedTasks = futurePool.stream().filter(Future::isDone).count();
      updateProgress(finishedTasks, totalTasks);

      if (isCancelled()) {
        updateMessage(resources.getString("cancelAllMsg"));
        break;
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
}
