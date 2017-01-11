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
import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

@SuppressWarnings("WeakerAccess")
public class FeasibilityBox extends VBox implements Initializable {

  private final Provider<ConflictTree> conflictTreeProvider;
  private static final String ICON_SIZE = "50";

  private String openInTimetable;
  private String restartComputation;
  private String generatePdf;
  private String generatePartial;
  private String removeString;
  private String unsatCoreString;
  private String stepwiseUnsatCore;
  private String cancelString;
  private String impossibleCourseString;
  private ResultState resultState;

  private String noConflictString;
  private SolverTask<Set<Integer>> unsatCoreTask;
  private SolverTask<Boolean> feasibilityTask;
  private final ObjectProperty<Course> majorCourseProperty;
  private final ObjectProperty<Course> minorCourseProperty;
  private final ExecutorService executorService;
  private final Delayed<SolverService> delayedSolverService;
  private final Delayed<Store> delayedStore;
  private final Set<Course> impossibleCourses;
  private final Router router;

  private final ListView<FeasibilityBox> parent;
  private final ListProperty<Integer> unsatCoreProperty = new SimpleListProperty<>();
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
  private ComboBox<String> cbAction;

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

    majorCourseProperty = new SimpleObjectProperty<>(majorCourse);
    minorCourseProperty = new SimpleObjectProperty<>(minorCourse);

    inflater.inflate("components/FeasibilityBox", this, this, "feasibilityBox");
  }

  @Override
  public final void initialize(final URL location, final ResourceBundle resources) {
    openInTimetable = resources.getString("openInTimetable");
    restartComputation = resources.getString("restartComputation");
    generatePdf = resources.getString("generatePDF");
    generatePartial = resources.getString("generatePartial");
    stepwiseUnsatCore = resources.getString("stepwiseUnsatCore");
    removeString = resources.getString("remove");
    unsatCoreString = resources.getString("unsatCore");
    cancelString = resources.getString("cancel");
    impossibleCourseString = resources.getString("impossibleCourse");
    noConflictString = resources.getString("noConflict");

    lbMajor.textProperty()
        .bind(Bindings.selectString(majorCourseProperty, "fullName"));
    lbMinor.textProperty()
        .bind(Bindings.selectString(minorCourseProperty, "fullName"));

    delayedSolverService.whenAvailable(solver -> {
      initFeasibilityTask(solver);
      executorService.submit(feasibilityTask);
    });
  }

  private void initFeasibilityTask(final SolverService solverService) {
    final Course cMajor = majorCourseProperty.get();
    final Course cMinor = minorCourseProperty.get();

    if (cMinor != null) {
      feasibilityTask = solverService.checkFeasibilityTask(cMajor, cMinor);
    } else {
      feasibilityTask = solverService.checkFeasibilityTask(cMajor);
    }

    feasibilityTask.setOnFailed(event -> {
      cbAction.setItems(getActionsForInfeasibleCourse(feasibilityTask.getReason()));
      cbAction.getSelectionModel().selectFirst();
      resultState = ResultState.FAILED;
    });

    feasibilityTask.setOnSucceeded(event -> Platform.runLater(() -> {
      cbAction.setItems(feasibilityTask.getValue()
          ? FXCollections.observableList(minorCourseProperty.get() != null
          ? FXCollections.observableArrayList(
          openInTimetable, generatePdf, generatePartial, removeString)
          : FXCollections.observableArrayList(openInTimetable, removeString))
          : getActionsForInfeasibleCourse(""));
      cbAction.getSelectionModel().selectFirst();
      resultState = ResultState.SUCCEEDED;
    }));

    feasibilityTask.setOnCancelled(event -> {
      cbAction.setItems(FXCollections.observableList(
          Arrays.asList(openInTimetable, restartComputation, removeString)));
      cbAction.getSelectionModel().selectFirst();
      resultState = ResultState.FAILED;
    });

    feasibilityTask.setOnScheduled(event -> {
      cbAction.setItems(FXCollections.observableList(Collections.singletonList(cancelString)));
      cbAction.getSelectionModel().selectFirst();
    });

    progressIndicator.setStyle("-fx-progress-color: " + TaskStateColor.WORKING.getColor());
    progressIndicator.visibleProperty().bind(feasibilityTask.runningProperty());

    lbIcon.visibleProperty().bind(feasibilityTask.runningProperty().not());
    lbIcon.graphicProperty().bind(TaskBindings.getIconBinding(ICON_SIZE, feasibilityTask));
    lbIcon.styleProperty().bind(TaskBindings.getStyleBinding(feasibilityTask));
  }

  @FXML
  @SuppressWarnings("unused")
  private void submitAction() {
    final String selectedItem = cbAction.getSelectionModel().getSelectedItem();
    final Course majorCourse = majorCourseProperty.get();
    final Course minorCourse = minorCourseProperty.get();

    if (selectedItem.equals(openInTimetable)) {
      router.transitionTo(RouteNames.TIMETABLE, new Course[] {majorCourse, minorCourse},
          resultState);
    }
    if (selectedItem.equals(restartComputation)) {
      initFeasibilityTask(delayedSolverService.get());
      executorService.submit(feasibilityTask);
    }
    if (selectedItem.equals(generatePdf)) {
      router.transitionTo(RouteNames.PDF_TIMETABLES, majorCourse, minorCourse);
    }
    if (selectedItem.equals(generatePartial)) {
      router.transitionTo(RouteNames.PARTIAL_TIMETABLES, majorCourse, minorCourse);
    }
    if (selectedItem.equals(stepwiseUnsatCore)) {
      if (minorCourse != null) {
        router.transitionTo(RouteNames.UNSAT_CORE, majorCourse, minorCourse);
      } else {
        router.transitionTo(RouteNames.UNSAT_CORE, majorCourse);
      }
    }
    if (selectedItem.equals(unsatCoreString)) {
      initUnsatCoreTask();
    }
    if (selectedItem.equals(removeString)) {
      parent.getItems().remove(this);
    }
    if (selectedItem.equals(cancelString)) {
      if (feasibilityTask.isRunning()) {
        interrupt();
        cbAction.setItems(FXCollections.observableList(
            Arrays.asList(openInTimetable, restartComputation, removeString)));
        cbAction.getSelectionModel().selectFirst();
      } else if (unsatCoreTask.isRunning()) {
        unsatCoreTask.cancel(true);
      }
    }
  }

  /**
   * Initialize and submit the {@link #unsatCoreTask task} to compute the unsat core and set all its
   * necessary listeners. If the task succeeded the {@link ConflictTree} is dynamically added to the
   * {@link this FeasibilityBox}. Otherwise the unsat core computation failed and an error message
   * is shown to the user.
   */
  private void initUnsatCoreTask() {
    final Course majorCourse = majorCourseProperty.get();
    final Course minorCourse = minorCourseProperty.get();

    if (minorCourse != null) {
      unsatCoreTask = delayedSolverService.get().unsatCore(majorCourse, minorCourse);
    } else {
      unsatCoreTask = delayedSolverService.get().unsatCore(majorCourse);
    }

    unsatCoreTask.setOnSucceeded(unsatCore -> {
      unsatCoreProperty.set(FXCollections.observableArrayList(unsatCoreTask.getValue()));
      final ConflictTree conflictTree = conflictTreeProvider.get();
      // TODO: move to conflict tree
      conflictTree.setConflictSessions(
          unsatCoreProperty.get()
              .stream().map(delayedStore.get()::getSessionById)
              .collect(Collectors.toList()));
      conflictTree.setUnsatCoreProperty(unsatCoreProperty);
      getChildren().add(conflictTree);
      cbAction.setItems(FXCollections.observableArrayList(
          openInTimetable, stepwiseUnsatCore, removeString));
      cbAction.getSelectionModel().selectFirst();
    });

    unsatCoreTask.setOnCancelled(unsatCore -> {
      if (ResourceBundle.getBundle("lang.tasks").getString("timeout")
          .equals(unsatCoreTask.getReason())) {
        lbErrorMsg.setText(noConflictString);
      }
      cbAction.setItems(FXCollections.observableList(
          Arrays.asList(unsatCoreString, openInTimetable, stepwiseUnsatCore, removeString)));
      cbAction.getSelectionModel().selectFirst();
    });

    unsatCoreTask.setOnFailed(unsatCore -> {
      lbErrorMsg.setText(noConflictString);
      cbAction.setItems(FXCollections.observableArrayList(
          openInTimetable, stepwiseUnsatCore, removeString));
      cbAction.getSelectionModel().selectFirst();
    });

    unsatCoreTask.setOnScheduled(unsatCore -> {
      cbAction.setItems(FXCollections.observableList(Collections.singletonList(cancelString)));
      cbAction.getSelectionModel().selectFirst();
    });

    executorService.submit(unsatCoreTask);
  }

  /**
   * Get the strings of the actions in {@link #cbAction} for infeasible courses, i.e. compute the
   * unsat core if the course is not impossible or the combination does not contain an impossible
   * course. Otherwise just offer the possibility to remove the feasibility box.
   */
  private ObservableList<String> getActionsForInfeasibleCourse(final String reason) {
    if (impossibleCourses.contains(majorCourseProperty.get())
        || (minorCourseProperty.get() != null
        && impossibleCourses.contains(minorCourseProperty.get()))) {
      lbErrorMsg.setText(impossibleCourseString);
      return FXCollections.observableList(Collections.singletonList(removeString));
    } else if (ResourceBundle.getBundle("lang.tasks").getString("timeout")
        .equals(reason)) {
      return FXCollections.observableList(
          Arrays.asList(openInTimetable, restartComputation, removeString));
    } else {
      return FXCollections.observableList(
          Arrays.asList(unsatCoreString, openInTimetable, stepwiseUnsatCore, removeString));
    }
  }

  @FXML
  private void interrupt() {
    feasibilityTask.cancel();
  }
}
