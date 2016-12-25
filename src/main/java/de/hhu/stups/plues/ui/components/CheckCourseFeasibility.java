package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.ui.layout.Inflater;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
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
  private ScrollPane scrollPaneResults;
  @FXML
  @SuppressWarnings("unused")
  private VBox resultBoxWrapper;
  @FXML
  @SuppressWarnings("unused")
  private Button btUnhighlightAllConflicts;

  /**
   * Component to select a combination of courses or a single subject obtained by {@link
   * CombinationOrSingleCourseSelection} and check its feasibility. The results are displayed in
   * {@link FeasibilityBox result boxes} within {@link #resultBoxWrapper a VBox}. When using the
   * component we need to initialize the courses via {@link #setCourses(List)} and optionally
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
    resultBoxWrapper.setSpacing(5.0);

    final IntegerBinding resultBoxChildren = Bindings.size(resultBoxWrapper.getChildren());
    scrollPaneResults.visibleProperty().bind(resultBoxChildren.greaterThan(0));

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
    if (combinationOrSingleCourseSelection.getSelectedCourses().size() == 2) {
      resultBoxWrapper.getChildren().add(0, feasibilityBoxFactory.create(
          combinationOrSingleCourseSelection.getSelectedCourses().get(0),
          combinationOrSingleCourseSelection.getSelectedCourses().get(1),
          resultBoxWrapper));
    } else {
      resultBoxWrapper.getChildren().add(0, feasibilityBoxFactory.create(
          combinationOrSingleCourseSelection.getSelectedCourses().get(0), null,
          resultBoxWrapper));
    }
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
}
