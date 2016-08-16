package de.hhu.stups.plues.injector;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import de.hhu.stups.plues.tasks.ObservableExecutorService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ExecutorServiceModule extends AbstractModule {

  private final ObservableExecutorService probExecutor;
  private final ObservableExecutorService executorPool;

  ExecutorServiceModule() {
    this.probExecutor = new ObservableExecutorService(Executors.newSingleThreadExecutor());
    this.executorPool = new ObservableExecutorService(Executors.newWorkStealingPool());

  }

  @Override
  protected void configure() {
    bind(ExecutorService.class).annotatedWith(Names.named("prob"))
      .toInstance(probExecutor);
    bind(ObservableExecutorService.class).annotatedWith(Names.named("prob"))
      .toInstance(probExecutor);

    bind(ExecutorService.class).toInstance(executorPool);
    bind(ObservableExecutorService.class).toInstance(executorPool);

  }

}
