package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import de.hhu.stups.plues.provider.RouterProvider;
import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.services.MainMenuService;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.tasks.ObservableListeningExecutorService;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.hhu.stups.plues.tasks.SolverLoaderTask;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.tasks.StoreLoaderTask;
import de.hhu.stups.plues.ui.components.MainMenuBar;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.controlsfx.control.StatusBar;
import org.controlsfx.control.TaskProgressView;
import org.reactfx.EventStreams;
import org.reactfx.util.FxTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

@Singleton
public class MainController implements Initializable, Activatable {

  private static final Map<Class, FontAwesomeIcon> iconMap = new HashMap<>();
  private static final FontAwesomeIcon DEFAULT_ICON = FontAwesomeIcon.TASKS;
  private static final Task EMPTY_TASK = new Task() {
    // just an empty task to simulate a pending progress bar
    @Override
    protected Object call() throws Exception {
      return null;
    }
  };

  static {
    iconMap.put(StoreLoaderTask.class, FontAwesomeIcon.DATABASE);
    iconMap.put(SolverLoaderTask.class, FontAwesomeIcon.COGS);
    iconMap.put(SolverTask.class, FontAwesomeIcon.CALENDAR);
    iconMap.put(PdfRenderingTask.class, FontAwesomeIcon.FILE_PDF_ALT);
  }

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final BooleanProperty taskBoxCollapsed = new SimpleBooleanProperty(true);
  private final Stage stage;
  private final MainMenuService mainMenuService;
  private final UiDataService uiDataService;
  private final RouterProvider routerProvider;
  private final Tab reportsTab = new Tab();
  private final Provider<Reports> reportsProvider;
  private final BooleanProperty progressVisibleProperty;

  private ResourceBundle resources;
  @FXML
  private MainMenuBar mainMenuBar;
  @FXML
  private TabPane tabPane;
  @FXML
  private TaskProgressView<Task<?>> taskProgress;
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
  @FXML
  private VBox boxProgressBar;

  private SplitPane.Divider mainSplitPaneDivider;
  private double visibleDividerPos;
  private boolean fadingInProgress = false;

  /**
   * MainController component.
   */
  @Inject
  public MainController(final Stage stage,
                        final ObservableListeningExecutorService executorService,
                        final MainMenuService mainMenuService,
                        final UiDataService uiDataService,
                        final Provider<Reports> reportsProvider,
                        final RouterProvider routerProvider) {
    this.stage = stage;
    this.reportsProvider = reportsProvider;
    this.mainMenuService = mainMenuService;
    this.uiDataService = uiDataService;
    this.routerProvider = routerProvider;
    progressVisibleProperty = new SimpleBooleanProperty(false);

    executorService.getTasks().filterMap(task -> {
      if (task instanceof Task<?>) {
        logger.trace("registering task for Taskview.");
      } else {
        logger.trace("Ignoring non-task runnable for Taskview.");
      }
      return task instanceof Task<?>;
    }, task -> ((Task<?>) task)).subscribe(this::register);

    logger.info("Starting PlÜS");
  }

  private void register(final Task<?> task) {
    Platform.runLater(() -> this.taskProgress.getTasks().add(task));
  }

  @SuppressWarnings("unused")
  private Node getGraphicForTask(final Task<?> task) {
    final FontAwesomeIcon icon = iconMap.getOrDefault(task.getClass(), DEFAULT_ICON);
    return FontAwesomeIconFactory.get().createIcon(icon, "2em");
  }

  @Override
  public final void initialize(final URL location, final ResourceBundle resources) {
    this.resources = resources;

    reportsTab.setText(resources.getString("reportsTitle"));

    mainSplitPane.getItems().remove(boxTaskProgress);

    initializeTaskProgress();
    boxTaskProgress.maxWidthProperty().bind(mainSplitPane.widthProperty().divide(3.0));
    boxTaskProgress.prefWidth(0);
    boxTaskProgress.prefWidthProperty().bind(mainSplitPane.widthProperty().divide(4.0));

    mainSplitPane.widthProperty().addListener((observable, oldValue, newValue) -> {
      // calculate the divider position for the case that the task box is not collapsed
      // and has its full width
      visibleDividerPos = (mainSplitPane.getWidth() - boxTaskProgress.getPrefWidth())
          / mainSplitPane.getWidth();
      if (taskBoxCollapsed.get() && mainSplitPaneDivider != null) {
        mainSplitPaneDivider.setPosition(1.0);
      }
    });

    mainStatusBar.setText("");

    clearStatusBar();

    taskBoxCollapsed.addListener((observable, oldValue, shouldHide) ->
        hideTaskProgressBox(shouldHide));

    boxProgressBar.setOnMouseEntered(event -> stage.getScene().setCursor(Cursor.HAND));
    boxProgressBar.setOnMouseExited(event -> stage.getScene().setCursor(Cursor.DEFAULT));
    boxProgressBar.visibleProperty().bind(progressVisibleProperty);
    mainProgressBar.visibleProperty().bind(progressVisibleProperty);

    lbRunningTasks.setOnMouseEntered(event -> stage.getScene().setCursor(Cursor.HAND));
    lbRunningTasks.setOnMouseExited(event -> stage.getScene().setCursor(Cursor.DEFAULT));
    lbRunningTasks.visibleProperty().bind(progressVisibleProperty);

    initializeKeyPressedHandler();

    reportsTab.setClosable(true);

    mainMenuService.getDelayedStore().whenAvailable(store -> {
      if (mainSplitPane.getItems().contains(boxTaskProgress)) {
        return;
      }
      mainSplitPane.getItems().add(1, boxTaskProgress);
      mainSplitPaneDivider = mainSplitPane.getDividers().get(0);
      mainSplitPaneDivider.setPosition(1.0);
      disableDivider(true);
      initializeTaskProgressListener();
    });

    mainMenuService.getStoreLoaderProgressProperty().addListener(
        (observable, oldValue, newValue) -> {
          lbRunningTasks.setText(resources.getString("loadStore"));
          progressVisibleProperty.set(true);
          mainProgressBar.progressProperty().bind(observable);
        });

    stage.setOnCloseRequest(this::closeWindowRequest);
  }

  private void initializeTaskProgress() {
    taskProgress.setGraphicFactory(this::getGraphicForTask);
    //
    taskProgress.prefWidthProperty().bind(scrollPaneTaskProgress.widthProperty());
    taskProgress.prefHeightProperty().bind(scrollPaneTaskProgress.heightProperty());
    //
    uiDataService.runningTasksProperty().bind(
        EventStreams.sizeOf(taskProgress.getTasks()).toBinding(0));
    //
    EventStreams.valuesOf(uiDataService.cancelAllTasksProperty())
        .filter(shouldCancel -> shouldCancel)
        .subscribe(newValue -> Platform.runLater(() -> {
          taskProgress.getTasks().forEach(task -> Platform.runLater(() -> task.cancel(true)));
          uiDataService.cancelAllTasksProperty().set(false);
        }));
  }

  private void initializeKeyPressedHandler() {
    EventStreams.eventsOf(stage, KeyEvent.KEY_PRESSED)
        .map(KeyEvent::getCode)
        .filterMap(keyCode -> {
          switch (keyCode) {
            case DIGIT1:
            case DIGIT2:
            case DIGIT3:
            case DIGIT4:
            case DIGIT5:
            case DIGIT6:
              return true;
            default:
              return false;
          }
        }, keyCode -> Integer.parseInt(keyCode.getName()) - 1)
        .subscribe(keyCode -> tabPane.getSelectionModel().select(keyCode));
  }

  private void initializeTaskProgressListener() {
    final ObservableList<Task<?>> scheduledTasks = taskProgress.getTasks();

    EventStreams.changesOf(scheduledTasks).subscribe(change -> {
      if (scheduledTasks.isEmpty()) {
        removeTaskProgressBox();
      } else {
        progressVisibleProperty.set(true);
        bindProgressPropertyIfNecessary(scheduledTasks);
        setStatusBarText(scheduledTasks.size(), taskBoxCollapsed.get());
      }
    });

    EventStreams.merge(EventStreams.eventsOf(lbRunningTasks, MouseEvent.MOUSE_CLICKED),
        EventStreams.eventsOf(boxProgressBar, MouseEvent.MOUSE_CLICKED))
        .subscribe(mouseEvent -> {
          if (fadingInProgress) {
            mouseEvent.consume();
            return;
          }
          taskBoxCollapsed.setValue(!taskBoxCollapsed.get());
        });
  }

  /**
   * Set the {@link #mainStatusBar}'s text according to the amount of running tasks and whether the
   * side bar is collapsed or not.
   */
  @SuppressWarnings("unused")
  private void setStatusBarText(final int taskAmount, final boolean taskBoxCollapsed) {
    final String tasksSingular;
    final String tasksPlural;
    if (taskBoxCollapsed) {
      tasksSingular = resources.getString("tasksSingularCollapsed");
      tasksPlural = resources.getString("tasksPluralCollapsed");
    } else {
      tasksSingular = resources.getString("tasksSingular");
      tasksPlural = resources.getString("tasksPlural");
    }
    lbRunningTasks.setText(taskAmount + " " + ((taskAmount == 1) ? tasksSingular : tasksPlural));
  }

  /**
   * Bind the {@link #mainProgressBar progress bar's} ProgressProperty to the running task if there
   * is exactly one given. Otherwise the progress is just pending and not bound to any progress.
   */
  private void bindProgressPropertyIfNecessary(final ObservableList<Task<?>> scheduledTasks) {
    if (scheduledTasks.size() == 1) {
      mainProgressBar.progressProperty().bind(scheduledTasks.get(0).progressProperty());
    } else {
      mainProgressBar.progressProperty().bind(EMPTY_TASK.progressProperty());
    }
  }

  /**
   * Wait some time and hide the {@link #taskProgress task progress view} if there are no running
   * tasks anymore.
   */
  private void removeTaskProgressBox() {
    clearStatusBar();
    FxTimer.runLater(java.time.Duration.ofSeconds(3), () -> {
      if (taskProgress.getTasks().isEmpty()) {
        taskBoxCollapsed.setValue(true);
      }
    });
  }

  /**
   * Fade-in or fade-out the {@link #boxTaskProgress} by moving the {@link #mainSplitPaneDivider} to
   * the destination.
   */
  private void hideTaskProgressBox(final boolean hide) {
    if ((!hide || mainSplitPane.getItems().contains(boxTaskProgress)) && !fadingInProgress) {
      setStatusBarText(taskProgress.getTasks().size(), hide);
      disableDivider(hide);
      fadingInProgress = true;

      final Timeline timeline = new Timeline();
      final double destination = hide ? 1.0 : visibleDividerPos;

      final KeyValue dividerPosition =
          new KeyValue(mainSplitPaneDivider.positionProperty(), destination);

      timeline.getKeyFrames().add(new KeyFrame(Duration.millis(250), dividerPosition));
      timeline.setOnFinished(event -> fadingInProgress = false);

      Platform.runLater(timeline::play);
    }
  }

  /**
   * Lookup and disable the {@link #mainSplitPane split pane's} divider.
   */
  private void disableDivider(final boolean bool) {
    final Node divider = mainSplitPane.lookup("#mainSplitPane > .split-pane-divider");
    if (divider != null) {
      divider.setDisable(bool);
    }
  }

  private void clearStatusBar() {
    mainStatusBar.setText("");
    mainProgressBar.progressProperty().unbind();
    progressVisibleProperty.set(false);
  }

  /**
   * Open the reports view in a new tab within the {@link #tabPane}.
   */
  private void openReports() {
    if (!tabPane.getTabs().contains(reportsTab)) {
      reportsTab.setContent(reportsProvider.get());
      reportsTab.setOnClosed(event -> reportsProvider.get().dispose());
      tabPane.getTabs().add(reportsTab);
    }
    tabPane.getSelectionModel().select(reportsTab);
  }

  /**
   * Ask user for permission to close window using Alert. User can save database before closing.
   */
  private void closeWindowRequest(final Event event) {
    final Router router = routerProvider.get();
    if (!mainMenuService.isDatabaseChanged()) {
      router.transitionTo(RouteNames.SHUTDOWN);
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
      mainMenuBar.saveFile();
      router.transitionTo(RouteNames.SHUTDOWN);
    } else if ((result == saveAs && !mainMenuBar.saveFileAs()) || result == cancel) {
      // if the result is to cancel or 'save as' has been canceled we ignore the close request and
      // consume the event, otherwise we close the stage
      event.consume();
    } else {
      router.transitionTo(RouteNames.SHUTDOWN);
    }
  }

  @Override
  public void activateController(final RouteNames route, final Object... args) {
    if (RouteNames.OPEN_REPORTS.equals(route)) {
      openReports();
    } else if (RouteNames.CLOSE_APP.equals(route)) {
      if (args.length == 0) {
        return;
      }
      closeWindowRequest((Event) args[0]);
    }
  }
}
