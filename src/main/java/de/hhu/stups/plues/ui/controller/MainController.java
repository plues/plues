package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.prob.Solver;
import de.hhu.stups.plues.tasks.SolverLoaderTask;
import de.hhu.stups.plues.tasks.SolverLoaderTaskFactory;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.tasks.SolverServiceFactory;
import de.hhu.stups.plues.tasks.StoreLoaderTask;
import de.hhu.stups.plues.ui.components.ExceptionDialog;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.controlsfx.control.TaskProgressView;

import java.io.File;
import java.net.URL;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;

@Singleton
public class MainController implements Initializable {

  private static final String LAST_DIR = "LAST_DIR";

  private final Delayed<Store> delayedStore;
  private final Delayed<SolverService> delayedSolverService;

  private final SolverLoaderTaskFactory solverLoaderTaskFactory;
  private final SolverServiceFactory solverServiceFactory;

  private final Properties properties;
  private final Stage stage;

  private final ExecutorService executor;

  @FXML
  private MenuItem openFileMenuItem;
  @FXML
  private TaskProgressView<Task<?>> taskProgress;

  /**
   * MainController component.
   */
  @Inject
  public MainController(final Delayed<Store> delayedStore,
                        final Delayed<SolverService> delayedSolverService,
                        final SolverLoaderTaskFactory solverLoaderTaskFactory,
                        final SolverServiceFactory solverServiceFactory,
                        final Properties properties,
                        final Stage stage,
                        final ExecutorService executorService) {
    this.delayedStore = delayedStore;
    this.delayedSolverService = delayedSolverService;
    this.solverLoaderTaskFactory = solverLoaderTaskFactory;
    this.solverServiceFactory = solverServiceFactory;
    this.properties = properties;
    this.stage = stage;
    this.executor = executorService;
  }

  @Override
  public final void initialize(final URL location,
                               final ResourceBundle resources) {
    if (this.properties.get("dbpath") != null) {
      this.loadData((String) this.properties.get("dbpath"));
    }
  }

  /**
   * Opens a file.
   */
  @SuppressWarnings("UnusedParameters")
  public final void openFile(final ActionEvent actionEvent) {
    final Preferences prefs
        = Preferences.userNodeForPackage(MainController.class);
    final String initialDir
        = prefs.get(LAST_DIR, System.getProperty("user.home"));
    //
    final FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Open a Database"); // TODO: i18n

    fileChooser.setInitialDirectory(new File(initialDir));
    fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter(
        "SQLite3 Database", "*.sqlite", "*.sqlite3"));
    //
    final File file = fileChooser.showOpenDialog(stage);
    //
    if (file != null) {
      final String newInitialDir = file.getAbsoluteFile().getParent();
      if (!newInitialDir.equals(initialDir)) {
        prefs.put(LAST_DIR, newInitialDir);
      }
      //
      this.loadData(file.getAbsolutePath());
    }
  }

  private void loadData(final String path) {

    final StoreLoaderTask storeLoader = this.getStoreLoaderTask(path);
    final SolverLoaderTask solverLoader = this.getSolverLoaderTask(storeLoader);

    this.openFileMenuItem.setDisable(true);
    this.submitTask(storeLoader);

    this.submitTask(solverLoader);
  }

  private StoreLoaderTask getStoreLoaderTask(final String path) {

    final StoreLoaderTask storeLoader = new StoreLoaderTask(path);
    //
    storeLoader.progressProperty().addListener(
        (observable, oldValue, newValue) -> System.out.println("STORE " + newValue));
    //
    storeLoader.messageProperty().addListener(
        (observable, oldValue, newValue) -> System.out.println("STORE " + newValue));
    //
    storeLoader.setOnFailed(event -> {
      final Throwable ex = event.getSource().getException();
      showCriticalExceptionDialog(ex, "Database could not be loaded");
      Platform.exit();
    });
    //
    storeLoader.setOnSucceeded(
        value -> System.out.println("STORE:loading Store succeeded"));

    storeLoader.setOnSucceeded(event -> Platform.runLater(() -> {
      final Store s = (Store) event.getSource().getValue();
      this.delayedStore.set(s);
    }));
    return storeLoader;
  }

  private SolverLoaderTask getSolverLoaderTask(
      final StoreLoaderTask storeLoader) {

    final SolverLoaderTask solverLoader
        = this.solverLoaderTaskFactory.create(storeLoader);

    solverLoader.progressProperty().addListener(
        (observable, oldValue, newValue) -> System.out.println(newValue));

    solverLoader.messageProperty().addListener(
        (observable, oldValue, newValue) -> System.out.println(newValue));

    solverLoader.setOnSucceeded(
        value -> System.out.println("loading Solver succeeded"));
    solverLoader.setOnSucceeded(event -> {
      final Solver s = (Solver) event.getSource().getValue();
      // TODO: check if this needs to run on UI thread
      this.delayedSolverService.set(solverServiceFactory.create(s));
    });
    //
    solverLoader.setOnFailed(event -> {
      final Throwable ex = event.getSource().getException();
      showCriticalExceptionDialog(ex, "Solver could not be loaded");
      Platform.exit();
    });

    return solverLoader;
  }

  private void showCriticalExceptionDialog(final Throwable ex, final String message) {
    final ExceptionDialog ed = new ExceptionDialog();

    ed.setTitle("Critical Exception");
    ed.setHeaderText(message);
    ed.setException(ex);

    ed.showAndWait();
  }

  private void submitTask(final Task<?> task, final ExecutorService exec) {
    this.taskProgress.getTasks().add(task);
    exec.submit(task);
  }

  public void submitTask(final Task<?> task) {
    this.submitTask(task, this.executor);
  }
}
