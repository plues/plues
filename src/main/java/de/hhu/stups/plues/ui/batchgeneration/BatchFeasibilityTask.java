package de.hhu.stups.plues.ui.batchgeneration;

import de.hhu.stups.plues.tasks.SolverTask;
import javafx.concurrent.Task;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class BatchFeasibilityTask extends Task<Collection<SolverTask<Boolean>>> {
  private final ExecutorService executor;
  private Collection<SolverTask<Boolean>> tasks;

  public BatchFeasibilityTask(final ExecutorService executor,
                              final Collection<SolverTask<Boolean>> tasks) {
    this.executor = executor;
    this.tasks = tasks;
  }

  @Override
  protected Collection<SolverTask<Boolean>> call() throws Exception {
    updateTitle("Checking all feasibilities");
    final List<Future<?>> futurePool
        = tasks.stream().map(executor::submit).collect(Collectors.toList());

    final long totalTasks = futurePool.size();

    long finishedTasks;
    do {
      finishedTasks = futurePool.stream().filter(Future::isDone).count();
      updateProgress(finishedTasks, totalTasks);

      if (isCancelled()) {
        updateMessage("Cancelled");
        break;
      }

    }
    while (totalTasks != finishedTasks);

    return tasks;
  }

  public void setTasks(Collection<SolverTask<Boolean>> tasks) {
    this.tasks = tasks;
  }

  @Override
  protected void cancelled() {
    this.tasks.forEach(Task::cancel);
  }
}
