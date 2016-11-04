package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.controller.PdfRenderingHelper;
import de.hhu.stups.plues.ui.layout.Inflater;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
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
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nullable;

public class FeasibilityBox extends GridPane implements Initializable {

  private static final String WORKING_COLOR = "#BDE5F8";

  private String removeString;
  private String unsatCoreString;
  private String cancelString;
  private String impossibleCourseString;
  private String noConflictString;

  private final ObjectProperty<Course> majorCourseProperty;
  private final ObjectProperty<Course> minorCourseProperty;
  private SolverTask<Boolean> task;
  private final ExecutorService executorService;
  private final Delayed<SolverService> delayedSolverService;
  private final Set<String> impossibleCourses;
  private final VBox parent;

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
   * A container to present a computed feasibility result for a given major and/or minor course like
   * a {@link ResultBox} but with slightly different behavior.
   */
  @Inject
  public FeasibilityBox(final Inflater inflater,
                        final Delayed<SolverService> delayedSolverService,
                        final ExecutorService executorService,
                        @Assisted("major") final Course majorCourse,
                        @Nullable @Assisted("minor") final Course minorCourse,
                        @Assisted("impossibleCourses") final Set<String> impossibleCourses,
                        @Assisted("parent") final VBox parent) {
    super();
    this.delayedSolverService = delayedSolverService;
    this.executorService = executorService;
    this.impossibleCourses = impossibleCourses;
    this.parent = parent;

    this.majorCourseProperty = new SimpleObjectProperty<>(majorCourse);
    this.minorCourseProperty = new SimpleObjectProperty<>(minorCourse);

    setHgap(10.0);

    inflater.inflate("components/resultbox", this, this, "resultbox");
  }

  @Override
  public final void initialize(final URL location, final ResourceBundle resources) {
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
      final Course cMajor = majorCourseProperty.get();
      final Course cMinor = minorCourseProperty.get();

      if (cMinor != null) {
        task = solver.checkFeasibilityTask(cMajor, cMinor);
      } else {
        task = solver.checkFeasibilityTask(cMajor);
      }

      progressIndicator.setStyle("-fx-progress-color: " + WORKING_COLOR);
      progressIndicator.visibleProperty().bind(task.runningProperty());

      executorService.submit(task);
    });

    final String bgColorCommand = "-fx-background-color:";
    task.setOnSucceeded(event -> Platform.runLater(() -> {
      cbAction.setItems(task.getValue()
          ? FXCollections.observableList(Collections.singletonList(removeString))
          : getActionsForInfeasibleCourse());
      cbAction.getSelectionModel().selectFirst();
      lbIcon.setGraphic(FontAwesomeIconFactory.get().createIcon(task.getValue()
          ? FontAwesomeIcon.CHECK : FontAwesomeIcon.REMOVE, "50"));
      lbIcon.setStyle(bgColorCommand + (task.getValue()
          ? PdfRenderingHelper.SUCCESS_COLOR : PdfRenderingHelper.FAILURE_COLOR));
    }));

    task.setOnFailed(event -> {
      cbAction.setItems(FXCollections.observableList(Collections.singletonList(removeString)));
      cbAction.getSelectionModel().selectFirst();
      lbIcon.setGraphic(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.REMOVE, "50"));
      lbIcon.setStyle(bgColorCommand + PdfRenderingHelper.FAILURE_COLOR);
    });

    task.setOnCancelled(event -> {
      cbAction.setItems(FXCollections.observableList(Collections.singletonList(removeString)));
      cbAction.getSelectionModel().selectFirst();
      lbIcon.setGraphic(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.QUESTION, "50"));
      lbIcon.setStyle(bgColorCommand + PdfRenderingHelper.WARNING_COLOR);
    });

    progressIndicator.setStyle("-fx-progress-color: " + WORKING_COLOR);
    progressIndicator.visibleProperty().bind(task.runningProperty());

    cbAction.setItems(FXCollections.observableList(Collections.singletonList(cancelString)));
    cbAction.getSelectionModel().selectFirst();
  }

  @FXML
  @SuppressWarnings("unused")
  private void submitAction() {
    final String selectedItem = cbAction.getSelectionModel().getSelectedItem();

    if (selectedItem.equals(unsatCoreString)) {
      final SolverTask<List<Integer>> unsatCoreTask;
      final Course majorCourse = majorCourseProperty.get();
      final Course minorCourse = minorCourseProperty.get();

      if (minorCourse != null) {
        unsatCoreTask = delayedSolverService.get().unsatCore(majorCourse, minorCourse);
      } else {
        unsatCoreTask = delayedSolverService.get().unsatCore(majorCourse);
      }

      unsatCoreTask.setOnSucceeded(unsatCore -> {
        // Todo: do something with the unsat core
        cbAction.setItems(FXCollections.singletonObservableList(removeString));
        cbAction.getSelectionModel().selectFirst();
      });

      unsatCoreTask.setOnFailed(unsatCore -> {
        lbErrorMsg.setText(noConflictString);
        cbAction.setItems(FXCollections.singletonObservableList(removeString));
        cbAction.getSelectionModel().selectFirst();
      });

      executorService.submit(unsatCoreTask);
    }
    if (selectedItem.equals(removeString)) {
      parent.getChildren().remove(this);
    }
    if (selectedItem.equals(cancelString)) {
      interrupt();
      cbAction.setItems(FXCollections.observableList(Collections.singletonList(removeString)));
      cbAction.getSelectionModel().selectFirst();
    }
  }

  /**
   * Get the actions for infeasible courses, i.e. compute the unsat core if the course is not
   * impossible or the combination does not contain an impossible course. Otherwise just offer the
   * possibility to remove the feasibility box.
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
    task.cancel();
  }
}