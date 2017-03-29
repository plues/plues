package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

public class CheckCourseFeasibility extends VBox implements Initializable {

  private final FeasibilityBoxFactory feasibilityBoxFactory;
  private final BooleanProperty solverAvailableProperty;

  private final UiDataService uiDataService;

  @FXML
  @SuppressWarnings("unused")
  private CombinationOrSingleCourseSelection combinationOrSingleCourseSelection;
  @FXML
  @SuppressWarnings("unused")
  private Button btCheckFeasibility;
  @FXML
  @SuppressWarnings("unused")
  private ListView<FeasibilityBox> feasibilityBoxWrapper;
  @FXML
  @SuppressWarnings("unused")
  private Button btUnhighlightAllConflicts;

  /**
   * Component to select a combination of courses or a single subject obtained by {@link
   * CombinationOrSingleCourseSelection} and check its feasibility. The results are displayed in
   * {@link FeasibilityBox result boxes} within {@link #feasibilityBoxWrapper a VBox}. When using
   * the component we need to initialize the courses via {@link #setCourses(List)} and optionally
   * highlight the impossible courses with the use of {@link #impossibleCoursesProperty()}. As soon
   * as the solver is available the {@link #solverAvailableProperty} is set to true to enable
   * computations like {@link #btCheckFeasibility}.
   */
  @Inject
  public CheckCourseFeasibility(final Inflater inflater,
                                final FeasibilityBoxFactory feasibilityBoxFactory,
                                final Delayed<SolverService> solverServiceDelayed,
                                final UiDataService uiDataService) {
    this.feasibilityBoxFactory = feasibilityBoxFactory;
    this.uiDataService = uiDataService;

    solverAvailableProperty = new SimpleBooleanProperty(false);
    solverServiceDelayed.whenAvailable(solverService -> solverAvailableProperty.set(true));

    inflater.inflate("components/CheckCourseFeasibility", this, this, "checkCourseFeasibility");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    feasibilityBoxWrapper.visibleProperty().bind(
        Bindings.isEmpty(feasibilityBoxWrapper.getItems()).not());
    // disable list-view selection
    feasibilityBoxWrapper.getSelectionModel().selectedIndexProperty().addListener(
        (observable, oldvalue, newValue) ->
            Platform.runLater(() -> feasibilityBoxWrapper.getSelectionModel().select(-1)));

    btCheckFeasibility.disableProperty().bind(solverAvailableProperty.not());
    btUnhighlightAllConflicts.visibleProperty().bind(this.uiDataService
        .conflictMarkedSessionsProperty().emptyProperty().not());
  }

  /**
   * Check the feasibility of the current selected course or single subject. This method is used as
   * the action of {@link #btCheckFeasibility}.
   */
  @FXML
  @SuppressWarnings("unused")
  public void checkFeasibility() {
    Platform.runLater(() ->
        addOrRestartFeasibilityBox(combinationOrSingleCourseSelection.getSelectedCourses()));
  }

  /**
   * In case the {@link #feasibilityBoxWrapper} already contains a {@link FeasibilityBox} with the
   * selected courses we restart this box and bring it to the top of the list view.
   * Otherwise, a new feasibility box is created.
   */
  @SuppressWarnings("unused")
  private void addOrRestartFeasibilityBox(final ObservableList<Course> selectedCourses) {
    if (selectedCourses.size() == 0) {
      return;
    }
    final Course majorCourse = selectedCourses.get(0);
    final Course minorCourse;
    if (selectedCourses.size() == 2) {
      minorCourse = selectedCourses.get(1);
    } else {
      minorCourse = null;
    }
    final Optional<FeasibilityBox> containsBox = feasibilityBoxWrapper.getItems().stream().filter(
        feasibilityBox -> majorCourse.equals(feasibilityBox.getMajorCourse())
            && (minorCourse == null || minorCourse.equals(feasibilityBox.getMinorCourse())))
        .findFirst();
    if (containsBox.isPresent()) {
      toTopOfListview(containsBox.get());
      return;
    }
    feasibilityBoxWrapper.getItems().add(0, feasibilityBoxFactory.create(majorCourse, minorCourse,
        feasibilityBoxWrapper));
    feasibilityBoxWrapper.scrollTo(0);
  }

  private void toTopOfListview(final FeasibilityBox feasibilityBox) {
    feasibilityBoxWrapper.getItems().remove(feasibilityBox);
    feasibilityBoxWrapper.getItems().add(0, feasibilityBox);
    feasibilityBoxWrapper.scrollTo(0);
    feasibilityBox.restartComputationAction();
  }

  public void setCourses(final List<Course> courses) {
    combinationOrSingleCourseSelection.setCourses(courses);
  }

  public void selectCourses(final Course... courses) {
    combinationOrSingleCourseSelection.selectCourses(courses);
  }

  @FXML
  @SuppressWarnings("unused")
  public void unhighlightConflicts() {
    uiDataService.setConflictMarkedSessions(FXCollections.observableArrayList());
  }

  @SuppressWarnings("WeakerAccess")
  public SetProperty<Course> impossibleCoursesProperty() {
    return combinationOrSingleCourseSelection.impossibleCoursesProperty();
  }

  public Set<Course> getImpossibleCourses() {
    return combinationOrSingleCourseSelection.getImpossibleCourses();
  }

  public ListView<FeasibilityBox> getFeasibilityBoxWrapper() {
    return feasibilityBoxWrapper;
  }
}
