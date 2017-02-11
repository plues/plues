package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.ResultState;
import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.TaskBindings;
import de.hhu.stups.plues.ui.TaskStateColor;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nullable;


@SuppressWarnings("WeakerAccess")
public class FeasibilityBox extends VBox implements Initializable {

  private final Provider<ConflictTree> conflictTreeProvider;
  private static final String ICON_SIZE = "50";

  private final Course major;
  private final Course minor;
  private final Course[] courses;

  private String impossibleCourseString;
  private String noConflictString;
  private ResultState resultState;

  private UnsatCoreTaskManager unsatCoreTaskManager;
  private FeasibilityTaskManager feasibilityTaskManager;

  private final ObjectProperty<ObservableList<Actions>> cbActionItemsProperty;
  private final StringProperty errorMsgProperty;
  private final ExecutorService executorService;
  private final Delayed<SolverService> delayedSolverService;
  private final Set<Course> impossibleCourses;
  private final Router router;

  private final ListView<FeasibilityBox> parent;
  private final ListProperty<Integer> unsatCoreProperty = new SimpleListProperty<>();

  // lists of actions for each possible state
  private final ObservableList<Actions> succeededActionsMajorMinor
      = FXCollections.observableArrayList(Actions.OPEN_IN_TIMETABLE,
                                          Actions.GENERATE_PDF,
                                          Actions.GENERATE_PARTIAL,
                                          Actions.REMOVE);

  private final ObservableList<Actions> succeededActionsMajorOnly
      = FXCollections.observableArrayList(Actions.OPEN_IN_TIMETABLE,
                                          Actions.REMOVE);

  private final ObservableList<Actions> failedWithConflictActions
      = FXCollections.observableArrayList(Actions.UNSAT_CORE,
                                          Actions.OPEN_IN_TIMETABLE,
                                          Actions.STEPWISE_UNSAT_CORE,
                                          Actions.REMOVE);

  private final ObservableList<Actions> conflictActions
      = FXCollections.observableArrayList(Actions.OPEN_IN_TIMETABLE,
                                          Actions.STEPWISE_UNSAT_CORE,
                                          Actions.REMOVE);

  private final ObservableList<Actions> cancelledActions
      = FXCollections.observableArrayList(Actions.OPEN_IN_TIMETABLE,
                                          Actions.RESTART_COMPUTATION,
                                          Actions.REMOVE);

  private final ObservableList<Actions> scheduledActions
      = FXCollections.observableArrayList(Actions.CANCEL);

  private final ObservableList<Actions> impossibleActions
      = FXCollections.observableArrayList(Actions.REMOVE);

  private final ObservableList<Actions> timeoutActions
      = FXCollections.observableArrayList(Actions.OPEN_IN_TIMETABLE,
                                          Actions.RESTART_COMPUTATION,
                                          Actions.REMOVE);

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
   * A container to display the feasibility of a combination of courses or a single one. For
   * infeasible courses it is possible to compute the unsat core which is presented in a {@link
   * ConflictTree TreeView}.
   */
  @Inject
  public FeasibilityBox(final Inflater inflater,
                        final Router router,
                        final Delayed<SolverService> delayedSolverService,
                        final ExecutorService executorService,
                        final Provider<ConflictTree> conflictTreeProvider,
                        final UiDataService uiDataService,
                        @Assisted("major") final Course majorCourse,
                        @Nullable @Assisted("minor") final Course minorCourse,
                        @Assisted final ListView<FeasibilityBox> parent) {
    super();
    this.delayedSolverService = delayedSolverService;
    this.router = router;
    this.executorService = executorService;
    this.conflictTreeProvider = conflictTreeProvider;
    this.impossibleCourses = uiDataService.getImpossibleCoures();
    this.parent = parent;

    major = majorCourse;
    minor = minorCourse;
    courses = buildCourses(majorCourse, minorCourse);

    cbActionItemsProperty = new SimpleObjectProperty<>();
    errorMsgProperty = new SimpleStringProperty();

    inflater.inflate("components/FeasibilityBox", this, this, "feasibilityBox");
  }

  private Course[] buildCourses(Course major, Course minor) {
    if (minor == null) {
      return new Course[] {major};
    }
    return new Course[] {major, minor};
  }

  @Override
  public final void initialize(final URL location, final ResourceBundle resources) {
    impossibleCourseString = resources.getString("impossibleCourse");
    noConflictString = resources.getString("noConflict");

    lbErrorMsg.textProperty().bind(errorMsgProperty);

    initializeCourseLabels();
    initializeActionCheckBox(resources);

    restartComputationAction();
  }

  private void initializeCourseLabels() {
    lbMajor.setText(major.getFullName());
    if (this.minor != null) {
      lbMinor.setText(minor.getFullName());
    } else {
      lbMinor.setText("");
    }
  }

  private void initializeActionCheckBox(ResourceBundle resources) {
    cbAction.setConverter(new ActionsStringConverter(resources));
    cbAction.itemsProperty().bind(cbActionItemsProperty);
    cbActionItemsProperty.addListener((observable, oldValue, newValue) ->
        cbAction.getSelectionModel().selectFirst());
  }

  @FXML
  private void submitAction() {
    final Actions selectedItem = cbAction.getSelectionModel().getSelectedItem();

    if (selectedItem == null) {
      return;
    }
    performAction(selectedItem);
  }

  @SuppressWarnings("RedundantCast")
  private void performAction(Actions selectedItem) {
    switch (selectedItem) {
      case OPEN_IN_TIMETABLE:
        router.transitionTo(RouteNames.TIMETABLE, courses, resultState);
        break;
      case RESTART_COMPUTATION:
        restartComputationAction();
        break;
      case GENERATE_PDF:
        transitionAction(RouteNames.PDF_TIMETABLES);
        break;
      case GENERATE_PARTIAL:
        transitionAction(RouteNames.PARTIAL_TIMETABLES);
        break;
      case STEPWISE_UNSAT_CORE:
        transitionAction(RouteNames.UNSAT_CORE);
        break;
      case REMOVE:
        parent.getItems().remove(this);
        break;
      case UNSAT_CORE:
        unsatCoreAction();
        break;
      case CANCEL:
        cancelAction();
        break;
      default:
        break;
    }
  }

  private void transitionAction(RouteNames route) {
    if (minor == null) {
      router.transitionTo(route, major);
    } else {
      router.transitionTo(route, major, minor);
    }
  }

  private void unsatCoreAction() {
    delayedSolverService.whenAvailable(solverService -> {
      unsatCoreTaskManager = new UnsatCoreTaskManager(solverService);
      unsatCoreTaskManager.start();
    });
  }

  private void restartComputationAction() {
    delayedSolverService.whenAvailable(solverService -> {
      feasibilityTaskManager = new FeasibilityTaskManager(solverService);
      feasibilityTaskManager.start();
    });
  }

  private void cancelAction() {
    if (feasibilityTaskManager.isRunning()) {
      interrupt();
      cbActionItemsProperty.setValue(cancelledActions);
    } else if (unsatCoreTaskManager.isRunning()) {
      unsatCoreTaskManager.cancel(true);
    }
  }



  private ObservableList<Actions> getActionsForFeasibleCourse() {
    if (this.minor != null) {
      return succeededActionsMajorMinor;
    }
    return succeededActionsMajorOnly;
  }

  /**
   * Get the strings of the actions in {@link #cbAction} for infeasible courses, i.e. compute the
   * unsat core if the course is not impossible or the combination does not contain an impossible
   * course. Otherwise just offer the possibility to remove the feasibility box.
   */
  private ObservableList<Actions> getActionsForInfeasibleCourse(final String reason) {
    if (impossibleCourses.contains(this.major)
        || (this.minor != null && impossibleCourses.contains(this.minor))) {
      errorMsgProperty.setValue(impossibleCourseString);
      return impossibleActions;
    } else if (ResourceBundle.getBundle("lang.tasks").getString("timeout").equals(reason)) {
      return timeoutActions;
    } else {
      return failedWithConflictActions;
    }
  }

  @FXML
  private void interrupt() {
    feasibilityTaskManager.cancel();
  }

  private enum Actions {

    OPEN_IN_TIMETABLE("openInTimetable"),
    RESTART_COMPUTATION("restartComputation"),
    GENERATE_PDF("generatePDF"),
    GENERATE_PARTIAL("generatePartial"),
    STEPWISE_UNSAT_CORE("stepwiseUnsatCore"),
    REMOVE("remove"),
    UNSAT_CORE("unsatCore"),
    CANCEL("cancel");

    private final String key;

    Actions(String key) {
      this.key = key;
    }

    public String getKey() {
      return key;
    }

  }

  private static class ActionsStringConverter extends StringConverter<Actions> {
    private final ResourceBundle resources;

    public ActionsStringConverter(ResourceBundle resources) {
      this.resources = resources;
    }

    @Override
    public String toString(Actions value) {
      return resources.getString(value.getKey());
    }

    @Override
    public Actions fromString(String string) {
      throw new IllegalAccessError("not supported");
    }
  }

  /**
   * Initialize and submit the {@link #unsatCoreTask task} to compute the unsat core and set all its
   * necessary listeners. If the task succeeded the {@link ConflictTree} is dynamically added to the
   * {@link this FeasibilityBox}. Otherwise the unsat core computation failed and an error message
   * is shown to the user.
   */
  private class UnsatCoreTaskManager {
    private final SolverTask<Set<Integer>> unsatCoreTask;

    private UnsatCoreTaskManager(SolverService solverService) {
      this.unsatCoreTask = solverService.unsatCore(courses);

      unsatCoreTask.setOnSucceeded(this::taskSucceeded);
      unsatCoreTask.setOnCancelled(this::taskCancelled);
      unsatCoreTask.setOnFailed(this::taskFailed);
      unsatCoreTask.setOnScheduled(this::taskScheduled);
    }

    @SuppressWarnings("unused")
    private void taskScheduled(WorkerStateEvent workerStateEvent) {
      cbActionItemsProperty.setValue(scheduledActions);
    }

    @SuppressWarnings("unused")
    private void taskFailed(WorkerStateEvent workerStateEvent) {
      errorMsgProperty.setValue(noConflictString);
      cbActionItemsProperty.setValue(conflictActions);
    }

    @SuppressWarnings("unused")
    private void taskCancelled(WorkerStateEvent workerStateEvent) {
      if (ResourceBundle.getBundle("lang.tasks").getString("timeout")
            .equals(unsatCoreTask.getReason())) {
        errorMsgProperty.setValue(noConflictString);
      }
      cbActionItemsProperty.setValue(failedWithConflictActions);
    }

    @SuppressWarnings("unused")
    private void taskSucceeded(WorkerStateEvent workerStateEvent) {
      Platform.runLater(() -> {
        unsatCoreProperty.set(FXCollections.observableArrayList(unsatCoreTask.getValue()));

        final ConflictTree conflictTree = conflictTreeProvider.get();
        conflictTree.setUnsatCoreProperty(unsatCoreProperty);
        getChildren().add(conflictTree);

        cbActionItemsProperty.setValue(conflictActions);
      });
    }

    public boolean isRunning() {
      return (this.unsatCoreTask != null && this.unsatCoreTask.isRunning());
    }

    public void cancel(boolean mayInterruptIfRunning) {
      if (this.unsatCoreTask == null) {
        return;
      }
      this.unsatCoreTask.cancel(mayInterruptIfRunning);
    }

    public void start() {
      executorService.submit(unsatCoreTask);
    }
  }

  private class FeasibilityTaskManager {
    private final SolverTask<Boolean> feasibilityTask;

    private FeasibilityTaskManager(SolverService solverService) {
      feasibilityTask = solverService.checkFeasibilityTask(courses);

      feasibilityTask.setOnFailed(this::taskFailed);
      feasibilityTask.setOnSucceeded(this::taskSucceeded);
      feasibilityTask.setOnCancelled(this::taskCancelled);
      feasibilityTask.setOnScheduled(this::taskScheduled);

      setupBindings();
    }

    private void setupBindings() {
      progressIndicator.setStyle("-fx-progress-color: " + TaskStateColor.WORKING.getColor());
      progressIndicator.visibleProperty().bind(feasibilityTask.runningProperty());

      lbIcon.visibleProperty().bind(feasibilityTask.runningProperty().not());
      lbIcon.graphicProperty().bind(TaskBindings.getIconBinding(ICON_SIZE, feasibilityTask));
      lbIcon.styleProperty().bind(TaskBindings.getStyleBinding(feasibilityTask));
    }

    @SuppressWarnings("unused")
    private void taskFailed(WorkerStateEvent workerStateEvent) {
      cbActionItemsProperty.setValue(getActionsForInfeasibleCourse(feasibilityTask.getReason()));
      resultState = ResultState.FAILED;
    }

    @SuppressWarnings("unused")
    private void taskScheduled(WorkerStateEvent workerStateEvent) {
      cbActionItemsProperty.setValue(scheduledActions);
    }

    @SuppressWarnings("unused")
    private void taskCancelled(WorkerStateEvent workerStateEvent) {
      cbActionItemsProperty.setValue(cancelledActions);
      resultState = ResultState.FAILED;
    }

    @SuppressWarnings("unused")
    private void taskSucceeded(WorkerStateEvent event) {
      Platform.runLater(() -> {
        final ObservableList<Actions> actions;
        boolean feasible = (boolean) event.getSource().getValue();

        if (feasible) {
          actions = getActionsForFeasibleCourse();
        } else {
          actions = getActionsForInfeasibleCourse("");
        }

        cbActionItemsProperty.setValue(actions);
        resultState = ResultState.SUCCEEDED;
      });
    }

    public void start() {
      executorService.submit(feasibilityTask);
    }

    public boolean isRunning() {
      return feasibilityTask != null && feasibilityTask.isRunning();
    }

    public void cancel() {
      if (feasibilityTask == null) {
        return;
      }
      feasibilityTask.cancel();
    }
  }
}
