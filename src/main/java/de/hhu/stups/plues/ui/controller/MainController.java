package de.hhu.stups.plues.ui.controller;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.codecentric.centerdevice.MenuToolkit;
import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.ObservableStore;
import de.hhu.stups.plues.modelgenerator.XmlExporter;
import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.routes.Router;
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
import de.hhu.stups.plues.ui.components.ExceptionDialog;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.controlsfx.control.StatusBar;
import org.controlsfx.control.TaskProgressView;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

@Singleton
public class MainController implements Initializable {

  private static final Map<Class, FontAwesomeIcon> iconMap = new HashMap<>();
  private static final FontAwesomeIcon DEFAULT_ICON = FontAwesomeIcon.TASKS;
  private static final String LAST_DB_OPEN_DIR = "LAST_DB_OPEN_DIR";
  private static final String LAST_XML_EXPORT_DIR = "LAST_XML_EXPORT_DIR";
  private static final String DB_PATH = "dbpath";
  private static final String TEMP_DB_PATH = "tempDBpath";
  private static final ListeningScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE;

  static {
    iconMap.put(StoreLoaderTask.class, FontAwesomeIcon.DATABASE);
    iconMap.put(SolverLoaderTask.class, FontAwesomeIcon.COGS);
    iconMap.put(SolverTask.class, FontAwesomeIcon.CALENDAR);
    iconMap.put(PdfRenderingTask.class, FontAwesomeIcon.FILE_PDF_ALT);
  }

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final Delayed<ObservableStore> delayedStore;
  private final Properties properties;
  private final Stage stage;
  private final ExecutorService executor;
  private final UiDataService uiDataService;
  private final Preferences userPreferences;

  private final Preferences preferences = Preferences.userNodeForPackage(MainController.class);
  private final SolverLoaderImpl solverLoader;
  private final StoreLoaderTaskFactory storeLoaderTaskFactory;
  private final Router router;
  private final ResourceManager resourceManager;
  private final Delayed<SolverService> delayedSolverService;
  private final ToggleGroup sessionPreferenceToggle = new ToggleGroup();
  private final ToggleGroup timeoutPreferenceToggle = new ToggleGroup();

  private boolean databaseChanged = false;
  private ResourceBundle resources;

  @FXML
  private MenuBar menuBar;
  @FXML
  private MenuItem saveFileMenuItem;
  @FXML
  private MenuItem saveFileAsMenuItem;
  @FXML
  private MenuItem openFileMenuItem;
  @FXML
  private MenuItem exportStateMenuItem;
  @FXML
  private Menu selectTimeoutMenu;
  @FXML
  private MenuItem setTimeoutMenuItem;
  @FXML
  private RadioMenuItem oneMinuteMenuItem;
  @FXML
  private RadioMenuItem threeMinutesMenuItem;
  @FXML
  private RadioMenuItem fiveMinutesMenuItem;
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
  @FXML
  private Menu windowMenu;
  @FXML
  private MenuItem aboutMenuItem;
  @FXML
  private ScrollPane scrollPaneTaskProgress;
  @FXML
  private VBox boxTaskProgress;
  @FXML
  private SplitPane mainSplitPane;
  @FXML
  private StatusBar mainStatusBar;
  @FXML
  private ProgressBar mainProgressBar;
  @FXML
  private Label lbRunningTasks;

  private RadioMenuItem customTimeoutItem;
  private final IntegerProperty customTimeoutProperty;

  /**
   * MainController component.
   */
  @Inject
  public MainController(final Delayed<ObservableStore> delayedStore,
                        final Delayed<SolverService> delayedSolverService,
                        final SolverLoaderImpl solverLoader, final Properties properties,
                        final Stage stage,
                        final Router router,
                        final StoreLoaderTaskFactory storeLoaderTaskFactory,
                        final ObservableListeningExecutorService executorService,
                        final ResourceManager resourceManager,
                        final UiDataService uiDataService) {
    this.delayedStore = delayedStore;
    this.delayedSolverService = delayedSolverService;
    this.solverLoader = solverLoader;
    this.properties = properties;
    this.stage = stage;
    this.router = router;
    this.storeLoaderTaskFactory = storeLoaderTaskFactory;
    this.executor = executorService;
    this.resourceManager = resourceManager;
    this.uiDataService = uiDataService;

    customTimeoutProperty = new SimpleIntegerProperty(0);
    userPreferences = Preferences.userRoot().node("Plues");

    executorService.addObserver((observable, arg) -> this.register(arg));

    logger.info("Starting PlÜS Version: " + properties.get("version"));
  }

  private void register(final Object task) {
    if (task instanceof Task<?>) {
      logger.trace("registering task for taskview");
      Platform.runLater(() -> this.taskProgress.getTasks().add((Task<?>) task));
    } else {
      logger.trace("ignoring task for taskview");
    }
  }

  @SuppressWarnings("unused")
  private Node getGraphicForTask(final Task<?> task) {
    final FontAwesomeIcon icon = iconMap.getOrDefault(task.getClass(), DEFAULT_ICON);
    return FontAwesomeIconFactory.get().createIcon(icon, "2em");
  }

  @SuppressWarnings("unused")
  private void handleKeyPressed(final KeyEvent event) {
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
  }

  @Override
  public final void initialize(final URL location,
                               final ResourceBundle resources) {
    this.resources = resources;

    taskProgress.setGraphicFactory(this::getGraphicForTask);

    taskProgress.prefWidthProperty().bind(scrollPaneTaskProgress.widthProperty());
    taskProgress.prefHeightProperty().bind(scrollPaneTaskProgress.heightProperty());

    boxTaskProgress.prefWidth(mainSplitPane.getWidth() / 3.0);
    boxTaskProgress.maxWidthProperty().bind(mainSplitPane.widthProperty().divide(3.0));
    boxTaskProgress.minWidthProperty().bind(mainSplitPane.widthProperty().divide(5.0));

    // don't show tasks on startup
    mainSplitPane.getItems().remove(boxTaskProgress);
    mainStatusBar.setText("");

    removeTaskProgressBox();

    mainProgressBar.setOnMouseEntered(event -> stage.getScene().setCursor(Cursor.HAND));
    mainProgressBar.setOnMouseExited(event -> stage.getScene().setCursor(Cursor.DEFAULT));

    lbRunningTasks.setOnMouseEntered(event -> stage.getScene().setCursor(Cursor.HAND));
    lbRunningTasks.setOnMouseExited(event -> stage.getScene().setCursor(Cursor.DEFAULT));

    initializeTaskProgressListener();

    tabPane.setOnKeyPressed(this::handleKeyPressed);

    initializeMenu();

    delayedSolverService.whenAvailable(solverService -> {
      openReportsMenuItem.setDisable(false);
      setTimeoutMenuItem.setDisable(false);
      oneMinuteMenuItem.disableProperty().bind(oneMinuteMenuItem.selectedProperty());
      threeMinutesMenuItem.disableProperty().bind(threeMinutesMenuItem.selectedProperty());
      fiveMinutesMenuItem.disableProperty().bind(fiveMinutesMenuItem.selectedProperty());
    });

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
        logger.error("Closing resources", exception);
        Thread.currentThread().interrupt();
      }
    });

    // reset unsaved flag.
    uiDataService.lastSavedDateProperty().addListener(
        (observable, oldValue, newValue) -> this.databaseChanged = false);

  }

  private void initializeTaskProgressListener() {
    final ObservableList<Task<?>> scheduledTasks = taskProgress.getTasks();
    final String tasksSingular = resources.getString("tasksSingular");
    final String tasksPlural = resources.getString("tasksPlural");

    scheduledTasks.addListener((ListChangeListener.Change<? extends Task<?>> change) -> {
      if (scheduledTasks.isEmpty()) {
        removeTaskProgressBox();
      } else {
        if (!mainStatusBar.getRightItems().contains(mainProgressBar)) {
          mainStatusBar.getRightItems().addAll(lbRunningTasks, mainProgressBar);
        }
        bindProgressPropertyIfNecessary(scheduledTasks);
        lbRunningTasks.setText(scheduledTasks.size() + " " + ((scheduledTasks.size() == 1)
            ? tasksSingular : tasksPlural));
      }
    });

    final EventHandler<MouseEvent> mouseEventEventHandler = event -> {
      final ObservableList<Node> splitPaneItems = mainSplitPane.getItems();
      if (splitPaneItems.contains(boxTaskProgress)) {
        splitPaneItems.remove(boxTaskProgress);
      } else {
        splitPaneItems.add(boxTaskProgress);
      }
    };
    lbRunningTasks.addEventHandler(MouseEvent.MOUSE_CLICKED, mouseEventEventHandler);
    mainProgressBar.addEventHandler(MouseEvent.MOUSE_CLICKED, mouseEventEventHandler);
  }

  /**
   * Bind the {@link #mainProgressBar progress bar's} ProgressProperty to the first running task if
   * it is unbound and there is at least one scheduled task given.
   */
  private void bindProgressPropertyIfNecessary(final ObservableList<Task<?>> scheduledTasks) {
    if (scheduledTasks.size() == 1) {
      mainProgressBar.progressProperty().bind(scheduledTasks.get(0).progressProperty());
    } else if (!mainProgressBar.progressProperty().isBound()) {
      final Optional<Task<?>> optionalRunningTask = scheduledTasks.stream()
          .filter(Task::isRunning).findFirst();

      if (optionalRunningTask.isPresent()) {
        final Task<?> runningTask = optionalRunningTask.get();
        mainProgressBar.progressProperty().bind(runningTask.progressProperty());
        final EventHandler<WorkerStateEvent> unbindProgressBar =
            event -> mainProgressBar.progressProperty().unbind();
        runningTask.setOnSucceeded(unbindProgressBar);
        runningTask.setOnCancelled(unbindProgressBar);
        runningTask.setOnFailed(unbindProgressBar);
      }
    }
  }

  /**
   * Wait some time and hide the {@link #taskProgress task progress view} if there are no running
   * tasks anymore.
   */

  static {
    final ThreadFactory threadFactoryBuilder = new ThreadFactoryBuilder().setDaemon(true)
        .setNameFormat("task-progress-hide-runner-%d").build();

    SCHEDULED_EXECUTOR_SERVICE = MoreExecutors.listeningDecorator(
        Executors.newSingleThreadScheduledExecutor(threadFactoryBuilder));

  }

  private void removeTaskProgressBox() {
    mainProgressBar.progressProperty().unbind();
    mainStatusBar.getRightItems().remove(lbRunningTasks);
    mainStatusBar.getRightItems().remove(mainProgressBar);
    SCHEDULED_EXECUTOR_SERVICE.schedule(() ->
        Platform.runLater(() -> {
          if (taskProgress.getTasks().isEmpty()) {
            mainSplitPane.getItems().remove(boxTaskProgress);
          }
        }), 3, TimeUnit.SECONDS);
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

      // Remove About menu item from Help
      aboutMenuItem.getParentMenu().getItems().remove(aboutMenuItem);
      aboutMenuItem.setText("About " + applicationName);

      // Create Mac-style application menu
      final Menu applicationMenu = tk.createDefaultApplicationMenu(applicationName);
      menuBar.getMenus().add(0, applicationMenu);
      tk.setApplicationMenu(applicationMenu);
      applicationMenu.getItems().setAll(aboutMenuItem, new SeparatorMenuItem(),
          new SeparatorMenuItem(), tk.createHideMenuItem(applicationName),
          tk.createHideOthersMenuItem(), tk.createUnhideAllMenuItem(), new SeparatorMenuItem(),
          tk.createQuitMenuItem(applicationName));

      // Add Mac-style items to Window menu
      windowMenu.getItems().addAll(tk.createMinimizeMenuItem(), tk.createZoomMenuItem(),
          tk.createCycleWindowsItem(), new SeparatorMenuItem(), tk.createBringAllToFrontItem(),
          new SeparatorMenuItem());
      tk.autoAddWindowMenuItems(windowMenu);
      tk.setGlobalMenuBar(menuBar);
    }
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
    rbMenuItemSessionId.setToggleGroup(sessionPreferenceToggle);
    rbMenuItemSessionKey.setToggleGroup(sessionPreferenceToggle);

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

    oneMinuteMenuItem.setToggleGroup(timeoutPreferenceToggle);
    threeMinutesMenuItem.setToggleGroup(timeoutPreferenceToggle);
    fiveMinutesMenuItem.setToggleGroup(timeoutPreferenceToggle);
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
  private void saveFile() {
    try {
      Files.copy((Path) properties.get(TEMP_DB_PATH), Paths.get(properties.getProperty(DB_PATH)),
          StandardCopyOption.REPLACE_EXISTING);
      uiDataService.setLastSavedDate(new Date());
      logger.info("File saving finished!");
    } catch (final IOException exc) {
      logger.error("File saving failed!", exc);
    }
  }

  /**
   * Saves a file at another location.
   */
  @FXML
  @SuppressWarnings("unused")
  private boolean saveFileAs() {
    final FileChooser fileChooser = prepareFileChooser("saveDB");
    fileChooser.setInitialFileName("data.sqlite3");
    //
    final File file = fileChooser.showSaveDialog(stage);
    //
    if (file != null) {
      try {
        Files.copy((Path) properties.get(TEMP_DB_PATH), Paths.get(file.getAbsolutePath()));
        logger.info("File saving finished!");
        return true;
      } catch (final IOException exception) {
        logger.error("File saving failed!", exception);
      }
    }
    return false;
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
  @SuppressWarnings("unused")
  private void exportCurrentDbState() {
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
        (observable, oldValue, newValue) -> logger.trace("STORE progress " + newValue));
    //
    storeLoader.messageProperty().addListener(
        (observable, oldValue, newValue) -> logger.trace("STORE message " + newValue));
    //
    storeLoader.setOnFailed(event -> {
      final Throwable ex = event.getSource().getException();
      logger.error("Database could not be loaded", ex);
      showCriticalExceptionDialog(ex, "Database could not be loaded");
      Platform.exit();
    });
    //
    storeLoader.setOnSucceeded(
        value -> logger.trace("STORE: loading Store succeeded"));

    storeLoader.setOnSucceeded(event -> Platform.runLater(() -> {
      final ObservableStore s = (ObservableStore) event.getSource().getValue();
      this.delayedStore.set(s);
    }));
    return storeLoader;
  }

  private void showCriticalExceptionDialog(final Throwable ex, final String message) {
    Platform.runLater(() -> {
      final ExceptionDialog ed = new ExceptionDialog();

      ed.setTitle(resources.getString("edTitle"));
      ed.setHeaderText(message);
      ed.setException(ex);

      ed.showAndWait();
    });
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
  @SuppressWarnings("unused")
  private void openChangeLog() {
    router.transitionTo(RouteNames.CHANGELOG, resources.getString("logTitle"));
  }

  /**
   * Open the reports view in a new stage.
   */
  @FXML
  @SuppressWarnings("unused")
  private void openReports() {
    router.transitionTo(RouteNames.REPORTS, resources.getString("reportsTitle"));
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
  private void setTimeoutCustom() {
    final TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle(resources.getString("timeout.Title"));
    dialog.setHeaderText(resources.getString("timeout.Header"));
    dialog.setContentText(resources.getString("timeout.Content"));

    final Optional<String> result = dialog.showAndWait();
    result.ifPresent(timeout -> {
      try {
        initializeCustomTimeoutMenuItem();
        final int timeoutValue = Integer.parseInt(timeout);
        setTimeout(timeoutValue);
        customTimeoutProperty.setValue(timeoutValue);
      } catch (final NumberFormatException exception) {
        logger.error("Incorrect input: " + timeout);
      }
    });
  }

  private void initializeCustomTimeoutMenuItem() {
    if (customTimeoutItem != null) {
      return;
    }
    customTimeoutItem = new RadioMenuItem();
    customTimeoutItem.setToggleGroup(timeoutPreferenceToggle);
    customTimeoutItem.disableProperty().bind(customTimeoutItem.selectedProperty());
    selectTimeoutMenu.getItems().add(customTimeoutItem);
    customTimeoutItem.addEventHandler(MouseEvent.MOUSE_CLICKED, event ->
        setTimeout(customTimeoutProperty.get()));

    customTimeoutProperty.addListener((observable, oldValue, newValue) -> {
      customTimeoutItem.setText(String.format(resources.getString("timeout.custom"), newValue));
      customTimeoutItem.setSelected(true);
    });
  }

  /**
   * Set timeout for solver tasks.
   *
   * @param timeout New timeout
   */
  private void setTimeout(final int timeout) {
    delayedSolverService.whenAvailable(solverService -> {
      solverService.setTimeout(timeout);
      logger.info("Timeout set to " + timeout + " seconds");
    });
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
    closeConfirmation.setTitle(resources.getString("dialog.close.confirm"));
    closeConfirmation.setHeaderText(resources.getString("dialog.close.title"));
    closeConfirmation.getDialogPane().setPrefSize(825.0, 150.0);

    final ButtonType save = new ButtonType(resources.getString("dialog.close.save"));
    final ButtonType saveAs = new ButtonType(resources.getString("dialog.close.saveAs"));
    final ButtonType withoutSaving = new ButtonType(
        resources.getString("dialog.close.withoutSaving"));
    final ButtonType cancel = new ButtonType(resources.getString("dialog.close.cancel"),
        ButtonBar.ButtonData.CANCEL_CLOSE);
    closeConfirmation.getButtonTypes().setAll(save, saveAs, withoutSaving, cancel);

    final Optional<ButtonType> answer = closeConfirmation.showAndWait();
    final ButtonType result = answer.orElse(cancel);

    if (result == save) {
      saveFile();
    } else if ((result == saveAs && !saveFileAs()) || result == cancel) {
      // if the result is to cancel we ignore the close request and consume the event
      // in all other cases we close the stage
      event.consume();
    } else {
      stage.close();
    }
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
  @SuppressWarnings( {"UnusedParameters", "unused"})
  private void showHtmlHandbook(final ActionEvent actionEvent) {
    router.transitionTo(RouteNames.HANDBOOK_HTML);
  }

  @FXML
  @SuppressWarnings("UnusedParameters")
  public void showPdfHandbook(final ActionEvent actionEvent) {
    router.transitionTo(RouteNames.HANDBOOK_PDF);
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
