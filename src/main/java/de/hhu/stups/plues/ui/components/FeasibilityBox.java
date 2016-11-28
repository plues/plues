package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.TaskStateColor;
import de.hhu.stups.plues.ui.controller.PdfRenderingHelper;
import de.hhu.stups.plues.ui.layout.Inflater;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
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
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
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

  private String removeString;
  private String unsatCoreString;
  private String cancelString;
  private String impossibleCourseString;
  private String noConflictString;

  private SolverTask<Set<Integer>> unsatCoreTask;
  private SolverTask<Boolean> feasibilityTask;
  private final ObjectProperty<Course> majorCourseProperty;
  private final ObjectProperty<Course> minorCourseProperty;
  private final ExecutorService executorService;
  private final Delayed<SolverService> delayedSolverService;
  private final Delayed<Store> delayedStore;
  private final Set<String> impossibleCourses;
  private final VBox parent;

  private final ListProperty<Integer> unsatCoreProperty = new SimpleListProperty<>();
  @FXML
  @SuppressWarnings("unused")
  private GridPane gridPaneResults;
  @FXML
  @SuppressWarnings("unused")
  private StackPane statePane;
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
  @FXML
  @SuppressWarnings("unused")
  private Button btSubmit;

  /**
   * A container to display the feasibility of a combination of courses or a single one. For
   * infeasible courses it is possible to compute the unsat core which is presented in a {@link
   * ConflictTree TreeView}.
   */
  @Inject
  public FeasibilityBox(final Inflater inflater,
                        final Delayed<Store> delayedStore,
                        final Delayed<SolverService> delayedSolverService,
                        final ExecutorService executorService,
                        final Provider<ConflictTree> conflictTreeProvider,
                        @Assisted("major") final Course majorCourse,
                        @Nullable @Assisted("minor") final Course minorCourse,
                        @Assisted("impossibleCourses") final Set<String> impossibleCourses,
                        @Assisted("parent") final VBox parent) {
    super();
    this.delayedSolverService = delayedSolverService;
    this.delayedStore = delayedStore;
    this.executorService = executorService;
    this.conflictTreeProvider = conflictTreeProvider;
    this.impossibleCourses = impossibleCourses;
    this.parent = parent;

    majorCourseProperty = new SimpleObjectProperty<>(majorCourse);
    minorCourseProperty = new SimpleObjectProperty<>(minorCourse);

    inflater.inflate("components/FeasibilityBox", this, this, "feasibilityBox");
  }

  @Override
  public final void initialize(final URL location, final ResourceBundle resources) {
    removeString = resources.getString("remove");
    unsatCoreString = resources.getString("unsatCore");
    cancelString = resources.getString("cancel");
    impossibleCourseString = resources.getString("impossibleCourse");
    noConflictString = resources.getString("noConflict");

    gridPaneResults.setHgap(5.0);

    lbMajor.textProperty()
        .bind(Bindings.selectString(majorCourseProperty, "fullName"));
    lbMinor.textProperty()
        .bind(Bindings.selectString(minorCourseProperty, "fullName"));

    delayedSolverService.whenAvailable(solver -> {
      final Course cMajor = majorCourseProperty.get();
      final Course cMinor = minorCourseProperty.get();

      if (cMinor != null) {
        feasibilityTask = solver.checkFeasibilityTask(cMajor, cMinor);
      } else {
        feasibilityTask = solver.checkFeasibilityTask(cMajor);
      }

      progressIndicator.setStyle("-fx-progress-color: " + TaskStateColor.WORKING.getColor());
      progressIndicator.visibleProperty().bind(feasibilityTask.runningProperty());

      executorService.submit(feasibilityTask);
    });

    feasibilityTask.setOnSucceeded(event -> Platform.runLater(() -> {
      cbAction.setItems(feasibilityTask.getValue()
          ? FXCollections.observableList(Collections.singletonList(removeString))
          : getActionsForInfeasibleCourse());
      cbAction.getSelectionModel().selectFirst();
    }));

    feasibilityTask.setOnFailed(event -> {
      cbAction.setItems(FXCollections.observableList(Collections.singletonList(removeString)));
      cbAction.getSelectionModel().selectFirst();
    });

    feasibilityTask.setOnCancelled(event -> {
      cbAction.setItems(FXCollections.observableList(Collections.singletonList(removeString)));
      cbAction.getSelectionModel().selectFirst();
    });

    progressIndicator.setStyle("-fx-progress-color: " + TaskStateColor.WORKING.getColor());
    progressIndicator.visibleProperty().bind(feasibilityTask.runningProperty());

    lbIcon.visibleProperty().bind(feasibilityTask.runningProperty().not());
    lbIcon.graphicProperty().bind(PdfRenderingHelper.getIconBinding(ICON_SIZE, feasibilityTask));
    lbIcon.styleProperty().bind(PdfRenderingHelper.getStyleBinding(feasibilityTask));


    cbAction.setItems(FXCollections.observableList(Collections.singletonList(cancelString)));
    cbAction.getSelectionModel().selectFirst();
  }

  @FXML
  @SuppressWarnings("unused")
  private void submitAction() {
    final String selectedItem = cbAction.getSelectionModel().getSelectedItem();

    if (selectedItem.equals(unsatCoreString)) {
      initUnsatCoreTask();
    }
    if (selectedItem.equals(removeString)) {
      parent.getChildren().remove(this);
    }
    if (selectedItem.equals(cancelString)) {
      if (feasibilityTask.isRunning()) {
        interrupt();
        cbAction.setItems(FXCollections.observableList(Collections.singletonList(removeString)));
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
      conflictTree.setConflictSessions(delayedStore.get().getSessions()
          .stream().filter(session -> unsatCoreProperty.get().contains(session.getId()))
          .collect(Collectors.toList()));
      conflictTree.setUnsatCoreProperty(unsatCoreProperty);
      getChildren().add(conflictTree);
      cbAction.setItems(FXCollections.singletonObservableList(removeString));
      cbAction.getSelectionModel().selectFirst();
    });

    unsatCoreTask.setOnCancelled(unsatCore -> {
      if (ResourceBundle.getBundle("lang.tasks").getString("timeout")
          .equals(unsatCoreTask.getReason())) {
        lbErrorMsg.setText(noConflictString);
        cbAction.setItems(FXCollections.singletonObservableList(removeString));
        cbAction.getSelectionModel().selectFirst();
      } else {
        cbAction.setItems(FXCollections.observableList(
            Arrays.asList(unsatCoreString, removeString)));
        cbAction.getSelectionModel().selectFirst();
      }
    });

    unsatCoreTask.setOnFailed(unsatCore -> {
      lbErrorMsg.setText(noConflictString);
      cbAction.setItems(FXCollections.singletonObservableList(removeString));
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
  private ObservableList<String> getActionsForInfeasibleCourse() {
    if (impossibleCourses.contains(majorCourseProperty.get().getName())
        || (minorCourseProperty.get() != null
        && impossibleCourses.contains(minorCourseProperty.get().getName()))) {
      lbErrorMsg.setText(impossibleCourseString);
      return FXCollections.observableList(Collections.singletonList(removeString));
    } else {
      return FXCollections.observableList(Arrays.asList(unsatCoreString, removeString));
    }
  }

  @FXML
  private void interrupt() {
    feasibilityTask.cancel();
  }
}
