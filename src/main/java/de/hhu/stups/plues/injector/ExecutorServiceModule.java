package de.hhu.stups.plues.injector;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import de.hhu.stups.plues.tasks.ObservableListeningExecutorService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ExecutorServiceModule extends AbstractModule {

  private final ObservableListeningExecutorService probExecutor;
  private final ObservableListeningExecutorService executorPool;

  ExecutorServiceModule() {
    this.probExecutor = new ObservableListeningExecutorService(
      MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()));
    this.executorPool = new ObservableListeningExecutorService(
      MoreExecutors.listeningDecorator(Executors.newWorkStealingPool()));

  }

  @Override
  protected void configure() {
    bind(ExecutorService.class).annotatedWith(Names.named("prob"))
      .toInstance(probExecutor);
    bind(ObservableListeningExecutorService.class).annotatedWith(Names.named("prob"))
      .toInstance(probExecutor);
    bind(ListeningExecutorService.class).annotatedWith(Names.named("prob"))
      .toInstance(probExecutor);

    bind(ExecutorService.class).toInstance(executorPool);
    bind(ObservableListeningExecutorService.class).toInstance(executorPool);
    bind(ListeningExecutorService.class).toInstance(executorPool);

  }

}
