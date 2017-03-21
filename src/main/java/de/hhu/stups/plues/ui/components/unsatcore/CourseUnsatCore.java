package de.hhu.stups.plues.ui.components.unsatcore;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.components.CombinationOrSingleCourseSelection;
import de.hhu.stups.plues.ui.layout.Inflater;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

/**
 * Select a course and check its feasibility. If the course is infeasible, the {@link
 * #unsatCoreButtonBar} is enabled and the unsat core modules can be computed. When running another
 * unsat core task from the {@link de.hhu.stups.plues.ui.controller.UnsatCore} the task should be
 * registered using {@link #registerTask(SolverTask)} in order to disable the {@link
 * #courseSelection}.
 */
public class CourseUnsatCore extends GridPane implements Initializable {

  private final ObjectProperty<Store> storeProperty;
  private final ObjectProperty<SolverService> solverServiceProperty;
  private final ListProperty<Course> coursesProperty;
  private final UiDataService uiDataService;
  private final BooleanProperty courseIsInfeasible;
  private final BooleanProperty taskRunningProperty;
  private final BooleanProperty taskScheduledProperty;
  private final ExecutorService executorService;

  @FXML
  @SuppressWarnings("unused")
  private Label unsatCoreInfo;
  @FXML
  @SuppressWarnings("unused")
  private Tooltip unsatCoreInfoTooltip;
  @FXML
  @SuppressWarnings("unused")
  private VBox contentBox;
  @FXML
  @SuppressWarnings("unused")
  private CombinationOrSingleCourseSelection courseSelection;
  @FXML
  @SuppressWarnings("unused")
  private UnsatCoreButtonBar checkFeasibilityButtonBar;
  @FXML
  @SuppressWarnings("unused")
  private UnsatCoreButtonBar unsatCoreButtonBar;

  /**
   * Initialize the component.
   */
  @Inject
  public CourseUnsatCore(final Inflater inflater,
                         final Delayed<Store> delayedStore,
                         final Delayed<SolverService> delayedSolverService,
                         final ExecutorService executorService,
                         final UiDataService uiDataService) {
    this.executorService = executorService;
    this.uiDataService = uiDataService;
    storeProperty = new SimpleObjectProperty<>();
    solverServiceProperty = new SimpleObjectProperty<>();
    courseIsInfeasible = new SimpleBooleanProperty(false);
    taskRunningProperty = new SimpleBooleanProperty(false);
    taskScheduledProperty = new SimpleBooleanProperty(false);

    delayedStore.whenAvailable(storeProperty::set);
    delayedSolverService.whenAvailable(solverServiceProperty::set);

    coursesProperty = new SimpleListProperty<>(FXCollections.emptyObservableList());

    inflater.inflate("components/unsatcore/CourseUnsatCore", this, this, "unsatCore", "Column");
  }

  public UnsatCoreButtonBar getUnsatCoreButtonBar() {
    return unsatCoreButtonBar;
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    checkFeasibilityButtonBar.setSubmitText(resources.getString("checkFeasibility"));
    checkFeasibilityButtonBar.setShowIconOnSucceeded(true);
    checkFeasibilityButtonBar.disableProperty().bind(solverServiceProperty.isNull()
        .or(courseIsInfeasible));
    checkFeasibilityButtonBar.setOnAction(this::checkFeasibility);

    unsatCoreButtonBar.setSubmitText(resources.getString("button.unsatCoreModules"));
    unsatCoreButtonBar.visibleProperty().bind(courseIsInfeasible);
    unsatCoreButtonBar.disableProperty().bind(solverServiceProperty.isNull()
        .or(courseIsInfeasible.not()));

    coursesProperty.addListener((observable, oldValue, newValue) -> {
      courseIsInfeasible.set(false);
      checkFeasibilityButtonBar.taskProperty().set(null);
    });

    unsatCoreInfo.setOnMouseEntered(event -> {
      final Point2D pos = unsatCoreInfo.localToScreen(
          unsatCoreInfo.getLayoutBounds().getMaxX(), unsatCoreInfo.getLayoutBounds().getMaxY());
      unsatCoreInfoTooltip.show(unsatCoreInfo, pos.getX(), pos.getY());
    });
    unsatCoreInfo.setOnMouseExited(event -> unsatCoreInfoTooltip.hide());

    unsatCoreInfo.graphicProperty().bind(Bindings.createObjectBinding(() ->
        FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.INFO_CIRCLE, "20")));

    storeProperty.addListener((observable, oldValue, store)
        -> courseSelection.setCourses(store.getCourses()));

    courseSelection.disableProperty().bind(storeProperty.isNull().or(taskRunningProperty)
        .or(taskScheduledProperty));
    courseSelection.impossibleCoursesProperty().bind(uiDataService.impossibleCoursesProperty());
    coursesProperty.bind(courseSelection.selectedCoursesProperty());
  }

  /**
   * Check the {@link #coursesProperty selected courses} feasibility and only enable the detailed
   * conflict search if the course is infeasible.
   */
  @SuppressWarnings("unused")
  public void checkFeasibility(final ActionEvent actionEvent) {
    if (coursesProperty.get() == null || coursesProperty.size() == 0) {
      return;
    }
    final SolverTask<Boolean> checkFeasibilityTask;
    if (coursesProperty.size() == 1) {
      checkFeasibilityTask = solverServiceProperty.get()
          .checkFeasibilityTask(coursesProperty().get().get(0));
    } else {
      checkFeasibilityTask = solverServiceProperty.get()
          .checkFeasibilityTask(coursesProperty().get().get(0), coursesProperty().get().get(1));
    }
    checkFeasibilityTask.setOnFailed(event ->
        courseIsInfeasible.set(Worker.State.FAILED.equals(checkFeasibilityTask.getState())));

    registerTask(checkFeasibilityTask);

    checkFeasibilityButtonBar.taskProperty().set(checkFeasibilityTask);

    executorService.submit(checkFeasibilityTask);
  }

  /**
   * Register a task, i.e. set listeners to disable the {@link #courseSelection} when a task is
   * scheduled or running.
   */
  public void registerTask(final SolverTask<?> task) {
    taskScheduledProperty.set(true);
    task.setOnRunning(event -> taskScheduledProperty.set(false));
    task.setOnCancelled(event -> {
      taskScheduledProperty.set(false);
      taskRunningProperty.unbind();
    });
    taskRunningProperty.bind(task.runningProperty());
  }

  public ListProperty<Course> coursesProperty() {
    return coursesProperty;
  }

  public BooleanProperty taskRunningProperty() {
    return taskRunningProperty;
  }

  public BooleanProperty taskScheduledProperty() {
    return taskScheduledProperty;
  }

  public BooleanProperty courseIsInfeasibleProperty() {
    return courseIsInfeasible;
  }

  public void selectCourses(final Course... courses) {
    courseSelection.selectCourses(courses);
  }
}
