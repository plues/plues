package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.modelgenerator.XmlExporter;
import de.hhu.stups.plues.prob.Solver;
import de.hhu.stups.plues.tasks.ObservableListeningExecutorService;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.hhu.stups.plues.tasks.SolverLoaderTask;
import de.hhu.stups.plues.tasks.SolverLoaderTaskFactory;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.tasks.SolverServiceFactory;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.tasks.StoreLoaderTask;
import de.hhu.stups.plues.ui.components.ExceptionDialog;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.controlsfx.control.TaskProgressView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;


@Singleton
public class MainController implements Initializable {

  private static final Map<Class, FontAwesomeIcon> iconMap = new HashMap<>();
  private static final FontAwesomeIcon DEFAULT_ICON = FontAwesomeIcon.TASKS;
  private static final String LAST_DIR = "LAST_DIR";

  static {
    iconMap.put(StoreLoaderTask.class, FontAwesomeIcon.DATABASE);
    iconMap.put(SolverLoaderTask.class, FontAwesomeIcon.COGS);
    iconMap.put(SolverTask.class, FontAwesomeIcon.CALENDAR);
    iconMap.put(PdfRenderingTask.class, FontAwesomeIcon.FILE_PDF_ALT);
  }

  private final Logger logger = Logger.getLogger(getClass().getName());
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
  private MenuItem exportStateMenuItem;

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
                        @Named("prob") final ObservableListeningExecutorService probExecutor,
                        final ObservableListeningExecutorService executorService) {
    this.delayedStore = delayedStore;
    this.delayedSolverService = delayedSolverService;
    this.solverLoaderTaskFactory = solverLoaderTaskFactory;
    this.solverServiceFactory = solverServiceFactory;
    this.properties = properties;
    this.stage = stage;
    this.executor = executorService;

    probExecutor.addObserver((observable, arg) -> this.register(arg));
    executorService.addObserver((observable, arg) -> this.register(arg));

    logger.log(Level.INFO, "Starting PlÜS Version: " + properties.get("version"));
  }

  private void register(final Object task) {
    if (task instanceof Task<?>) {
      logger.log(Level.FINE, "registering task for taskview");
      Platform.runLater(() -> this.taskProgress.getTasks().add((Task<?>) task));
    } else {
      logger.log(Level.FINE, "ignoring task for taskview");
    }
  }

  private Node getGraphicForTask(final Task<?> task) {
    final FontAwesomeIcon icon = iconMap.getOrDefault(task.getClass(), DEFAULT_ICON);
    return FontAwesomeIconFactory.get().createIcon(icon, "2em");
  }

  @Override
  public final void initialize(final URL location,
                               final ResourceBundle resources) {

    this.taskProgress.setGraphicFactory(this::getGraphicForTask);
    this.exportStateMenuItem.setDisable(true);

    delayedStore.whenAvailable(s -> this.exportStateMenuItem.setDisable(false));

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

  /**
   * The menu item's action to export the current state of the database to a zip file
   * containing the xml files.
   */
  @FXML
  public final void exportCurrentDbState() {
    // TODO: should we have a modal progress window to avoid confusion, since the export takes
    // a few instants to finish
    // TODO: consider generating the file to a temporary location and moving it to the final
    // location after the generation finished successfully.
    final DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
    final String dateTime = dateFormat.format(new Date());

    final FileChooser fileChooser = new FileChooser();
    fileChooser.setInitialFileName("plues_xml_database_" + dateTime + ".zip");
    fileChooser.setTitle("Choose the zip file's location");

    final File selectedFile = fileChooser.showSaveDialog(null);

    if (selectedFile != null) {
      executor.execute(new Task<Void>() {

        @Override
        protected Void call() throws Exception {

          updateTitle("Exporting XML");
          updateProgress(1, 3);
          updateMessage("Generating .zip file");

          try (ByteArrayOutputStream exportXmlStream = new XmlExporter(delayedStore.get()).export();
               OutputStream outputStream = new FileOutputStream(selectedFile)) {
            updateProgress(2, 3);

            updateMessage("Writing .zip file");
            exportXmlStream.writeTo(outputStream);
            updateProgress(3, 3);
            logger.info("Wrote xml export to " + selectedFile.getAbsolutePath());

          } catch (final IOException exception) {
            showCriticalExceptionDialog(exception, "XML Export Failed");
          }
          return null;
        }
      });
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
        (observable, oldValue, newValue) -> logger.log(Level.FINE, "STORE progress " + newValue));
    //
    storeLoader.messageProperty().addListener(
        (observable, oldValue, newValue) -> logger.log(Level.FINE, "STORE message " + newValue));
    //
    storeLoader.setOnFailed(event -> {
      final Throwable ex = event.getSource().getException();
      logger.log(Level.SEVERE, "Database could not be loaded");
      showCriticalExceptionDialog(ex, "Database could not be loaded");
      Platform.exit();
    });
    //
    storeLoader.setOnSucceeded(
        value -> logger.log(Level.FINE, "STORE: loading Store succeeded"));

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
    exec.submit(task);
  }

  public void submitTask(final Task<?> task) {
    this.submitTask(task, this.executor);
  }
}
