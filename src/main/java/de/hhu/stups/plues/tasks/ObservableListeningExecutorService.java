package de.hhu.stups.plues.tasks;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Collection;
import java.util.List;
import java.util.Observable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A wrapper class for ListeningExecutorService instances that is observable. Observers get notified
 * whenever a new work item is submitted.
 */
public class ObservableListeningExecutorService extends Observable
    implements ListeningExecutorService {

  private final ListeningExecutorService executorService;

  public ObservableListeningExecutorService(final ListeningExecutorService service) {
    this.executorService = service;
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
    this.setChanged();
    this.notifyObservers(task);
    return executorService.submit(task);
  }

  @SuppressFBWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
  @Override
  public <T> ListenableFuture<T> submit(final Runnable task, final T result) {
    this.setChanged();
    this.notifyObservers(task);
    return executorService.submit(task, result);
  }

  @Override
  public ListenableFuture<?> submit(final Runnable task) {
    this.setChanged();
    this.notifyObservers(task);
    return executorService.submit(task);
  }

  @Override
  public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks)
      throws InterruptedException {
    this.setChanged();
    this.notifyObservers(tasks);
    return executorService.invokeAll(tasks);
  }

  @Override
  public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks,
                                       final long timeout, final TimeUnit unit)
      throws InterruptedException {
    this.setChanged();
    this.notifyObservers(tasks);
    return executorService.invokeAll(tasks, timeout, unit);
  }

  @Override
  public <T> T invokeAny(final Collection<? extends Callable<T>> tasks)
      throws InterruptedException, ExecutionException {
    this.setChanged();
    this.notifyObservers(tasks);
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
    this.setChanged();
    this.notifyObservers(command);
    executorService.execute(command);
  }
}
