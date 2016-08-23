package de.hhu.stups.plues.tasks;

import java.util.Collection;
import java.util.List;
import java.util.Observable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A wrapper class for ExecutorService instances that is observable. Observers get notified whenever
 * a new work item is submitted.
 */
public class ObservableExecutorService extends Observable implements ExecutorService {
  private final ExecutorService executorService;

  public ObservableExecutorService(final ExecutorService service) {
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
  public <T> Future<T> submit(final Callable<T> task) {
    this.setChanged();
    this.notifyObservers(task);
    return executorService.submit(task);
  }

  @Override
  public <T> Future<T> submit(final Runnable task, final T result) {
    this.setChanged();
    this.notifyObservers(task);
    return executorService.submit(task, result);
  }

  @Override
  public Future<?> submit(final Runnable task) {
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
    this.setChanged();
    this.notifyObservers(tasks);
    return executorService.invokeAny(tasks, timeout, unit);
  }

  @Override
  public void execute(final Runnable command) {
    this.setChanged();
    this.notifyObservers(command);
    executorService.execute(command);
  }
}
