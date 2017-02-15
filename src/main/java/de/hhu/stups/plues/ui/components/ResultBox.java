package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.prob.ResultState;
import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.hhu.stups.plues.tasks.PdfRenderingTaskFactory;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.TaskBindings;
import de.hhu.stups.plues.ui.controller.PdfRenderingHelper;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ListBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.net.URL;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nullable;

public class ResultBox extends VBox implements Initializable {

  private static final String WORKING_COLOR = "#BDE5F8";
  private static final String ICON_SIZE = "50";
  private final Course major;
  private final Course minor;

  private PdfRenderingTask task;
  private ResultState resultState;

  private final Router router;
  private final ExecutorService executorService;
  private final ListView<ResultBox> parent;
  private final Delayed<SolverService> delayedSolverService;
  private final PdfRenderingTaskFactory renderingTaskFactory;

  private final StringProperty errorMsgProperty = new SimpleStringProperty();
  private final ObjectProperty<Path> pdf = new SimpleObjectProperty<>();

  // lists of actions for each possible state
  private static final ObservableList<Actions> succeededActions
      = FXCollections.observableArrayList(Actions.SHOW,
                                          Actions.SAVE_AS,
                                          Actions.OPEN_IN_TIMETABLE,
                                          Actions.GENERATE_PARTIAL,
                                          Actions.REMOVE);

  private static final ObservableList<Actions> failedActions
      = FXCollections.observableArrayList(Actions.OPEN_IN_TIMETABLE, Actions.REMOVE);

  private static final ObservableList<Actions> cancelledActions
      = FXCollections.observableArrayList(Actions.OPEN_IN_TIMETABLE,
                                          Actions.RESTART_COMPUTATION,
                                          Actions.REMOVE);

  private static final ObservableList<Actions> scheduledActions
      = FXCollections.observableArrayList(Actions.CANCEL);


  @FXML
  @SuppressWarnings("unused")
  private ProgressIndicator progressIndicator;
  @FXML
  @SuppressWarnings("unused")
  private Label lbIcon;
  @FXML
  @SuppressWarnings("unused")
  private Label lbMajor;
  @FXML
  @SuppressWarnings("unused")
  private Label lbMinor;
  @FXML
  @SuppressWarnings("unused")
  private Label lbErrorMsg;
  @FXML
  @SuppressWarnings("unused")
  private ComboBox<Actions> cbAction;

  /**
   * Constructor for ResultBox.
   *
   * @param inflater             Inflater to handle fxml loader tasks
   * @param renderingTaskFactory PDF Rendering task Factory
   * @param major                Major course
   * @param minor                Minor course if present, else null
   * @param parent               The parent wrapper (VBox) to remove a single result box.
   */
  @Inject
  @SuppressWarnings("WeakerAccess")
  public ResultBox(final Inflater inflater,
                   final Router router,
                   final Delayed<SolverService> delayedSolverService,
                   final PdfRenderingTaskFactory renderingTaskFactory,
                   final ExecutorService executorService,
                   @Assisted("major") final Course major,
                   @Nullable @Assisted("minor") final Course minor,
                   @Assisted("parent") final ListView<ResultBox> parent) {
    super();
    this.router = router;
    this.parent = parent;
    this.delayedSolverService = delayedSolverService;
    this.executorService = executorService;
    this.renderingTaskFactory = renderingTaskFactory;

    this.major = major;
    this.minor = minor;
    inflater.inflate("components/Resultbox", this, this, "resultbox");
  }

  private Course[] buildCourses(final Course major, final Course minor) {
    if (minor == null) {
      return new Course[] {major};
    }
    return new Course[] {major, minor};
  }

  @Override
  public final void initialize(final URL location, final ResourceBundle resources) {
    initializeCourseLabels();

    runSolverTask();

    progressIndicator.setStyle(" -fx-progress-color: " + WORKING_COLOR);

    lbErrorMsg.textProperty().bind(
        Bindings.createStringBinding(
            () -> resources.getString(errorMsgProperty.get()),
            errorMsgProperty));

    cbAction.setConverter(new ActionsStringConverter(resources));
    cbAction.itemsProperty().addListener((observable, oldValue, newValue) ->
        cbAction.getSelectionModel().selectFirst());
  }

  private void runSolverTask() {
    delayedSolverService.whenAvailable(solver -> {
      initSolverTask(solver);
      executorService.submit(task);
    });
  }

  private void initializeCourseLabels() {
    lbMajor.setText(major.getFullName());
    if (this.minor != null) {
      lbMinor.setText(minor.getFullName());
    } else {
      lbMinor.setText("");
    }
  }

  private void initSolverTask(final SolverService solverService) {
    final SolverTask<FeasibilityResult> solverTask;

    solverTask = solverService.computeFeasibilityTask(buildCourses(major, minor));
    task = renderingTaskFactory.create(major, minor, solverTask);

    task.setOnSucceeded(event -> Platform.runLater(() -> {
      pdf.set((Path) event.getSource().getValue());
      resultState = ResultState.SUCCEEDED;
    }));

    task.setOnFailed(event -> {
      errorMsgProperty.set("error_gen");
      resultState = ResultState.FAILED;
    });

    task.setOnCancelled(event -> resultState = ResultState.FAILED);

    taskBindings();
  }

  private void taskBindings() {
    progressIndicator.visibleProperty().bind(task.runningProperty());
    //
    cbAction.itemsProperty().unbind();
    cbAction.itemsProperty().bind(new ActionsBinding(task.stateProperty()));
    //
    lbIcon.visibleProperty().bind(task.runningProperty().not());
    lbIcon.graphicProperty().bind(TaskBindings.getIconBinding(ICON_SIZE, task));
    lbIcon.styleProperty().bind(TaskBindings.getStyleBinding(task));
  }

  @FXML
  @SuppressWarnings("unused")
  private void submitAction() {
    final Actions selectedItem = cbAction.getSelectionModel().getSelectedItem();

    switch (selectedItem) {
      case SHOW:
        showPdf();
        break;
      case SAVE_AS:
        savePdf();
        break;
      case GENERATE_PARTIAL:
        generatePartialAction();
        break;
      case OPEN_IN_TIMETABLE:
        router.transitionTo(RouteNames.TIMETABLE, buildCourses(major, minor) , resultState);
        break;
      case RESTART_COMPUTATION:
        runSolverTask();
        break;
      case REMOVE:
        parent.getItems().remove(this);
        break;
      case CANCEL:
        interrupt();
        break;
      default:
        throw new IllegalArgumentException("Unexpected enum value");
    }
  }

  private void generatePartialAction() {
    if (minor == null) {
      router.transitionTo(RouteNames.PARTIAL_TIMETABLES, major);
    } else {
      router.transitionTo(RouteNames.PARTIAL_TIMETABLES, major, minor);
    }
  }

  @FXML
  private void showPdf() {
    PdfRenderingHelper.showPdf(pdf.get(), e -> errorMsgProperty.set("error_temp"));
  }

  @FXML
  private void savePdf() {
    PdfRenderingHelper.savePdf(pdf.get(), major, minor, lbErrorMsg);
  }

  @FXML
  private void interrupt() {
    task.cancel();
  }

  private enum Actions {

    CANCEL("cancel"),
    GENERATE_PARTIAL("generatePartial"),
    OPEN_IN_TIMETABLE("openInTimetable"),
    REMOVE("remove"),
    RESTART_COMPUTATION("restartComputation"),
    SAVE_AS("saveAs"),
    SHOW("show");

    private final String key;

    Actions(final String key) {
      this.key = key;
    }

    public String getKey() {
      return key;
    }

  }

  private static class ActionsStringConverter extends StringConverter<Actions> {
    private final ResourceBundle resources;

    ActionsStringConverter(final ResourceBundle resources) {
      this.resources = resources;
    }

    @Override
    public String toString(final Actions value) {
      return resources.getString(value.getKey());
    }

    @Override
    public Actions fromString(final String string) {
      throw new IllegalAccessError("not supported");
    }
  }

  private static class ActionsBinding extends ListBinding<Actions> {
    private final ReadOnlyObjectProperty<Worker.State> property;

    ActionsBinding(final ReadOnlyObjectProperty<Worker.State> property) {
      this.property = property;
      bind(property);
    }

    @Override
    protected ObservableList<Actions> computeValue() {
      final Worker.State state = property.get();
      switch (state) {
        case SUCCEEDED:
          return succeededActions;
        case CANCELLED:
          return cancelledActions;
        case FAILED:
          return failedActions;
        case READY:
        case RUNNING:
        case SCHEDULED:
        default:
          return scheduledActions;
      }

    }
  }
}
