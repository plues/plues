package de.hhu.stups.plues.tasks;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.injector.Timeout;
import de.hhu.stups.plues.prob.Solver;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.ui.components.ExceptionDialog;
import javafx.application.Platform;

import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

public class SolverLoaderImpl implements SolverLoader {
  private final Delayed<SolverService> delayedSolverService;
  private final SolverLoaderTaskFactory solverLoaderTaskFactory;
  private final ExecutorService executor;
  private final int timeout;

  @Inject
  SolverLoaderImpl(final Delayed<SolverService> delayedSolverService,
                   @Timeout final int timeout,
                   final SolverLoaderTaskFactory solverLoaderTaskFactory,
                   final ExecutorService executorService) {

    this.delayedSolverService = delayedSolverService;
    this.solverLoaderTaskFactory = solverLoaderTaskFactory;
    this.executor = executorService;
    this.timeout = timeout;
  }

  private SolverLoaderTask getSolverLoaderTask(final Store store) {

    final SolverLoaderTask solverLoader
        = this.solverLoaderTaskFactory.create(store);

    solverLoader.setOnSucceeded(event -> {
      final Solver s = (Solver) event.getSource().getValue();
      Platform.runLater(() -> this.delayedSolverService.set(new SolverService(s, this.timeout)));
    });
    //
    solverLoader.setOnFailed(event -> Platform.runLater(() -> {
      final Throwable ex = event.getSource().getException();
      final ExceptionDialog ed = new ExceptionDialog();

      final ResourceBundle resources = ResourceBundle.getBundle("lang.tasks");

      ed.setTitle(resources.getString("edTitle"));
      ed.setHeaderText(resources.getString("edHeader"));
      ed.setException(ex);

      ed.showAndWait();
      Platform.exit();
    }));

    return solverLoader;
  }

  @Override
  public void load(final Store store) {
    this.executor.submit(this.getSolverLoaderTask(store));
  }
}
