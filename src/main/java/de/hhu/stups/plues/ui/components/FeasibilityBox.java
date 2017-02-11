package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
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

  private String impossibleCourseString;
  private String noConflictString;
  private ResultState resultState;

  private SolverTask<Set<Integer>> unsatCoreTask;
  private SolverTask<Boolean> feasibilityTask;
  private final ObjectProperty<ObservableList<Actions>> cbActionItemsProperty;
  private final StringProperty errorMsgProperty;
  private final ExecutorService executorService;
  private final Delayed<SolverService> delayedSolverService;
  private final Delayed<Store> delayedStore;
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
                        final Delayed<Store> delayedStore,
                        final Delayed<SolverService> delayedSolverService,
                        final ExecutorService executorService,
                        final Provider<ConflictTree> conflictTreeProvider,
                        final UiDataService uiDataService,
                        @Assisted("major") final Course majorCourse,
                        @Nullable @Assisted("minor") final Course minorCourse,
                        @Assisted final ListView<FeasibilityBox> parent) {
    super();
    this.delayedSolverService = delayedSolverService;
    this.delayedStore = delayedStore;
    this.router = router;
    this.executorService = executorService;
    this.conflictTreeProvider = conflictTreeProvider;
    this.impossibleCourses = uiDataService.getImpossibleCoures();
    this.parent = parent;

    major = majorCourse;
    minor = minorCourse;
    cbActionItemsProperty = new SimpleObjectProperty<>();
    errorMsgProperty = new SimpleStringProperty();

    inflater.inflate("components/FeasibilityBox", this, this, "feasibilityBox");
  }

  @Override
  public final void initialize(final URL location, final ResourceBundle resources) {
    impossibleCourseString = resources.getString("impossibleCourse");
    noConflictString = resources.getString("noConflict");

    lbErrorMsg.textProperty().bind(errorMsgProperty);

    initializeCourseLabels();
    initializeActionCheckBox(resources);

    delayedSolverService.whenAvailable(solver -> {
      initFeasibilityTask();
      executorService.submit(feasibilityTask);
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

  private void initializeActionCheckBox(ResourceBundle resources) {
    cbAction.setConverter(new ActionsStringConverter(resources));
    cbAction.itemsProperty().bind(cbActionItemsProperty);
    cbActionItemsProperty.addListener((observable, oldValue, newValue) ->
        cbAction.getSelectionModel().selectFirst());
  }

  private void initFeasibilityTask() {
    feasibilityTask = buildFeasibilityTask();

    feasibilityTask.setOnFailed(event -> {
      cbActionItemsProperty.setValue(getActionsForInfeasibleCourse(feasibilityTask.getReason()));
      resultState = ResultState.FAILED;
    });

    feasibilityTask.setOnSucceeded(this::feasibilityCoreTaskSucceeded);

    feasibilityTask.setOnCancelled(event -> {
      cbActionItemsProperty.setValue(cancelledActions);
      resultState = ResultState.FAILED;
    });

    feasibilityTask.setOnScheduled(event -> cbActionItemsProperty.setValue(scheduledActions));

    progressIndicator.setStyle("-fx-progress-color: " + TaskStateColor.WORKING.getColor());
    progressIndicator.visibleProperty().bind(feasibilityTask.runningProperty());

    lbIcon.visibleProperty().bind(feasibilityTask.runningProperty().not());
    lbIcon.graphicProperty().bind(TaskBindings.getIconBinding(ICON_SIZE, feasibilityTask));
    lbIcon.styleProperty().bind(TaskBindings.getStyleBinding(feasibilityTask));
  }

  @FXML
  @SuppressWarnings("unused")
  private void submitAction() {
    final Actions selectedItem = cbAction.getSelectionModel().getSelectedItem();

    if (selectedItem == null) {
      return;
    }

    switch (selectedItem) {
      case OPEN_IN_TIMETABLE:
        router.transitionTo(RouteNames.TIMETABLE, new Course[] {this.major, this.minor},
              resultState);
        break;
      case RESTART_COMPUTATION:
        initFeasibilityTask();
        executorService.submit(feasibilityTask);
        break;
      case GENERATE_PDF:
        router.transitionTo(RouteNames.PDF_TIMETABLES, this.major, this.minor);
        break;
      case GENERATE_PARTIAL:
        router.transitionTo(RouteNames.PARTIAL_TIMETABLES, this.major, this.minor);
        break;
      case STEPWISE_UNSAT_CORE:
        stepwiseUnsatCoreAction();
        break;
      case REMOVE:
        parent.getItems().remove(this);
        break;
      case UNSAT_CORE:
        initUnsatCoreTask();
        break;
      case CANCEL:
        cancelAction();
        break;
      default:
        break;
    }
  }

  private void stepwiseUnsatCoreAction() {
    if (this.minor != null) {
      router.transitionTo(RouteNames.UNSAT_CORE, this.major, this.minor);
    } else {
      router.transitionTo(RouteNames.UNSAT_CORE, this.major);
    }
  }

  private void cancelAction() {
    if (feasibilityTask.isRunning()) {
      interrupt();
      cbActionItemsProperty.setValue(cancelledActions);
    } else if (unsatCoreTask.isRunning()) {
      unsatCoreTask.cancel(true);
    }
  }

  /**
   * Initialize and submit the {@link #unsatCoreTask task} to compute the unsat core and set all its
   * necessary listeners. If the task succeeded the {@link ConflictTree} is dynamically added to the
   * {@link this FeasibilityBox}. Otherwise the unsat core computation failed and an error message
   * is shown to the user.
   */
  private void initUnsatCoreTask() {
    unsatCoreTask = buildUnsatCoreTask();

    unsatCoreTask.setOnSucceeded(this::unsatCoreTaskSucceeded);

    unsatCoreTask.setOnCancelled(unsatCore -> {
      if (ResourceBundle.getBundle("lang.tasks").getString("timeout")
          .equals(unsatCoreTask.getReason())) {
        errorMsgProperty.setValue(noConflictString);
      }
      cbActionItemsProperty.setValue(failedWithConflictActions);
    });

    unsatCoreTask.setOnFailed(unsatCore -> {
      errorMsgProperty.setValue(noConflictString);
      cbActionItemsProperty.setValue(conflictActions);
    });

    unsatCoreTask.setOnScheduled(unsatCore -> cbActionItemsProperty.setValue(scheduledActions));

    executorService.submit(unsatCoreTask);
  }

  @SuppressWarnings("unused")
  private void feasibilityCoreTaskSucceeded(WorkerStateEvent event) {
    Platform.runLater(() -> {
      final ObservableList<Actions> actions;
      boolean feasible = feasibilityTask.getValue();

      if (feasible) {
        actions = getActionsForFeasibleCourse();
      } else {
        actions = getActionsForInfeasibleCourse("");
      }

      cbActionItemsProperty.setValue(actions);
      resultState = ResultState.SUCCEEDED;
    });
  }

  private ObservableList<Actions> getActionsForFeasibleCourse() {
    if (this.minor != null) {
      return succeededActionsMajorMinor;
    }
    return succeededActionsMajorOnly;
  }

  @SuppressWarnings("unused")
  private void unsatCoreTaskSucceeded(WorkerStateEvent event) {
    Platform.runLater(() -> {
      unsatCoreProperty.set(FXCollections.observableArrayList(unsatCoreTask.getValue()));

      final ConflictTree conflictTree = conflictTreeProvider.get();
      conflictTree.setUnsatCoreProperty(unsatCoreProperty, delayedStore.get());
      getChildren().add(conflictTree);

      cbActionItemsProperty.setValue(conflictActions);
    });
  }

  private SolverTask<Set<Integer>> buildUnsatCoreTask() {
    if (this.minor != null) {
      return delayedSolverService.get().unsatCore(this.major, this.minor);
    }
    return delayedSolverService.get().unsatCore(this.major);
  }

  private SolverTask<Boolean> buildFeasibilityTask() {
    if (this.minor != null) {
      return delayedSolverService.get().checkFeasibilityTask(this.major, this.minor);
    }
    return delayedSolverService.get().checkFeasibilityTask(this.major);
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
    feasibilityTask.cancel();
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
}
