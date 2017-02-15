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
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ListBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleListProperty;
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
import java.util.Arrays;
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

  private SolverTask<?> solverTask;

  private final ExecutorService executorService;
  private final Delayed<SolverService> delayedSolverService;
  private final Set<Course> impossibleCourses;
  private final Router router;

  private final ListView<FeasibilityBox> parent;
  private final ListProperty<Integer> unsatCoreProperty = new SimpleListProperty<>();

  // lists of actions for each possible state
  private static final ObservableList<Actions> succeededActionsMajorMinor
      = FXCollections.observableArrayList(Actions.OPEN_IN_TIMETABLE,
                                          Actions.GENERATE_PDF,
                                          Actions.GENERATE_PARTIAL,
                                          Actions.REMOVE);

  private static final ObservableList<Actions> succeededActionsMajorOnly
      = FXCollections.observableArrayList(Actions.OPEN_IN_TIMETABLE,
                                          Actions.REMOVE);

  private static final ObservableList<Actions> failedWithConflictActions
      = FXCollections.observableArrayList(Actions.UNSAT_CORE,
                                          Actions.OPEN_IN_TIMETABLE,
                                          Actions.STEPWISE_UNSAT_CORE,
                                          Actions.REMOVE);

  private static final ObservableList<Actions> conflictActions
      = FXCollections.observableArrayList(Actions.OPEN_IN_TIMETABLE,
                                          Actions.STEPWISE_UNSAT_CORE,
                                          Actions.REMOVE);

  private static final ObservableList<Actions> cancelledActions
      = FXCollections.observableArrayList(Actions.OPEN_IN_TIMETABLE,
                                          Actions.RESTART_COMPUTATION,
                                          Actions.REMOVE);

  private static final ObservableList<Actions> scheduledActions
      = FXCollections.observableArrayList(Actions.CANCEL);

  private static final ObservableList<Actions> impossibleActions
      = FXCollections.observableArrayList(Actions.REMOVE);

  private static final ObservableList<Actions> timeoutActions
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

    inflater.inflate("components/FeasibilityBox", this, this, "feasibilityBox");
  }

  private Course[] buildCourses(final Course major, final Course minor) {
    if (minor == null) {
      return new Course[] {major};
    }
    return new Course[] {major, minor};
  }

  @Override
  public final void initialize(final URL location, final ResourceBundle resources) {
    impossibleCourseString = resources.getString("impossibleCourse");
    noConflictString = resources.getString("noConflict");

    initializeCourseLabels();
    initializeActionComboBox(resources);

    progressIndicator.setStyle("-fx-progress-color: " + TaskStateColor.WORKING.getColor());

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

  private void initializeActionComboBox(final ResourceBundle resources) {
    cbAction.setConverter(new ActionsStringConverter(resources));
    cbAction.itemsProperty().addListener((observable, oldValue, newValue) ->
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

  private void performAction(final Actions selectedItem) {
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
        interrupt();
        break;
      default:
        throw new IllegalArgumentException("Unexpected enum value");
    }
  }

  private void transitionAction(final RouteNames route) {
    if (minor == null) {
      router.transitionTo(route, major);
    } else {
      router.transitionTo(route, major, minor);
    }
  }

  /**
   * Initialize and submit the {@link #solverTask task} to compute the unsat core and set all its
   * necessary listeners. If the task succeeded the {@link ConflictTree} is dynamically added to the
   * {@link this FeasibilityBox}. Otherwise the unsat core computation failed and an error message
   * is shown to the user.
   */
  private void unsatCoreAction() {
    delayedSolverService.whenAvailable(solverService -> {
      final SolverTask<Set<Integer>> task = solverService.unsatCore(courses);
      task.setOnSucceeded(event -> Platform.runLater(() -> showConflictTree(task.getValue())));

      lbErrorMsg.textProperty().bind(this.getErrorMsgBinding(task));

      cbAction.itemsProperty().unbind();
      cbAction.itemsProperty().bind(new UnsatCoreActionsBinding(task.stateProperty()));

      this.solverTask = task;
      executorService.submit(task);
    });
  }

  private StringBinding getErrorMsgBinding(final SolverTask<?> task) {
    return Bindings.createStringBinding(() -> {
      switch (task.getState()) {
        case CANCELLED:
          if (ResourceBundle.getBundle("lang.tasks").getString("timeout")
                .equals(task.getReason())) {
            return noConflictString;
          }
          return "";
        case FAILED:
          return noConflictString;
        default:
          return "";
      }
    }, task.stateProperty());
  }

  private void showConflictTree(final Set<Integer> value) {
    unsatCoreProperty.set(FXCollections.observableArrayList(value));
    final ConflictTree conflictTree = conflictTreeProvider.get();

    conflictTree.setUnsatCoreProperty(unsatCoreProperty);
    getChildren().add(conflictTree);
  }

  private void restartComputationAction() {
    delayedSolverService.whenAvailable(solverService -> {
      final SolverTask<Boolean> task = solverService.checkFeasibilityTask(courses);
      feasibilityTaskBindings(task);

      cbAction.itemsProperty().unbind();
      cbAction.itemsProperty().bind(new FeasibilityActionsBinding(task));

      this.solverTask = task;
      executorService.submit(task);
    });
  }

  private void feasibilityTaskBindings(final SolverTask<Boolean> task) {
    task.setOnSucceeded(event -> resultState = ResultState.SUCCEEDED);
    task.setOnFailed(event -> {
      if (impossibleCourses.containsAll(Arrays.asList(courses))) {
        lbErrorMsg.textProperty().setValue(impossibleCourseString);
      }
      resultState = ResultState.FAILED;
    });
    task.setOnCancelled(event -> resultState = ResultState.FAILED);

    progressIndicator.visibleProperty().bind(task.runningProperty());
    lbIcon.visibleProperty().bind(task.runningProperty().not());
    lbIcon.graphicProperty().bind(TaskBindings.getIconBinding(ICON_SIZE, task));
    lbIcon.styleProperty().bind(TaskBindings.getStyleBinding(task));
  }

  @FXML
  private void interrupt() {
    if (solverTask == null) {
      return;
    }
    solverTask.cancel(true);
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

    Actions(final String key) {
      this.key = key;
    }

    public String getKey() {
      return key;
    }

  }

  private static class ActionsStringConverter extends StringConverter<Actions> {
    private final ResourceBundle resources;

    public ActionsStringConverter(final ResourceBundle resources) {
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

  private static class UnsatCoreActionsBinding extends ListBinding<Actions> {
    public final ReadOnlyObjectProperty<Worker.State> observable;

    private UnsatCoreActionsBinding(final ReadOnlyObjectProperty<Worker.State> workerState) {
      observable = workerState;
      bind(observable);
    }

    @Override
    protected ObservableList<Actions> computeValue() {
      switch (observable.get()) {
        case READY:
        case SCHEDULED:
        case RUNNING:
          return scheduledActions;
        case SUCCEEDED:
        case FAILED:
          return conflictActions;
        case CANCELLED:
        default:
          return failedWithConflictActions;
      }
    }
  }

  private class FeasibilityActionsBinding extends ListBinding<Actions> {
    private final SolverTask<Boolean> task;

    private FeasibilityActionsBinding(final SolverTask<Boolean> task) {
      this.task = task;
      bind(task.stateProperty());
    }

    @Override
    protected ObservableList<Actions> computeValue() {
      switch (task.getState()) {
        case READY:
        case RUNNING:
        case SCHEDULED:
          return scheduledActions;
        case SUCCEEDED:
          return getSucceededActions();
        case FAILED:
          return getActionsForInfeasibleCourse(task.getReason());
        case CANCELLED:
        default:
          return cancelledActions;
      }
    }

    private ObservableList<Actions> getSucceededActions() {
      final boolean feasible = task.getValue();
      if (feasible) {
        return getActionsForFeasibleCourse();
      }
      return getActionsForInfeasibleCourse("");
    }

    /**
     * Get the strings of the actions in {@link #cbAction} for infeasible courses, i.e. compute the
     * unsat core if the course is not impossible or the combination does not contain an impossible
     * course. Otherwise just offer the possibility to remove the feasibility box.
     */
    private ObservableList<Actions> getActionsForInfeasibleCourse(final String reason) {
      if (impossibleCourses.containsAll(Arrays.asList(courses))) {
        return impossibleActions;
      } else if (ResourceBundle.getBundle("lang.tasks").getString("timeout").equals(reason)) {
        return timeoutActions;
      } else {
        return failedWithConflictActions;
      }
    }


    private ObservableList<Actions> getActionsForFeasibleCourse() {
      if (minor != null) {
        return succeededActionsMajorMinor;
      }
      return succeededActionsMajorOnly;
    }

  }
}
