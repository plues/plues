package de.hhu.stups.plues.tasks;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.reactfx.EventSource;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A wrapper class for ListeningExecutorService instances that is observable. Observers get notified
 * whenever a new work item is submitted.
 */
public class ObservableListeningExecutorService implements ListeningExecutorService {

  private final ListeningExecutorService executorService;
  private final EventSource<Object> tasks;

  public ObservableListeningExecutorService(final ListeningExecutorService service) {
    this.executorService = service;
    this.tasks = new EventSource<>();
  }

  @Override
  public void shutdown() {
    executorService.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow() {
    return executorService.shutdownNow();
  }

  @Override
  public boolean isShutdown() {
    return executorService.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return executorService.isTerminated();
  }

  @Override
  public boolean awaitTermination(final long timeout,
                                  final TimeUnit unit) throws InterruptedException {
    return executorService.awaitTermination(timeout, unit);
  }

  @Override
  public <T> ListenableFuture<T> submit(final Callable<T> task) {
    this.tasks.push(task);
    return executorService.submit(task);
  }

  @SuppressFBWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
  @Override
  public <T> ListenableFuture<T> submit(final Runnable task, final T result) {
    this.tasks.push(task);
    return executorService.submit(task, result);
  }

  @Override
  public ListenableFuture<?> submit(final Runnable task) {
    this.tasks.push(task);
    return executorService.submit(task);
  }

  @Override
  public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks)
      throws InterruptedException {
    tasks.forEach(this.tasks::push);
    return executorService.invokeAll(tasks);
  }

  @Override
  public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks,
                                       final long timeout, final TimeUnit unit)
      throws InterruptedException {
    tasks.forEach(this.tasks::push);
    return executorService.invokeAll(tasks, timeout, unit);
  }

  @Override
  public <T> T invokeAny(final Collection<? extends Callable<T>> tasks)
      throws InterruptedException, ExecutionException {
    tasks.forEach(this.tasks::push);
    return executorService.invokeAny(tasks);
  }

  @Override
  public <T> T invokeAny(final Collection<? extends Callable<T>> tasks,
                         final long timeout, final TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return executorService.invokeAny(tasks, timeout, unit);
  }

  @Override
  public void execute(final Runnable command) {
    if (command == null) {
      return;
    }
    this.tasks.push(command);
    executorService.execute(command);
  }

  public EventSource<Object> getTasks() {
    return tasks;
  }
}
