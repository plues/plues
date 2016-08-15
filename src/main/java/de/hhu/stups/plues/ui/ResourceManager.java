package de.hhu.stups.plues.ui;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.injector.ProB;
import de.hhu.stups.plues.tasks.SolverService;

import java.sql.Time;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ResourceManager {
  private final Delayed<Store> store;
  private final ExecutorService executorService;
  private final ExecutorService probExecutor;

  @Inject
  public ResourceManager(Delayed<Store> store, ExecutorService executorService, @ProB final ExecutorService probExecutor) {
    this.store = store;
    this.executorService = executorService;
    this.probExecutor = probExecutor;
  }

  public void close () throws InterruptedException {
    Store store = this.store.get();
    if(store != null) {
      this.store.get().close();
    }
    System.out.println("Store closed");

    this.executorService.shutdown();
    this.probExecutor.shutdown();
    System.out.println("shutdown");

    this.executorService.awaitTermination(10, TimeUnit.SECONDS);
    this.probExecutor.awaitTermination(3, TimeUnit.SECONDS);
    System.out.println("waited for termination");

    this.executorService.shutdownNow();
    this.probExecutor.shutdownNow();
    System.out.println("killed");
  }
}
