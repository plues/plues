package de.hhu.stups.plues.ui.components;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;

import de.codecentric.centerdevice.MenuToolkit;
import de.hhu.stups.plues.modelgenerator.XmlExporter;
import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.services.HistoryManager;
import de.hhu.stups.plues.services.MainMenuService;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.tasks.SolverLoaderImpl;
import de.hhu.stups.plues.tasks.StoreLoaderTask;
import de.hhu.stups.plues.ui.components.timetable.SessionDisplayFormat;
import de.hhu.stups.plues.ui.controller.MainController;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;

import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;


public class MainMenuBar extends MenuBar implements Initializable {

  private static final String SESSION_FORMAT_PREF_KEY = "sessionFormat";
  private static final String LAST_DB_OPEN_DIR = "LAST_DB_OPEN_DIR";
  private static final String LAST_XML_EXPORT_DIR = "LAST_XML_EXPORT_DIR";
  private static final String TEMP_DB_PATH = "tempDBpath";
  private static final String DB_PATH = "dbpath";
  private static final ListeningExecutorService EXECUTOR_SERVICE;

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final UiDataService uiDataService;
  private final IntegerProperty customTimeoutProperty;
  private final Preferences preferences = Preferences.userNodeForPackage(MainController.class);
  private final SolverLoaderImpl solverLoader;
  private final ExecutorService executor;
  private final Preferences userPreferences;
  private final ToggleGroup timeoutPreferenceToggle = new ToggleGroup();
  @FXML
  @SuppressWarnings("unused")
  private ToggleGroup sessionPreferenceToggle;
  private final Properties properties;
  private final Router router;
  private final MainMenuService mainMenuService;

  private Boolean undoRedoInProgress = false;
  private RadioMenuItem customTimeoutItem;
  private ResourceBundle resources;

  @FXML
  @SuppressWarnings("unused")
  private MenuItem undoLastMenuItem;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem undoAllMenuItem;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem redoLastMenuItem;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem saveFileMenuItem;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem saveFileAsMenuItem;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem openFileMenuItem;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem exportStateMenuItem;
  @FXML
  @SuppressWarnings("unused")
  private Menu selectTimeoutMenu;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem setTimeoutMenuItem;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem openChangeLogMenuItem;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem openReportsMenuItem;
  @FXML
  @SuppressWarnings("unused")
  private MenuItem aboutMenuItem;
  @FXML
  @SuppressWarnings("unused")
  private RadioMenuItem fifteenSecondsMenuItem;
  @FXML
  @SuppressWarnings("unused")
  private RadioMenuItem oneMinuteMenuItem;
  @FXML
  @SuppressWarnings("unused")
  private RadioMenuItem threeMinutesMenuItem;
  @FXML
  @SuppressWarnings("unused")
  private RadioMenuItem fiveMinutesMenuItem;
  @FXML
  @SuppressWarnings("unused")
  private RadioMenuItem twentyMinutesMenuItem;

  /**
   * Constructor of the main menu bar.
   */
  @Inject
  public MainMenuBar(final Inflater inflater,
                     final SolverLoaderImpl solverLoader,
                     final UiDataService uiDataService,
                     final Router router,
                     final ExecutorService executor,
                     final Properties properties,
                     final MainMenuService mainMenuService) {
    this.uiDataService = uiDataService;
    this.router = router;
    this.solverLoader = solverLoader;
    this.executor = executor;
    this.properties = properties;
    this.mainMenuService = mainMenuService;

    customTimeoutProperty = new SimpleIntegerProperty(0);
    userPreferences = Preferences.userRoot().node("Plues");

    inflater.inflate("components/MainMenuBar", this, this, "MainController");
    logger.info("Version: " + properties.get("version"));
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    this.resources = resources;
    initializeMenu();

    final HistoryManager historyManager = mainMenuService.getHistoryManager();
    //
    final BooleanBinding tasksRunning = uiDataService.runningTasksProperty().greaterThan(0);
    final BooleanBinding canUndo = historyManager.undoHistoryEmptyProperty().or(tasksRunning)
        .or(historyManager.historyEnabledProperty().not());

    undoLastMenuItem.disableProperty().bind(canUndo);
    undoAllMenuItem.disableProperty().bind(canUndo);
    redoLastMenuItem.disableProperty().bind(historyManager.redoHistoryEmptyProperty()
        .or(tasksRunning).or(historyManager.historyEnabledProperty().not()));
    //

    mainMenuService.getDelayedSolverService().whenAvailable(solverService -> {
      openReportsMenuItem.setDisable(false);
      setTimeoutMenuItem.setDisable(false);
      fifteenSecondsMenuItem.setDisable(false);
      oneMinuteMenuItem.setDisable(false);
      threeMinutesMenuItem.setDisable(false);
      fiveMinutesMenuItem.setDisable(false);
      twentyMinutesMenuItem.setDisable(false);
    });

    mainMenuService.getDelayedStore().whenAvailable(observableStore -> {
      exportStateMenuItem.setDisable(false);
      openChangeLogMenuItem.setDisable(false);
      saveFileMenuItem.setDisable(false);
      saveFileAsMenuItem.setDisable(false);
    });

    if (this.properties.get(DB_PATH) != null) {
      loadData((String) this.properties.get(DB_PATH));
    }
  }

  private void initializeMenu() {
    initializeViewMenuItems();
    initializeMacOsMenu();
  }

  private void initializeMacOsMenu() {
    // based on https://github.com/bendisposto/prob2-ui/blob/master/src/main/java/de/prob2/ui/menu/MenuController.java#L244
    if (System.getProperty("os.name", "").toLowerCase().contains("mac")) {
      final String applicationName = "PlÜS";
      final MenuToolkit tk = MenuToolkit.toolkit();
      final ObservableList<Menu> menus = getMenus();

      // Remove About menu item from Help
      aboutMenuItem.getParentMenu().getItems().remove(aboutMenuItem);
      aboutMenuItem.setText("About " + applicationName);

      // Create Mac-style application menu
      final Menu applicationMenu = tk.createDefaultApplicationMenu(applicationName);
      menus.add(0, applicationMenu);
      tk.setApplicationMenu(applicationMenu);
      applicationMenu.getItems().setAll(aboutMenuItem, new SeparatorMenuItem(),
          new SeparatorMenuItem(), tk.createHideMenuItem(applicationName),
          tk.createHideOthersMenuItem(), tk.createUnhideAllMenuItem(), new SeparatorMenuItem(),
          tk.createQuitMenuItem(applicationName));

      // Add Mac-style items to Window menu
      final Menu windowMenu = new Menu(resources.getString("window"));
      windowMenu.setMnemonicParsing(false);
      windowMenu.getItems().addAll(tk.createMinimizeMenuItem(), tk.createZoomMenuItem(),
          tk.createCycleWindowsItem(), new SeparatorMenuItem(), tk.createBringAllToFrontItem(),
          new SeparatorMenuItem());
      menus.add(menus.size() - 1, windowMenu);
      tk.autoAddWindowMenuItems(windowMenu);
      tk.setGlobalMenuBar(this);
    }
  }

  private void initializeViewMenuItems() {
    final SessionDisplayFormat userFormat = getSessionDisplayFormatFromPreferences();
    uiDataService.setSessionDisplayFormatProperty(userFormat);


    fifteenSecondsMenuItem.setToggleGroup(timeoutPreferenceToggle);
    oneMinuteMenuItem.setToggleGroup(timeoutPreferenceToggle);
    threeMinutesMenuItem.setToggleGroup(timeoutPreferenceToggle);
    fiveMinutesMenuItem.setToggleGroup(timeoutPreferenceToggle);
    twentyMinutesMenuItem.setToggleGroup(timeoutPreferenceToggle);
  }

  private void initializeCustomTimeoutMenuItem() {
    if (customTimeoutItem != null) {
      return;
    }
    customTimeoutItem = new RadioMenuItem();
    customTimeoutItem.setToggleGroup(timeoutPreferenceToggle);

    final int lastButOne = selectTimeoutMenu.getItems().size() - 1;
    selectTimeoutMenu.getItems().add(lastButOne, customTimeoutItem);

    customTimeoutItem.addEventHandler(MouseEvent.MOUSE_CLICKED, event ->
        setTimeout(customTimeoutProperty.get()));

    customTimeoutProperty.addListener((observable, oldValue, newValue) -> {
      customTimeoutItem.setText(String.format(resources.getString("timeout.custom"), newValue));
      customTimeoutItem.setSelected(true);
    });
  }

  private SessionDisplayFormat getSessionDisplayFormatFromPreferences() {
    final String preference
        = userPreferences.get(SESSION_FORMAT_PREF_KEY,
        String.valueOf(SessionDisplayFormat.UNIT_KEY));

    SessionDisplayFormat userFormat;
    try {
      userFormat = SessionDisplayFormat.valueOf(preference);
    } catch (final IllegalArgumentException exception) {
      logger.error("Unknown SessionDisplayFormat", exception);
      userFormat = SessionDisplayFormat.UNIT_KEY;
      userPreferences.put(SESSION_FORMAT_PREF_KEY, String.valueOf(userFormat));
    }
    return userFormat;
  }

  @SuppressWarnings("unused")
  private void updateSessionDisplayFormat(final ObservableValue<? extends Toggle> observable,
                                          final Toggle oldValue, final Toggle newValue) {
    if (newValue == null) {
      return;
    }
    final String selectedPref = newValue.getUserData().toString();
    SessionDisplayFormat format = SessionDisplayFormat.UNIT_KEY;
    try {
      format = SessionDisplayFormat.valueOf(selectedPref);
    } catch (final IllegalArgumentException exception) {
      logger.error("User selected invalid session format", exception);
    }
    userPreferences.put(SESSION_FORMAT_PREF_KEY, String.valueOf(format));
    uiDataService.setSessionDisplayFormatProperty(format);
  }

  /**
   * Opens a file.
   */
  @FXML
  @SuppressWarnings("unused")
  public final void openFile(final ActionEvent actionEvent) {
    final FileChooser fileChooser =
        mainMenuService.prepareFileChooser(resources.getString("openDB"), preferences);

    //
    final File file = fileChooser.showOpenDialog(mainMenuService.getStage());
    //
    if (file != null) {
      final String newInitialDir = file.getAbsoluteFile().getParent();
      preferences.put(DB_PATH, file.getAbsolutePath());
      preferences.put(LAST_DB_OPEN_DIR, newInitialDir);
      //
      this.loadData(file.getAbsolutePath());
    }
  }

  @FXML
  @SuppressWarnings("unused")
  private void openReports() {
    router.transitionTo(RouteNames.OPEN_REPORTS);
  }

  @FXML
  @SuppressWarnings("unused")
  private void closeWindow(final Event event) {
    router.transitionTo(RouteNames.CLOSE_APP, event);
  }

  static {
    final ThreadFactory threadFactoryBuilder = new ThreadFactoryBuilder().setDaemon(true)
        .setNameFormat("task-progress-hide-runner-%d").build();

    EXECUTOR_SERVICE = MoreExecutors.listeningDecorator(
        Executors.newSingleThreadScheduledExecutor(threadFactoryBuilder));
  }

  /**
   * Navigate to the {@link de.hhu.stups.plues.ui.controller.Timetable} if necessary, i.e.
   * another tab is currently opened, and execute the specific undo/undoAll/redo operation
   * afterwards. In case the timetable is already present we just execute the command.
   */
  @SuppressWarnings("unused")
  private void openTimetableUndoRedoTask(final EventHandler<WorkerStateEvent> eventHandler) {
    final Task<Void> openTimetableTask = new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        router.transitionTo(RouteNames.TIMETABLE);
        // wait a short time to ensure that the tab is opened before executing the command
        TimeUnit.MILLISECONDS.sleep(200);
        return null;
      }
    };
    openTimetableTask.setOnSucceeded(eventHandler);
    EXECUTOR_SERVICE.execute(openTimetableTask);
  }

  @FXML
  @SuppressWarnings("unused")
  private void undoLastMoveOperation() {
    if (undoRedoInProgress) {
      return;
    }
    openTimetableUndoRedoTask(event -> mainMenuService.getHistoryManager().undoLastMoveOperation());
    blockUndoRedo();
  }

  @FXML
  @SuppressWarnings("unused")
  private void undoAllMoveOperations() {
    openTimetableUndoRedoTask(event -> mainMenuService.getHistoryManager().undoAllMoveOperations());
  }

  @FXML
  @SuppressWarnings("unused")
  private void redoLastMoveOperation() {
    if (undoRedoInProgress) {
      return;
    }
    openTimetableUndoRedoTask(event -> mainMenuService.getHistoryManager().redoLastMoveOperation());
    blockUndoRedo();
  }

  /**
   * Block undo/redo to prevent holding the corresponding keys which subjectively results in moving
   * the sessions all at once. Thus, we set a small delay.
   */
  private void blockUndoRedo() {
    executor.execute(() -> {
      undoRedoInProgress = true;
      try {
        TimeUnit.MILLISECONDS.sleep(200);
      } catch (final InterruptedException exception) {
        logger.error("Blocking undo/redo session move cancelled.", exception);
        Thread.currentThread().interrupt();
      }
      undoRedoInProgress = false;
    });
  }

  /**
   * Saves a file.
   */
  @FXML
  public void saveFile() {
    try {
      Files.copy((Path) properties.get(TEMP_DB_PATH), Paths.get(properties.getProperty(DB_PATH)),
          StandardCopyOption.REPLACE_EXISTING);
      uiDataService.setLastSavedDate(LocalDateTime.now());
      logger.info("File saving finished!");
    } catch (final IOException exc) {
      logger.error("File saving failed!", exc);
    }
  }

  /**
   * Saves a file at another location.
   */
  @FXML
  public boolean saveFileAs() {
    final FileChooser fileChooser = mainMenuService.prepareFileChooser("saveDB", preferences);
    fileChooser.setInitialFileName("data.sqlite3");
    //
    final File file = fileChooser.showSaveDialog(mainMenuService.getStage());
    //
    if (file != null) {
      try {
        Files.copy((Path) properties.get(TEMP_DB_PATH), Paths.get(file.getAbsolutePath()),
            StandardCopyOption.REPLACE_EXISTING);
        logger.info("File saving finished!");
        uiDataService.setLastSavedDate(LocalDateTime.now());
        return true;
      } catch (final IOException exception) {
        logger.error("File saving failed!", exception);
      }
    }
    return false;
  }

  /**
   * The menu item's action to export the current state of the database to a zip file containing the
   * xml files.
   */
  @FXML
  @SuppressWarnings("unused")
  private void exportCurrentDbState() {
    final File selectedFile = getXmlExportFile();

    if (selectedFile != null) {
      executor.execute(new ExportXmlTask(selectedFile));
    }
  }

  /**
   * Method to open ChangeLog by clicking on menu item.
   */
  @FXML
  @SuppressWarnings("unused")
  private void openChangeLog() {
    router.transitionTo(RouteNames.CHANGELOG, resources.getString("logTitle"));
  }

  @FXML
  @SuppressWarnings("unused")
  private void setTimeoutFifteenSeconds() {
    fifteenSecondsMenuItem.setSelected(true);
    setTimeout(15);
  }

  @FXML
  @SuppressWarnings("unused")
  private void setTimeoutOneMinute() {
    oneMinuteMenuItem.setSelected(true);
    setTimeout(60);
  }

  @FXML
  @SuppressWarnings("unused")
  private void setTimeoutThreeMinutes() {
    threeMinutesMenuItem.setSelected(true);
    setTimeout(180);
  }

  @FXML
  @SuppressWarnings("unused")
  private void setTimeoutFiveMinutes() {
    fiveMinutesMenuItem.setSelected(true);
    setTimeout(300);
  }

  @FXML
  @SuppressWarnings("unused")
  private void setTimeoutTwentyMinutes() {
    twentyMinutesMenuItem.setSelected(true);
    setTimeout(1200);
  }

  @FXML
  @SuppressWarnings("unused")
  private void setTimeoutCustom() {
    final TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle(resources.getString("timeout.Title"));
    dialog.setHeaderText(resources.getString("timeout.Header"));
    dialog.setContentText(resources.getString("timeout.Content"));

    final Optional<String> result = dialog.showAndWait();
    result.ifPresent(timeout -> {
      if (!timeout.isEmpty() && NumberUtils.isNumber(timeout)
          && Double.valueOf(timeout).intValue() > 0) {
        try {
          initializeCustomTimeoutMenuItem();
          final int timeoutValue = Double.valueOf(timeout).intValue();
          setTimeout(timeoutValue);
          customTimeoutProperty.setValue(timeoutValue);
        } catch (final NumberFormatException exception) {
          logger.error("Incorrect input: " + timeout);
        }
      }
    });
  }

  /**
   * Show credits.
   */
  @FXML
  @SuppressWarnings("unused")
  private void about() {
    router.transitionTo(RouteNames.ABOUT_WINDOW, resources.getString("about"));
  }

  @FXML
  @SuppressWarnings("unused")
  private void showHtmlHandbook(final ActionEvent actionEvent) {
    router.transitionTo(RouteNames.HANDBOOK_HTML);
  }

  @FXML
  @SuppressWarnings("unused")
  public void showPdfHandbook(final ActionEvent actionEvent) {
    router.transitionTo(RouteNames.HANDBOOK_PDF);
  }

  private File getXmlExportFile() {

    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
    final String dateTime = LocalDateTime.now().format(formatter);

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

  /**
   * Load data to the store.
   */
  private void loadData(final String path) {
    final StoreLoaderTask storeLoader = mainMenuService.getStoreLoaderTask(path,
        resources.getString("edTitle"));
    properties.setProperty(DB_PATH, path);
    mainMenuService.getDelayedStore().whenAvailable(solverLoader::load);

    this.openFileMenuItem.setDisable(true);
    executor.submit(storeLoader);
  }

  /**
   * Set timeout for solver tasks.
   *
   * @param timeout New timeout
   */
  private void setTimeout(final int timeout) {
    mainMenuService.getDelayedSolverService().whenAvailable(solverService -> {
      solverService.setTimeout(timeout);
      logger.info("Timeout set to " + timeout + " seconds");
    });
  }

  private class ExportXmlTask extends Task<Void> {

    private final File selectedFile;

    ExportXmlTask(final File selectedFile) {
      this.selectedFile = selectedFile;

      updateTitle(resources.getString("export.title"));
      updateProgress(0, 3);
      updateMessage(resources.getString("export.gen"));
    }

    @Override
    protected Void call() throws Exception {
      updateProgress(1, 3);
      writeZipFile();
      return null;
    }

    private void writeZipFile() {
      try (ByteArrayOutputStream exportXmlStream =
               new XmlExporter(mainMenuService.getDelayedStore().get()).export();
           OutputStream outputStream = new FileOutputStream(selectedFile)) {
        updateProgress(2, 3);

        updateMessage(resources.getString("export.write"));
        exportXmlStream.writeTo(outputStream);
        updateProgress(3, 3);
        logger.info("Wrote xml export to " + selectedFile.getAbsolutePath());
      } catch (final IOException exception) {
        mainMenuService.showCriticalExceptionDialog(exception, resources.getString("edTitle"),
            "XML Export Failed");
      }
    }
  }
}
