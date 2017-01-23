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
import de.hhu.stups.plues.ui.components.ExceptionDialog;
import de.prob.exception.CliError;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.hibernate.annotations.common.util.impl.LoggerFactory;
import org.jboss.logging.Logger;

import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;


public class SolverTask<T> extends Task<T> {

  private static final ListeningExecutorService EXECUTOR_SERVICE;
  private static final ListeningScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE;

  static {
    final ThreadFactory threadFactoryBuilder
        = new ThreadFactoryBuilder().setDaemon(true)
        .setNameFormat("solver-task-runner-%d").build();

    EXECUTOR_SERVICE = MoreExecutors.listeningDecorator(
      Executors.newSingleThreadExecutor(threadFactoryBuilder));
    SCHEDULED_EXECUTOR_SERVICE = MoreExecutors.listeningDecorator(
      Executors.newSingleThreadScheduledExecutor(threadFactoryBuilder));

  }

  private final Logger logger = LoggerFactory.logger(getClass());
  private final Callable<T> function;
  private final TimeUnit timeUnit;
  private final int timeout;
  private final ResourceBundle resources;
  private final Solver solver;

  private ListenableFuture<T> future;
  private ListenableScheduledFuture<?> timer;
  private String reason;

  public SolverTask(final String title, final Solver solver,
                    final Callable<T> func, final Integer timeout) {
    this(title, solver, func, timeout, TimeUnit.SECONDS);
  }

  SolverTask(final String title, final Solver solver,
             final Callable<T> func, final int timeout, final TimeUnit timeUnit) {

    this.function = timedCallableWrapper(title, func);
    this.solver = solver;
    this.timeout = timeout;
    this.timeUnit = timeUnit;

    this.resources = ResourceBundle.getBundle("lang.tasks");
    this.reason = resources.getString("cancelled");

    updateProgress(0, 100);
    updateTitle(title);
    updateMessage(resources.getString("waitingForExecution"));
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
    this.updateMessage(resources.getString("waiting"));
    synchronized (SolverTask.class) {
      if (this.isCancelled()) {
        logger.info("cancelled");
        return null;
      }

      timer = SCHEDULED_EXECUTOR_SERVICE.schedule(this::timeOut, this.timeout, this.timeUnit);
      future = EXECUTOR_SERVICE.submit(function);

      Futures.addCallback(future, new TaskCallback<>());
      updateProgress(10, 100);

      int percentage = 10;

      updateMessage(resources.getString("waitingForResult"));

      while (!future.isDone()) {
        percentage = (percentage + 2) % 90;
        updateProgress(percentage, 100);
        if (this.isCancelled()) {
          logger.info("cancelled");
          return null;
        }
        if (future.isCancelled()) {
          logger.info("future cancelled");
          updateMessage(resources.getString("probExit"));
          this.cancel();
          return null;
        }

        sleep();
      }
    }
    updateMessage(resources.getString("done"));
    updateProgress(100, 100);

    return future.get();
  }

  private void sleep() throws InterruptedException {
    try {
      TimeUnit.MILLISECONDS.sleep(100);
    } catch (final InterruptedException interrupted) {
      if (isCancelled()) {
        logger.info("Task cancelled while sleeping " + this.toString());
        throw interrupted;
      }
    }
  }

  private void timeOut() {
    logger.info("Timeout");
    this.reason = resources.getString("timeout");
    this.cancel();
  }

  @Override
  protected void cancelled() {
    logger.info("Cancelled handler");
    super.cancelled();

    logger.info(this.reason);
    updateMessage(this.reason);

    if (timer != null) {
      timer.cancel(true);
    }
    if (future != null) {
      if (!future.isDone()) {
        solver.interrupt();
      }
      future.cancel(true);
    }
  }

  @Override
  protected void succeeded() {
    logger.info("succeeded handler");
    super.succeeded();

    updateMessage(resources.getString("finished"));
    final T i = this.getValue();
    logger.info("Result: " + (i != null ? i.toString() : " null"));
  }

  @Override
  protected void failed() {
    updateMessage(resources.getString("failed"));

    if (timer != null) {
      timer.cancel(true);
    }
    if (future != null) {
      if (!future.isDone()) {
        solver.interrupt();
      }
      future.cancel(true);
    }

    // Check if the cause of the failure was a CliError. In that case we show an exception dialog
    // and close the application.
    // TODO: show a save dialog
    // TODO: handle case if many solver tasks fail, e.g. for a batch task.
    if (this.getException() != null
        && this.getException().getCause() != null
        && this.getException().getCause() instanceof CliError) {

      Platform.runLater(() -> {
        final ExceptionDialog ed = new ExceptionDialog();
        ed.setTitle(resources.getString("edTitle"));
        ed.setHeaderText(resources.getString("cliError"));
        ed.setException(this.getException());

        logger.fatal("Fatal CliError", this.getException());
        ed.showAndWait();
        Platform.exit();
      });
    } else {
      logger.error("failed handler", this.getException());
    }
  }

  public String getReason() {
    return reason;
  }

  private class TaskCallback<J> implements FutureCallback<J> {
    @Override
    public void onSuccess(@Nullable final J result) {
      timer.cancel(true);
    }

    @Override
    public void onFailure(final Throwable throwable) {
      timer.cancel(true);
    }
  }
}
