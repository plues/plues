package de.hhu.stups.plues.ui;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ResourceManager {
  private final Delayed<Store> store;
  private final ExecutorService executorService;
  private final ExecutorService probExecutor;

  private final Logger logger = Logger.getLogger(getClass().getSimpleName());

  /**
   * ResourceManager class used to manage resources that need to be closed when shutting down the
   * application.
   * @param store Store
   * @param executorService ExecutorService
   * @param probExecutor ExecutorService
   */
  @Inject
  public ResourceManager(final Delayed<Store> store, final ExecutorService executorService,
      @Named("prob") final ExecutorService probExecutor) {
    this.store = store;
    this.executorService = executorService;
    this.probExecutor = probExecutor;
  }

  /**
   * Close all managed resources.
   * @throws InterruptedException thrown if any of the executors throws it.
   */
  public void close() throws InterruptedException {
    final Store store = this.store.get();
    if (store != null) {
      this.store.get().close();
    }
    logger.info("Store closed");

    this.executorService.shutdown();
    this.probExecutor.shutdown();
    logger.info("shutdown");

    this.executorService.awaitTermination(10, TimeUnit.SECONDS);
    this.probExecutor.awaitTermination(3, TimeUnit.SECONDS);
    logger.info("waited for termination");

    this.executorService.shutdownNow();
    this.probExecutor.shutdownNow();
    logger.info("killed");
  }
}
