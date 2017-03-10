package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.ObservableStore;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.tasks.StoreLoaderTask;
import de.hhu.stups.plues.tasks.StoreLoaderTaskFactory;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.prefs.Preferences;

@Singleton
public class MainMenuService {

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final Delayed<SolverService> delayedSolverService;
  private final Delayed<ObservableStore> delayedStore;
  private final Stage stage;
  private final BooleanProperty databaseChangedProperty;
  private final BooleanProperty openReportsProperty;
  private final DoubleProperty storeLoaderProgressProperty;
  private final StoreLoaderTaskFactory storeLoaderTaskFactory;
  private final ObjectProperty<Event> closeWindowProperty;

  /**
   * Constructor of the service between the main controller and the menu bar. Mainly just
   * distributing some objects used in both classes.
   */
  @Inject
  public MainMenuService(final Delayed<SolverService> delayedSolverService,
                         final Delayed<ObservableStore> delayedStore,
                         final UiDataService uiDataService,
                         final StoreLoaderTaskFactory storeLoaderTaskFactory,
                         final Stage stage) {
    this.delayedSolverService = delayedSolverService;
    this.delayedStore = delayedStore;
    this.storeLoaderTaskFactory = storeLoaderTaskFactory;
    this.stage = stage;
    databaseChangedProperty = new SimpleBooleanProperty(false);
    openReportsProperty = new SimpleBooleanProperty(false);
    storeLoaderProgressProperty = new SimpleDoubleProperty(0.0);
    closeWindowProperty = new SimpleObjectProperty<>();

    delayedStore.whenAvailable(store ->
        store.addObserver((object, arg) -> databaseChangedProperty.setValue(true)));

    // reset unsaved flag.
    uiDataService.lastSavedDateProperty().addListener(
        (observable, oldValue, newValue) -> databaseChangedProperty.setValue(false));
  }

  /**
   * Create an exception dialog to show a critical exception.
   */
  void showCriticalExceptionDialog(final Throwable ex, final String title, final String message) {
    final ExceptionDialog ed = new ExceptionDialog();

    ed.setTitle(title);
    ed.setHeaderText(message);
    ed.setException(ex);

    ed.showAndWait();
  }

  /**
   * Create and return a {@link StoreLoaderTask}.
   */
  StoreLoaderTask getStoreLoaderTask(final String path, final String title) {
    final StoreLoaderTask storeLoader = storeLoaderTaskFactory.create(path);

    storeLoader.progressProperty().addListener(
        (observable, oldValue, newValue) -> logger.trace("STORE progress " + newValue));

    storeLoader.messageProperty().addListener(
        (observable, oldValue, newValue) -> logger.trace("STORE message " + newValue));

    storeLoader.setOnFailed(event -> {
      final Throwable ex = event.getSource().getException();
      final Throwable cause;
      if (ex.getCause() == null) {
        cause = ex;
      } else {
        cause = ex.getCause();
      }

      logger.error("Database could not be loaded", cause);
      showCriticalExceptionDialog(cause, title, "Database could not be loaded");
      Platform.exit();
    });

    storeLoader.setOnSucceeded(value -> logger.trace("STORE: loading Store succeeded"));
    storeLoader.setOnSucceeded(event -> Platform.runLater(() ->
        setObservableStore((ObservableStore) event.getSource().getValue())));

    storeLoaderProgressProperty.bind(storeLoader.progressProperty());

    return storeLoader;
  }

  /**
   * Prepare a file chooser and return the file.
   *
   * @param title       The file chooser's title.
   * @param preferences The aggrieved class' preferences.
   */
  FileChooser prepareFileChooser(final String title, final Preferences preferences) {
    final FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle(title);
    //
    final String initialDirName = preferences.get("LAST_DB_OPEN_DIR",
        System.getProperty("user.home"));
    final File initialDir = new File(initialDirName);
    if (initialDir.isDirectory()) {
      fileChooser.setInitialDirectory(initialDir);
    }
    //
    fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter(
        "SQLite3 Database", "*.sqlite", "*.sqlite3"));

    return fileChooser;
  }

  public Delayed<SolverService> getDelayedSolverService() {
    return delayedSolverService;
  }

  public Delayed<ObservableStore> getDelayedStore() {
    return delayedStore;
  }

  private void setObservableStore(final ObservableStore observableStore) {
    delayedStore.set(observableStore);
  }

  public DoubleProperty getStoreLoaderProgressProperty() {
    return storeLoaderProgressProperty;
  }

  public boolean isDatabaseChanged() {
    return databaseChangedProperty.get();
  }

  public BooleanProperty getOpenReportsProperty() {
    return openReportsProperty;
  }

  public ObjectProperty<Event> getCloseWindowProperty() {
    return closeWindowProperty;
  }

  public Stage getStage() {
    return stage;
  }
}
