package de.hhu.stups.plues.ui;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.provider.RouterProvider;
import de.hhu.stups.plues.routes.RouteNames;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ResourceManager {
  private final Delayed<Store> delayedStore;
  private final ExecutorService executorService;

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final Stage stage;

  /**
   * ResourceManager class used to manage resources that need to be closed when shutting down the
   * application.
   *
   * @param delayedStore    Delayed store
   * @param executorService ExecutorService
   */
  @Inject
  public ResourceManager(final Delayed<Store> delayedStore,
                         final ListeningExecutorService executorService,
                         final Stage stage,
                         final RouterProvider router) {
    this.delayedStore = delayedStore;
    this.executorService = executorService;
    this.stage = stage;

    router.get().register(RouteNames.SHUTDOWN, (routeName, args) -> {
      try {
        this.close();
      } catch (final InterruptedException exception) {
        logger.error("Closing resources", exception);
        Thread.currentThread().interrupt();
      }
    });
  }

  /**
   * Close all managed resources.
   *
   * @throws InterruptedException thrown if any of the executors throws it.
   */
  private void close() throws InterruptedException {
    stage.close();
    logger.info("Main stage closed");

    delayedStore.whenAvailable(Store::close);
    logger.info("Store closed");

    executorService.shutdown();
    logger.info("shutdown");

    executorService.awaitTermination(10, TimeUnit.SECONDS);
    logger.info("waited for termination");

    executorService.shutdownNow();
    logger.info("killed");
  }
}
