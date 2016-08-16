package de.hhu.stups.plues;

import java.util.Collection;
import java.util.List;
import java.util.Observable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ObservableExecutorService extends Observable implements ExecutorService {
  private final ExecutorService excecutorService;

  public ObservableExecutorService(ExecutorService service) {
    this.excecutorService = service;
  }

  @Override
  public void shutdown() {
    excecutorService.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow() {
    return excecutorService.shutdownNow();
  }

  @Override
  public boolean isShutdown() {
    return excecutorService.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return excecutorService.isTerminated();
  }

  @Override
  public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
    return excecutorService.awaitTermination(timeout, unit);
  }

  @Override
  public <T> Future<T> submit(final Callable<T> task) {
    this.setChanged();
    this.notifyObservers(task);
    return excecutorService.submit(task);
  }

  @Override
  public <T> Future<T> submit(final Runnable task, final T result) {
    this.setChanged();
    this.notifyObservers(task);
    return excecutorService.submit(task, result);
  }

  @Override
  public Future<?> submit(final Runnable task) {
    this.setChanged();
    this.notifyObservers(task);
    return excecutorService.submit(task);
  }

  @Override
  public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
    this.setChanged();
    this.notifyObservers(tasks);
    return excecutorService.invokeAll(tasks);
  }

  @Override
  public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException {
    this.setChanged();
    this.notifyObservers(tasks);
    return excecutorService.invokeAll(tasks, timeout, unit);
  }

  @Override
  public <T> T invokeAny(final Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    this.setChanged();
    this.notifyObservers(tasks);
    return excecutorService.invokeAny(tasks);
  }

  @Override
  public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    this.setChanged();
    this.notifyObservers(tasks);
    return excecutorService.invokeAny(tasks, timeout, unit);
  }

  @Override
  public void execute(final Runnable command) {
    this.setChanged();
    this.notifyObservers(command);
    excecutorService.execute(command);
  }
}
