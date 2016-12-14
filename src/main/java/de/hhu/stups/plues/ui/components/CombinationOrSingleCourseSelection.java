package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ListBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class CombinationOrSingleCourseSelection extends VBox implements Initializable {

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
   * This component offers the {@link MajorMinorCourseSelection} as well as the possibility to
   * select a single course from a given list. The current selection can be toggled via radio
   * buttons. When using the component we need to initialize the courses with the use of {@link
   * #setCourses(List)}. The selected combination of courses or a single course is stored in an
   * {@link #selectedCourses observable list} and can be accessed via {@link #getSelectedCourses}.
   * Impossible courses can be initialized via {@link #impossibleCoursesProperty}.
   */
  @Inject
  public CombinationOrSingleCourseSelection(final Inflater inflater) {
    selectedCourses = new SimpleListProperty<>();
    toggleGroup = new ToggleGroup();

    coursesProperty = new SimpleListProperty<>(FXCollections.emptyObservableList());
    impossibleCoursesProperty = new SimpleSetProperty<>(FXCollections.emptyObservableSet());

    setSpacing(5.0);

    inflater.inflate("components/CombinationOrSingleCourseSelection", this, this,
        "combinationOrSingleCourseSelection");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    majorMinorCourseSelection.setPercentWidth(100.0);
    majorMinorCourseSelection.impossibleCoursesProperty().bind(impossibleCoursesProperty);

    singleCourseSelection.cellFactoryProperty().bind(
        new ObjectBinding<Callback<ListView<Course>, ListCell<Course>>>() {
          {
            bind(impossibleCoursesProperty);
          }

          @Override
          protected Callback<ListView<Course>, ListCell<Course>> computeValue() {
            return
                majorMinorCourseSelection.getCallbackForImpossibleCourses(getImpossibleCourses());
          }
        });

    rbCombination.setToggleGroup(toggleGroup);
    rbSingleSelection.setToggleGroup(toggleGroup);

    singleCourseSelection.itemsProperty().bind(coursesProperty);
    singleCourseSelection.itemsProperty().addListener((observable, oldValue, newValue) -> {
      singleCourseSelection.getSelectionModel().selectFirst();
    });

    rbCombination.setSelected(true);
    rbCombination.disableProperty().bind(coursesProperty.emptyProperty());
    rbSingleSelection.disableProperty().bind(coursesProperty.emptyProperty());

    majorMinorCourseSelection.disableProperty().bind(
        rbCombination.selectedProperty().not().or(coursesProperty.emptyProperty()));
    singleCourseSelection.disableProperty().bind(
        rbSingleSelection.selectedProperty().not().or(coursesProperty.emptyProperty()));

    majorMinorCourseSelection.majorCourseListProperty()
        .bind(new SimpleListProperty<>(coursesProperty.filtered(Course::isMajor)));
    majorMinorCourseSelection.minorCourseListProperty()
        .bind(new SimpleListProperty<>(coursesProperty.filtered(Course::isMinor)));

    selectedCourses.bind(Bindings.when(rbSingleSelection.selectedProperty())
        .then(new ListBinding<Course>() {
          {
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
        }).otherwise(
            (ObservableList<Course>) majorMinorCourseSelection.selectedCoursesProperty()));
  }

  /**
   * Select courses within the {@link #majorMinorCourseSelection} if two combinable courses or a
   * standalone course is given. If only one combinable course is given use the {@link
   * #singleCourseSelection}.
   */
  public void selectCourses(Course... courses) {
    if (courses.length == 1 && courses[0].isCombinable()) {
      rbSingleSelection.setSelected(true);
      singleCourseSelection.getSelectionModel().select(courses[0]);
    } else {
      rbCombination.setSelected(true);
      if (courses.length > 0) {
        majorMinorCourseSelection.selectMajorCourse(courses[0]);
      }
      if (courses.length > 1) {
        majorMinorCourseSelection.selectMinorCourse(courses[1]);
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
}
