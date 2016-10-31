package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
  private final BooleanProperty solverProperty;

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

  /**
   * Component to select a combination of courses or a single subject obtained by {@link
   * CombinationOrSingleCourseSelection} and check its feasibility. The results are displayed in
   * {@link FeasibilityBox result boxes} within {@link #resultBoxWrapper a VBox}. When using the
   * component we need to initialize the courses via {@link #setCourses(List)} and optionally
   * highlight the impossible courses with the use of {@link #highlightImpossibleCourses(Set)}.
   */
  @Inject
  public CheckCourseFeasibility(final Inflater inflater,
                                final FeasibilityBoxFactory feasibilityBoxFactory) {
    this.feasibilityBoxFactory = feasibilityBoxFactory;
    solverProperty = new SimpleBooleanProperty(false);

    setSpacing(10.0);

    inflater.inflate("components/CheckCourseFeasibility", this, this, "checkCourseFeasibility");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    IntegerBinding resultBoxChildren = Bindings.size(resultBoxWrapper.getChildren());
    scrollPaneResults.visibleProperty().bind(resultBoxChildren.greaterThan(0));

    btCheckFeasibility.disableProperty().bind(solverProperty.not());
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
          combinationOrSingleCourseSelection.getSelectedCourses().get(1), resultBoxWrapper));
    } else {
      resultBoxWrapper.getChildren().add(0, feasibilityBoxFactory.create(
          combinationOrSingleCourseSelection.getSelectedCourses().get(0), null, resultBoxWrapper));
    }
  }

  public void setCourses(final List<Course> courses) {
    combinationOrSingleCourseSelection.setCourses(courses);
  }

  void highlightImpossibleCourses(Set<String> impossibleCourses) {
    combinationOrSingleCourseSelection.highlightImpossibleCourses(impossibleCourses);
  }

  void setSolverProperty(Boolean value) {
    solverProperty.setValue(value);
  }
}