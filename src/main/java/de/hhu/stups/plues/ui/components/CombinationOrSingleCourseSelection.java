package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.ui.layout.Inflater;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ListBinding;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * This component offers the {@link MajorMinorCourseSelection} as well as the possibility to
 * select a single course from a given list. The current selection can be toggled via radio
 * buttons. When using the component we need to initialize the courses with the use of {@link
 * #setCourses(List)}. The selected combination of courses or a single course is stored in an
 * {@link #selectedCourses observable list} and can be accessed via {@link #getSelectedCourses}.
 * Impossible courses can be initialized via {@link #impossibleCoursesProperty}.
 */
public class CombinationOrSingleCourseSelection extends VBox {

  private final ListProperty<Course> selectedCourses;
  private final ToggleGroup toggleGroup;
  private final SetProperty<Course> impossibleCoursesProperty;
  private final ListProperty<Course> coursesProperty;

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
   * Constructor of CombinationOrSingleCourseSelection.
   */
  @Inject
  public CombinationOrSingleCourseSelection(final Inflater inflater) {
    selectedCourses = new SimpleListProperty<>();
    toggleGroup = new ToggleGroup();

    coursesProperty = new SimpleListProperty<>(FXCollections.emptyObservableList());
    impossibleCoursesProperty = new SimpleSetProperty<>(FXCollections.emptyObservableSet());

    inflater.inflate("components/CombinationOrSingleCourseSelection", this, this,
        "combinationOrSingleCourseSelection");
  }

  @FXML
  public void initialize() {
    disableProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        rbCombination.disableProperty().bind(new SimpleBooleanProperty(true));
        rbSingleSelection.disableProperty().bind(new SimpleBooleanProperty(true));
        majorMinorCourseSelection.disableProperty().bind(new SimpleBooleanProperty(true));
        singleCourseSelection.disableProperty().bind(new SimpleBooleanProperty(true));
      } else {
        bindDisableProperties();
      }
    });

    majorMinorCourseSelection.setPercentWidth(100.0);
    majorMinorCourseSelection.impossibleCoursesProperty().bind(impossibleCoursesProperty);

    impossibleCoursesProperty.addListener((observable, oldValue, newValue)
        -> singleCourseSelection.setCellFactory(
        majorMinorCourseSelection.getCallbackForImpossibleCourses(newValue)));

    rbCombination.setToggleGroup(toggleGroup);
    rbSingleSelection.setToggleGroup(toggleGroup);

    singleCourseSelection.itemsProperty().bind(coursesProperty);
    singleCourseSelection.itemsProperty().addListener((observable, oldValue, newValue)
        -> singleCourseSelection.getSelectionModel().selectFirst());

    bindDisableProperties();

    majorMinorCourseSelection.majorCourseListProperty()
        .bind(new SimpleListProperty<>(coursesProperty.filtered(Course::isMajor)));

    selectedCourses.bind(Bindings.when(rbSingleSelection.selectedProperty())
        .then(new SingleCourseListBinding()).otherwise(
            (ObservableList<Course>) majorMinorCourseSelection.selectedCoursesProperty()));
  }

  private void bindDisableProperties() {
    rbCombination.disableProperty().bind(coursesProperty.emptyProperty());
    rbSingleSelection.disableProperty().bind(coursesProperty.emptyProperty());
    majorMinorCourseSelection.disableProperty().bind(
        rbCombination.selectedProperty().not().or(coursesProperty.emptyProperty()));
    singleCourseSelection.disableProperty().bind(
        rbSingleSelection.selectedProperty().not().or(coursesProperty.emptyProperty()));
  }

  /**
   * Select courses within the {@link #majorMinorCourseSelection} if two combinable courses or a
   * standalone course is given. If only one combinable course is given use the {@link
   * #singleCourseSelection}.
   */
  public void selectCourses(final Course... courses) {
    if (courses.length == 1 && courses[0].isCombinable()) {
      rbSingleSelection.setSelected(true);
      singleCourseSelection.getSelectionModel().select(courses[0]);
    } else {
      rbCombination.setSelected(true);
      if (courses.length > 0) {
        majorMinorCourseSelection.selectCourse(courses[0]);
      }
      if (courses.length > 1) {
        majorMinorCourseSelection.selectCourse(courses[1]);
      }
    }
  }

  /**
   * Initialize the courses within the components {@link #majorMinorCourseSelection} and {@link
   * #singleCourseSelection}.
   *
   * @param courses The unfiltered list of courses as it is obtained by the store.
   */
  public void setCourses(final List<Course> courses) {
    this.coursesProperty.set(FXCollections.observableList(courses));
  }

  @SuppressWarnings("WeakerAccess")
  public ObservableList<Course> getSelectedCourses() {
    return selectedCourses.get();
  }

  public ReadOnlyListProperty<Course> selectedCoursesProperty() {
    return selectedCourses;
  }


  public ObservableSet<Course> getImpossibleCourses() {
    return impossibleCoursesProperty.get();
  }

  public void setImpossibleCourses(final ObservableSet<Course> impossibleCourses) {
    this.impossibleCoursesProperty.set(impossibleCourses);
  }

  public SetProperty<Course> impossibleCoursesProperty() {
    return this.impossibleCoursesProperty;
  }

  public RadioButton getRbCombination() {
    return rbCombination;
  }

  public RadioButton getRbSingleSelection() {
    return rbSingleSelection;
  }

  public MajorMinorCourseSelection getMajorMinorCourseSelection() {
    return majorMinorCourseSelection;
  }

  public ComboBox<Course> getSingleCourseSelection() {
    return singleCourseSelection;
  }


  private class SingleCourseListBinding extends ListBinding<Course> {
    private SingleCourseListBinding() {
      bind(singleCourseSelection.getSelectionModel().selectedItemProperty());
    }

    @Override
    protected ObservableList<Course> computeValue() {
      final Course item = singleCourseSelection.getSelectionModel().getSelectedItem();
      if (item == null) {
        return FXCollections.emptyObservableList();
      }
      return FXCollections.singletonObservableList(
          singleCourseSelection.getSelectionModel().getSelectedItem());
    }
  }
}
