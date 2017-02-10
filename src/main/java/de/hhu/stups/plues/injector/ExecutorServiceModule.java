package de.hhu.stups.plues.injector;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.AbstractModule;

import de.hhu.stups.plues.tasks.ObservableListeningExecutorService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ExecutorServiceModule extends AbstractModule {

  private final ObservableListeningExecutorService executorPool;

  ExecutorServiceModule() {
    final int parallelism = Math.max(Runtime.getRuntime().availableProcessors(), 4);
    this.executorPool = new ObservableListeningExecutorService(
      MoreExecutors.listeningDecorator(Executors.newWorkStealingPool(parallelism)));

  }

  @Override
  protected void configure() {
    bind(ExecutorService.class).toInstance(executorPool);
    bind(ObservableListeningExecutorService.class).toInstance(executorPool);
    bind(ListeningExecutorService.class).toInstance(executorPool);
  }

}
