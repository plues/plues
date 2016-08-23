package de.hhu.stups.plues.tasks;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.hhu.stups.plues.prob.Solver;

import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class SolverTask<T> extends Task<T> {

  private static final ExecutorService EXECUTOR;

  static {
    final ThreadFactory threadFactoryBuilder
        = new ThreadFactoryBuilder().setDaemon(true)
        .setNameFormat("solver-task-runner-%d")
        .build();
    EXECUTOR = Executors.newSingleThreadExecutor(threadFactoryBuilder);
  }

  private final Callable<T> function;
  private final Solver solver;
  private Future<T> future;

  SolverTask(final String title, final String message, final Solver solver,
             final Callable<T> func) {
    this(title, solver, func);

    updateMessage(message);
  }

  private SolverTask(final String title, final Solver solver, final Callable<T> func) {
    this.function = func;
    this.solver = solver;

    updateTitle(title);
  }

  @Override
  protected T call() throws InterruptedException, ExecutionException {
    final int solverTaskTimeout = 5;    // minutes

    updateTitle("Solver task");
    updateProgress(10, 100);
    future = EXECUTOR.submit(function);

    try {
      future.get(solverTaskTimeout, TimeUnit.MINUTES);
    } catch (TimeoutException exception) {
      future.cancel(true);
    }

    int percentage = 10;
    while (!future.isDone()) {
      percentage = (percentage + 5) % 95;
      updateProgress(percentage, 100);
      if (this.isCancelled()) {
        updateMessage("Task canceled");
        return null;
      }
      if (future.isCancelled()) {
        updateMessage("ProB exited");
        return null;
      }
      TimeUnit.MILLISECONDS.sleep(100);
    }
    updateProgress(100, 100);
    return future.get();
  }

  @Override
  protected void cancelled() {
    super.cancelled();
    future.cancel(true);
    System.out.println("Task cancelled.");
    solver.interrupt();
  }

  @Override
  protected void succeeded() {
    super.succeeded();
    updateMessage("Done!");
    final T i = this.getValue();
    System.out.println("Result: " + i.toString());
  }
}
