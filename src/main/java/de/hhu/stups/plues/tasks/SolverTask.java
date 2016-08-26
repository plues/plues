package de.hhu.stups.plues.tasks;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.hhu.stups.plues.prob.Solver;
import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.Nullable;


public class SolverTask<T> extends Task<T> {

  private static final ListeningExecutorService EXECUTOR;
  private static final ListeningScheduledExecutorService TIMER;

  static {
    final ThreadFactory threadFactoryBuilder
        = new ThreadFactoryBuilder().setDaemon(true)
        .setNameFormat("solver-task-runner-%d").build();

    EXECUTOR = MoreExecutors.listeningDecorator(
      Executors.newSingleThreadExecutor(threadFactoryBuilder));
    TIMER = MoreExecutors.listeningDecorator(
      Executors.newSingleThreadScheduledExecutor(threadFactoryBuilder));

  }

  private final Logger logger = Logger.getLogger(getClass().getSimpleName());
  private final Callable<T> function;
  private final Solver solver;
  private ListenableFuture<T> future;
  private ListenableScheduledFuture<?> timer;

  SolverTask(final String title, final String message, final Solver solver,
             final Callable<T> func) {
    this(title, solver, func);

    updateMessage(message);
  }

  private SolverTask(final String title, final Solver solver, final Callable<T> func) {
    this.function = timedCallableWrapper(title, func);
    this.solver = solver;

    updateTitle(title);
  }

  private Callable<T> timedCallableWrapper(final String title, final Callable<T> func) {
    return () -> {
      final long start = System.nanoTime();
      final T result = func.call();
      final long end = System.nanoTime();
      logger.info("SolverTask " + title + " finished in "
          + TimeUnit.NANOSECONDS.toMillis(end - start) + " ms.");
      return result;
    };
  }

  @Override
  protected T call() throws InterruptedException, ExecutionException {
    final int solverTaskTimeout = 5;    // minutes

    updateProgress(10, 100);
    timer = TIMER.schedule(this::timeOut, solverTaskTimeout, TimeUnit.MINUTES);
    future = EXECUTOR.submit(function);

    Futures.addCallback(future, new FutureCallback<T>() {
      @Override
      public void onSuccess(@Nullable final T result) {
        timer.cancel(true);
      }

      @Override
      public void onFailure(final Throwable t) {
        timer.cancel(true);
      }
    });

    int percentage = 10;
    while (!future.isDone()) {
      percentage = (percentage + 2) % 95;
      updateProgress(percentage, 100);
      if (this.isCancelled()) {
        return null;
      }
      if (future.isCancelled()) {
        updateMessage("ProB exited");
        this.cancel();
        return null;
      }

      try {
        TimeUnit.MILLISECONDS.sleep(100);
      } catch (final InterruptedException interrupted) {
        if (isCancelled()) {
          return null;
        }
      }
    }
    updateProgress(100, 100);

    return future.get();
  }

  private void timeOut() {

    logger.info("Task timeout.");
    updateMessage("Task timeout");

    this.cancel();
  }

  @Override
  protected void cancelled() {
    super.cancelled();

    logger.info("Task cancelled.");
    updateMessage("Task cancelled");

    timer.cancel(true);
    future.cancel(true);

    solver.interrupt();
  }

  @Override
  protected void succeeded() {
    super.succeeded();

    updateMessage("Done!");
    final T i = this.getValue();
    logger.info("Result: " + i.toString());
  }
}
