package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.ObservableStore;
import de.hhu.stups.plues.modelgenerator.XmlExporter;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.tasks.ObservableListeningExecutorService;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.hhu.stups.plues.tasks.SolverLoaderImpl;
import de.hhu.stups.plues.tasks.SolverLoaderTask;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.tasks.StoreLoaderTask;
import de.hhu.stups.plues.tasks.StoreLoaderTaskFactory;
import de.hhu.stups.plues.ui.ResourceManager;
import de.hhu.stups.plues.ui.components.AboutWindow;
import de.hhu.stups.plues.ui.components.ChangeLog;
import de.hhu.stups.plues.ui.components.ExceptionDialog;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleGroup;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.controlsfx.control.TaskProgressView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
  private static final String LAST_DB_OPEN_DIR = "LAST_DB_OPEN_DIR";
  private static final String LAST_XML_EXPORT_DIR = "LAST_XML_EXPORT_DIR";
  private static final String DB_PATH = "dbpath";
  private static final String TEMP_DB_PATH = "tempDBpath";

  static {
    iconMap.put(StoreLoaderTask.class, FontAwesomeIcon.DATABASE);
    iconMap.put(SolverLoaderTask.class, FontAwesomeIcon.COGS);
    iconMap.put(SolverTask.class, FontAwesomeIcon.CALENDAR);
    iconMap.put(PdfRenderingTask.class, FontAwesomeIcon.FILE_PDF_ALT);
  }

  private final Logger logger = Logger.getLogger(getClass().getName());
  private final Delayed<ObservableStore> delayedStore;
  private final Properties properties;
  private final Stage stage;
  private final ExecutorService executor;
  private final UiDataService uiDataService;
  private final Preferences userPreferences;

  private final Preferences preferences = Preferences.userNodeForPackage(MainController.class);
  private final SolverLoaderImpl solverLoader;
  private final Provider<Reports> reportsProvider;
  private final StoreLoaderTaskFactory storeLoaderTaskFactory;
  private final ChangeLog changeLog;
  private final Provider<AboutWindow> aboutWindowProvider;
  private final ResourceManager resourceManager;
  private SolverService solverService;
  private final ToggleGroup sessionPreferenceToggle = new ToggleGroup();
  private boolean databaseChanged = false;

  private ResourceBundle resources;
  @FXML
  private MenuItem saveFileMenuItem;
  @FXML
  private MenuItem saveFileAsMenuItem;
  @FXML
  private MenuItem openFileMenuItem;
  @FXML
  private MenuItem exportStateMenuItem;
  @FXML
  private MenuItem setTimeoutMenuItem;
  @FXML
  private MenuItem oneMinuteMenuItem;
  @FXML
  private MenuItem threeMinutesMenuItem;
  @FXML
  private MenuItem fiveMinutesMenuItem;
  @FXML
  private MenuItem openChangeLog;
  @FXML
  private MenuItem openReportsMenuItem;
  @FXML
  private RadioMenuItem rbMenuItemSessionName;
  @FXML
  private RadioMenuItem rbMenuItemSessionId;
  @FXML
  private TabPane tabPane;

  @FXML
  private TaskProgressView<Task<?>> taskProgress;
  @FXML
  private RadioMenuItem rbMenuItemSessionKey;

  /**
   * MainController component.
   */
  @Inject
  public MainController(final Delayed<ObservableStore> delayedStore,
                        final Delayed<SolverService> delayedSolverService,
                        final SolverLoaderImpl solverLoader, final Properties properties,
                        final Stage stage,
                        final Provider<ChangeLog> changeLogProvider,
                        final Provider<AboutWindow> aboutWindowProvider,
                        final Provider<Reports> reportsProvider,
                        final StoreLoaderTaskFactory storeLoaderTaskFactory,
                        @Named("prob") final ObservableListeningExecutorService probExecutor,
                        final ObservableListeningExecutorService executorService,
                        final ResourceManager resourceManager,
                        final UiDataService uiDataService) {
    this.delayedStore = delayedStore;
    this.solverLoader = solverLoader;
    this.properties = properties;
    this.stage = stage;
    this.changeLog = changeLogProvider.get();
    this.aboutWindowProvider = aboutWindowProvider;
    this.reportsProvider = reportsProvider;
    this.storeLoaderTaskFactory = storeLoaderTaskFactory;
    this.executor = executorService;
    this.resourceManager = resourceManager;
    this.uiDataService = uiDataService;
    userPreferences = Preferences.userRoot().node("Plues");

    delayedSolverService.whenAvailable(solverService -> {
      this.solverService = solverService;
      openReportsMenuItem.setDisable(false);
      setTimeoutMenuItem.setDisable(false);
      oneMinuteMenuItem.setDisable(false);
      threeMinutesMenuItem.setDisable(false);
      fiveMinutesMenuItem.setDisable(false);
    });

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

  @SuppressWarnings("unused")
  private Node getGraphicForTask(final Task<?> task) {
    final FontAwesomeIcon icon = iconMap.getOrDefault(task.getClass(), DEFAULT_ICON);
    return FontAwesomeIconFactory.get().createIcon(icon, "2em");
  }

  @Override
  public final void initialize(final URL location,
                               final ResourceBundle resources) {
    this.resources = resources;

    this.taskProgress.setGraphicFactory(this::getGraphicForTask);
    this.exportStateMenuItem.setDisable(true);
    this.openChangeLog.setDisable(true);
    this.openReportsMenuItem.setDisable(true);
    this.saveFileMenuItem.setDisable(true);
    this.saveFileAsMenuItem.setDisable(true);
    this.setTimeoutMenuItem.setDisable(true);
    this.oneMinuteMenuItem.setDisable(true);
    this.threeMinutesMenuItem.setDisable(true);
    this.fiveMinutesMenuItem.setDisable(true);

    tabPane.setOnKeyPressed(event -> {
      switch (event.getCode()) {
        case DIGIT1:
          tabPane.getSelectionModel().select(0);
          break;
        case DIGIT2:
          tabPane.getSelectionModel().select(1);
          break;
        case DIGIT3:
          tabPane.getSelectionModel().select(2);
          break;
        case DIGIT4:
          tabPane.getSelectionModel().select(3);
          break;
        case DIGIT5:
          tabPane.getSelectionModel().select(4);
          break;
        case DIGIT6:
          tabPane.getSelectionModel().select(5);
          break;
        default:
          break;
      }
    });

    initializeViewMenuItems();

    delayedStore.whenAvailable(s -> {
      this.exportStateMenuItem.setDisable(false);
      this.openChangeLog.setDisable(false);
      this.saveFileMenuItem.setDisable(false);
      this.saveFileAsMenuItem.setDisable(false);

      // Handle database changes for confirmation dialogue on close -> set unsaved flag
      s.addObserver((object, arg) -> this.databaseChanged = true);
    });

    if (this.properties.get(DB_PATH) != null) {
      this.loadData((String) this.properties.get(DB_PATH));
    }

    stage.setOnCloseRequest(t -> {
      try {
        this.closeWindow(t);
        if (!t.isConsumed()) {
          this.resourceManager.close();
        }
      } catch (final InterruptedException exception) {
        final Logger logger = Logger.getAnonymousLogger();
        logger.log(Level.SEVERE, "Closing resources", exception);
      }
    });

    // reset unsaved flag.
    uiDataService.lastSavedDateProperty().addListener(
        (observable, oldValue, newValue) -> this.databaseChanged = false);

  }

  private void initializeViewMenuItems() {
    final String sessionName = "sessionName";
    final String sessionFormat = "sessionFormat";
    uiDataService.setSessionDisplayFormatProperty(userPreferences.get(sessionFormat, ""));

    if ("id".equals(userPreferences.get(sessionFormat, ""))) {
      rbMenuItemSessionId.setSelected(true);
    } else if ("key".equals(userPreferences.get(sessionFormat, ""))) {
      rbMenuItemSessionKey.setSelected(true);
    } else {
      rbMenuItemSessionName.setSelected(true);
    }

    rbMenuItemSessionName.setToggleGroup(sessionPreferenceToggle);
    rbMenuItemSessionName.setUserData(sessionName);
    rbMenuItemSessionId.setToggleGroup(sessionPreferenceToggle);
    rbMenuItemSessionId.setUserData("sessionId");
    rbMenuItemSessionKey.setToggleGroup(sessionPreferenceToggle);
    rbMenuItemSessionKey.setUserData("sessionKey");

    sessionPreferenceToggle.selectedToggleProperty().addListener(
        (observable, oldValue, newValue) -> {
          if (sessionPreferenceToggle.getSelectedToggle() != null) {
            final String selectedPref = sessionPreferenceToggle.getSelectedToggle().getUserData()
                .toString();
            if (sessionName.equals(selectedPref)) {
              userPreferences.put(sessionFormat, "name");
            } else if ("sessionKey".equals(selectedPref)) {
              userPreferences.put(sessionFormat, "key");
            } else {
              userPreferences.put(sessionFormat, "id");
            }
            uiDataService.setSessionDisplayFormatProperty(userPreferences.get(sessionFormat, ""));
          }
        });
  }

  /**
   * Opens a file.
   */
  @SuppressWarnings("UnusedParameters")
  public final void openFile(final ActionEvent actionEvent) {
    final FileChooser fileChooser = prepareFileChooser("openDB");
    //
    final File file = fileChooser.showOpenDialog(stage);
    //
    if (file != null) {
      final String newInitialDir = file.getAbsoluteFile().getParent();
      preferences.put(DB_PATH, file.getAbsolutePath());
      preferences.put(LAST_DB_OPEN_DIR, newInitialDir);
      //
      this.loadData(file.getAbsolutePath());
    }
  }

  /**
   * Saves a file.
   */
  @FXML
  @SuppressWarnings("UnusedParamters")
  private void saveFile() {
    try {
      Files.copy((Path) properties.get(TEMP_DB_PATH), Paths.get(properties.getProperty(DB_PATH)),
          StandardCopyOption.REPLACE_EXISTING);
      uiDataService.setLastSavedDate(new Date());
      logger.log(Level.INFO, "File saving finished!");
    } catch (final IOException exc) {
      logger.log(Level.SEVERE, "File saving failed!", exc);
    }
  }

  /**
   * Saves a file at another location.
   */
  @FXML
  @SuppressWarnings( {"UnusedParamters", "unused"})
  private void saveFileAs() {
    final FileChooser fileChooser = prepareFileChooser("saveDB");
    fileChooser.setInitialFileName("data.sqlite3");
    //
    final File file = fileChooser.showSaveDialog(stage);
    //
    if (file != null) {
      try {
        Files.copy((Path) properties.get(TEMP_DB_PATH), Paths.get(file.getAbsolutePath()));
        logger.log(Level.INFO, "File saving finished!");
      } catch (final IOException exception) {
        logger.log(Level.SEVERE, "File saving failed!", exception);
      }
    }
  }

  /**
   * Prepare a file chooser and return the file.
   *
   * @param title title key to find resource
   */
  private FileChooser prepareFileChooser(final String title) {
    final FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle(resources.getString(title));
    //
    final String initialDirName = preferences.get(LAST_DB_OPEN_DIR,
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

  /**
   * The menu item's action to export the current state of the database to a zip file containing the
   * xml files.
   */
  @FXML
  private void exportCurrentDbState() {
    // TODO: should we have a modal progress window to avoid confusion, since the export takes
    // a few instants to finish
    // TODO: consider generating the file to a temporary location and moving it to the final
    // location after the generation finished successfully.
    final File selectedFile = getXmlExportFile();

    if (selectedFile != null) {
      executor.execute(new ExportXmlTask(selectedFile));
    }
  }

  private File getXmlExportFile() {
    final DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
    final String dateTime = dateFormat.format(new Date());

    final FileChooser fileChooser = new FileChooser();
    //
    final File initialDir =
        new File(preferences.get(LAST_XML_EXPORT_DIR, System.getProperty("user.home")));
    if (initialDir.isDirectory()) {
      fileChooser.setInitialDirectory(initialDir);
    }
    //
    fileChooser.setInitialFileName("plues_xml_database_" + dateTime + ".zip");
    fileChooser.setTitle(resources.getString("chooser"));

    final File selectedFile = fileChooser.showSaveDialog(null);

    if (selectedFile != null) {

      final String newInitialDir = selectedFile.getAbsoluteFile().getParent();
      preferences.put(LAST_XML_EXPORT_DIR, newInitialDir);
    }
    return selectedFile;
  }

  private void loadData(final String path) {

    final StoreLoaderTask storeLoader = this.getStoreLoaderTask(path);
    delayedStore.whenAvailable(solverLoader::load);

    this.openFileMenuItem.setDisable(true);
    this.submitTask(storeLoader);

  }

  private StoreLoaderTask getStoreLoaderTask(final String path) {

    final StoreLoaderTask storeLoader = storeLoaderTaskFactory.create(path);
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
      final ObservableStore s = (ObservableStore) event.getSource().getValue();
      this.delayedStore.set(s);
    }));
    return storeLoader;
  }

  private void showCriticalExceptionDialog(final Throwable ex, final String message) {
    final ExceptionDialog ed = new ExceptionDialog();

    ed.setTitle(resources.getString("edTitle"));
    ed.setHeaderText(message);
    ed.setException(ex);

    ed.showAndWait();
  }

  private void submitTask(final Task<?> task, final ExecutorService exec) {
    exec.submit(task);
  }

  @SuppressWarnings("unused")
  private void submitTask(final Task<?> task) {
    this.submitTask(task, this.executor);
  }

  /**
   * Method to open ChangeLog by clicking on menu item.
   */
  @FXML
  private void openChangeLog() {
    final Stage logStage = new Stage();
    logStage.setTitle(resources.getString("logTitle"));
    logStage.setScene(new Scene(changeLog, 800, 600));
    logStage.setResizable(false);
    logStage.show();

    // TODO delete observer
  }

  /**
   * Open the reports view in a new stage.
   */
  @FXML
  private void openReports() {
    final Reports reports = reportsProvider.get();
    final Stage reportStage = new Stage();
    reportStage.setTitle(resources.getString("reportsTitle"));
    reportStage.setScene(new Scene(reports, 700, 620));
    reportStage.show();
  }

  @FXML
  private void setTimeoutOneMinute() {
    setTimeout(60);
  }

  @FXML
  private void setTimeoutThreeMinutes() {
    setTimeout(180);
  }

  @FXML
  private void setTimeoutFiveMinutes() {
    setTimeout(300);
  }

  @FXML
  private void setTimeoutCustom() {
    final TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle(resources.getString("timeoutTitle"));
    dialog.setHeaderText(resources.getString("timeoutHeader"));
    dialog.setContentText(resources.getString("timeoutContent"));

    final Optional<String> result = dialog.showAndWait();
    result.ifPresent(timeout -> {
      try {
        setTimeout(Integer.parseInt(timeout));
      } catch (final NumberFormatException exception) {
        logger.log(Level.SEVERE, "Incorrect input: " + timeout);
      }
    });
  }

  /**
   * Set timeout for solver tasks.
   * @param timeout New timeout
   */
  private void setTimeout(final int timeout) {
    solverService.setTimeout(timeout);
    logger.log(Level.INFO, "Timeout set to " + timeout + "seconds");
  }

  /**
   * Ask user for permission to close window using Alert. User can save database before closing.
   */
  @FXML
  private void closeWindow(final Event event) {
    if (!databaseChanged) {
      stage.close();
      return;
    }

    final Alert closeConfirmation = new Alert(Alert.AlertType.CONFIRMATION);
    closeConfirmation.setTitle("Confirm");
    closeConfirmation.setHeaderText("Save before closing?");

    final ButtonType save = new ButtonType("Save");
    final ButtonType saveAs = new ButtonType("Save as");
    final ButtonType withoutSaving = new ButtonType("Close without saving");
    final ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
    closeConfirmation.getButtonTypes().setAll(save, saveAs, withoutSaving, cancel);

    final Optional<ButtonType> answer = closeConfirmation.showAndWait();
    final ButtonType result = answer.orElse(cancel);

    if (result == save) {
      saveFile();
    } else if (result == saveAs) {
      saveFileAs();
    }

    // if the result is to cancel we ignore the close request and consume the event
    // in all other cases we close the stage
    if (result == cancel) {
      event.consume();
    } else {
      stage.close();
    }
  }

  /**
   * Show credits.
   */
  @FXML
  private void about() {
    final AboutWindow aboutWindow = aboutWindowProvider.get();
    final Stage aboutStage = new Stage();
    aboutWindow.setPadding(new Insets(10.0, 10.0, 10.0, 10.0));
    aboutStage.setTitle(resources.getString("about"));
    aboutStage.setScene(new Scene(aboutWindow, 550, 400));
    aboutStage.setResizable(false);
    aboutStage.show();
  }

  private class ExportXmlTask extends Task<Void> {

    private final File selectedFile;

    ExportXmlTask(final File selectedFile) {
      this.selectedFile = selectedFile;
    }

    @Override
    protected Void call() throws Exception {

      updateTitle(resources.getString("export.title"));
      updateProgress(1, 3);
      updateMessage(resources.getString("export.gen"));

      writeZipFile();
      return null;
    }

    private void writeZipFile() {
      try (ByteArrayOutputStream exportXmlStream = new XmlExporter(delayedStore.get()).export();
           OutputStream outputStream = new FileOutputStream(selectedFile)) {
        updateProgress(2, 3);

        updateMessage(resources.getString("export.write"));
        exportXmlStream.writeTo(outputStream);
        updateProgress(3, 3);
        logger.info("Wrote xml export to " + selectedFile.getAbsolutePath());

      } catch (final IOException exception) {
        showCriticalExceptionDialog(exception, "XML Export Failed");
      }
    }
  }
}
