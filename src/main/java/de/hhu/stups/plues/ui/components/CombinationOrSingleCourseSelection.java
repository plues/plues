package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

public class CombinationOrSingleCourseSelection extends VBox implements Initializable {

  private final ReadOnlyListProperty<Course> selectedCourses;
  private final ToggleGroup toggleGroup;

  @FXML
  @SuppressWarnings("unused")
  private RadioButton rbCombination;
  @FXML
  @SuppressWarnings("unused")
  private RadioButton rbSingleSelection;
  @FXML
  @SuppressWarnings("unused")
  private MajorMinorCourseSelection majorMinorCourseSelection;
  @FXML
  @SuppressWarnings("unused")
  private ComboBox<Course> singleCourseSelection;

  /**
   * This component offers the {@link MajorMinorCourseSelection} as well as the possibility to
   * select a single course from a given list. The current selection can be toggled via radio
   * buttons. When using the component we need to initialize the courses with the use of {@link
   * #setCourses(List)}. The selected combination of courses or a single course is stored in an
   * {@link #selectedCourses observable list} and can be accessed via {@link #getSelectedCourses}.
   * Impossible courses also need to be initialized via {@link #highlightImpossibleCourses(Set)}.
   */
  @Inject
  public CombinationOrSingleCourseSelection(final Inflater inflater) {
    selectedCourses = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());
    toggleGroup = new ToggleGroup();

    setSpacing(5.0);

    inflater.inflate("components/CombinationOrSingleCourseSelection", this, this,
        "combinationOrSingleCourseSelection");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    majorMinorCourseSelection.setPercentWidth(100.0);

    rbCombination.setToggleGroup(toggleGroup);
    rbSingleSelection.setToggleGroup(toggleGroup);

    rbCombination.selectedProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        majorMinorCourseSelection.setDisable(false);
        singleCourseSelection.setDisable(true);
        selectedCourses.clear();
        selectedCourses.add(majorMinorCourseSelection.getSelectedMajorCourse());
        final Optional<Course> optionalMinor;
        optionalMinor = majorMinorCourseSelection.getSelectedMinorCourse();
        if (optionalMinor.isPresent()) {
          selectedCourses.add(optionalMinor.get());
        }
      }
    });

    rbSingleSelection.selectedProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        majorMinorCourseSelection.setDisable(true);
        singleCourseSelection.setDisable(false);
        selectedCourses.clear();
        selectedCourses.add(singleCourseSelection.getSelectionModel().getSelectedItem());
      }
    });

    singleCourseSelection.setDisable(true);
    singleCourseSelection.valueProperty().addListener((observable, oldValue, newValue) -> {
      selectedCourses.clear();
      selectedCourses.add(newValue);
    });

    majorMinorCourseSelection.addListener(observable -> {
      selectedCourses.clear();
      selectedCourses.add(majorMinorCourseSelection.getSelectedMajorCourse());
      final Optional<Course> optionalMinor;
      optionalMinor = majorMinorCourseSelection.getSelectedMinorCourse();
      if (optionalMinor.isPresent()) {
        selectedCourses.add(optionalMinor.get());
      }
    });
  }

  /**
   * Initialize the courses within the components {@link #majorMinorCourseSelection} and {@link
   * #singleCourseSelection}.
   *
   * @param courses The unfiltered list of courses as it is obtained by the store.
   */
  public void setCourses(final List<Course> courses) {
    final List<Course> majorCourses;
    majorCourses = courses.stream().filter(Course::isMinor)
        .collect(Collectors.toList());
    final List<Course> minorCourses = courses.stream().filter(Course::isMajor)
        .collect(Collectors.toList());

    majorMinorCourseSelection.setMinorCourseList(FXCollections.observableList(majorCourses));
    majorMinorCourseSelection.setMajorCourseList(FXCollections.observableList(minorCourses));

    singleCourseSelection.setItems(FXCollections.observableList(courses));
    singleCourseSelection.getSelectionModel().selectFirst();

    rbCombination.setSelected(true);
  }

  public ReadOnlyListProperty<Course> getSelectedCourses() {
    return selectedCourses;
  }

  void highlightImpossibleCourses(final Set<String> impossibleCourses) {
    majorMinorCourseSelection.highlightImpossibleCourses(impossibleCourses);
    singleCourseSelection.setCellFactory(
        majorMinorCourseSelection.getCallbackForImpossibleCourses(impossibleCourses));
  }
}
