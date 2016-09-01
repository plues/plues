package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Course;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

public class MajorMinorCourseSelection extends GridPane implements Initializable {

  @FXML
  @SuppressWarnings("unused")
  private ComboBox<Course> cbMajor;

  @FXML
  @SuppressWarnings("unused")
  private ComboBox<Course> cbMinor;

  private ObservableList<Course> initialMinorCourseList;

  /**
   * Create the component containing the combo boxes to choose major and minor courses. The combo
   * boxes will fill the parent's width, therefore wrap the component in a grid pane for example.
   * When using the component we need to initially call {@link #setMajorCourseList(ObservableList)},
   * {@link #setMinorCourseList(ObservableList)}.
   *
   * @param loader The injected FXMLLoader.
   */
  @Inject
  MajorMinorCourseSelection(final FXMLLoader loader) {
    loader.setLocation(getClass().getResource("/fxml/components/MajorMinorCourseSelection.fxml"));

    loader.setRoot(this);
    loader.setController(this);

    try {
      loader.load();
    } catch (final IOException exception) {
      throw new RuntimeException(exception);
    }
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    cbMajor.setConverter(new CourseConverter());
    cbMinor.setConverter(new CourseConverter());

    final ReadOnlyObjectProperty<Course> selectedMajor
        = this.cbMajor.getSelectionModel().selectedItemProperty();

    final BooleanBinding majorIsCombinable;
    majorIsCombinable = Bindings.createBooleanBinding(() -> {
      final Course c = selectedMajor.get();
      return c != null && c.isCombinable();
    }, selectedMajor);

    cbMinor.disableProperty().bind(majorIsCombinable.not());

    // Filter courses from minor course list with a different short name as soon as a
    // major course is selected, so don't allow to select the same major and minor courses.
    cbMajor.valueProperty().addListener(((observable, oldValue, newValue) -> {
      if (initialMinorCourseList != null) {
        filterCbMinorCourses();
      }
    }));

  }

  /**
   * Highlight the impossible courses in red to signalize the user invalid course choices before
   * trying to generate the pdf file. This method is called as soon as the solver is available.
   *
   * @param impossibleCourses The set of impossible courses obtained by {@link
   *                          de.hhu.stups.plues.prob.Solver#getImpossibleCourses()
   *                          getImpossibleCourses}.
   */
  public void highlightImpossibleCourses(final Set<String> impossibleCourses) {
    cbMajor.setCellFactory(getCallbackForImpossibleCourses(impossibleCourses));
    cbMinor.setCellFactory(getCallbackForImpossibleCourses(impossibleCourses));
  }

  /**
   * Create callback for a given set of impossible courses to use as the combo box's cell factory.
   * This is necessary to change the item's color later on.
   *
   * @param impossibleCourses The set of impossible courses obtained by {@link
   *                          #highlightImpossibleCourses(Set) highlightImpossibleCourses}.
   * @return The callback providing the updated list cells of courses.
   */
  private Callback<ListView<Course>, ListCell<Course>> getCallbackForImpossibleCourses(
      final Set<String> impossibleCourses) {
    return new Callback<ListView<Course>, ListCell<Course>>() {
      @Override
      public ListCell<Course> call(final ListView<Course> listView) {
        return new ListCell<Course>() {
          @Override
          protected void updateItem(final Course item, final boolean empty) {
            super.updateItem(item, empty);

            if (item != null) {
              setText(item.getFullName());

              if (impossibleCourses.contains(item.getName())) {
                setTextFill(Color.RED);
              } else {
                setTextFill(Color.BLACK);
              }

            }

          }
        };
      }
    };
  }

  public Course getSelectedMajorCourse() {
    return cbMajor.getSelectionModel().getSelectedItem();
  }

  /**
   * @return Return the selected minor course or an empty Optional in case the combo box is
   *         disabled.
   */
  public final Optional<Course> getSelectedMinorCourse() {
    if (this.cbMinor.isDisabled()) {
      return Optional.empty();
    } else {
      return Optional.of(this.cbMinor.getSelectionModel()
          .getSelectedItem());
    }
  }

  ComboBox<Course> getMajorComboBox() {
    return this.cbMajor;
  }

  ComboBox<Course> getMinorComboBox() {
    return this.cbMinor;
  }

  /**
   * Set the initial minor course list. We need to store this list to be able to filter the possible
   * minor courses according to the currently chosen major course.
   */
  public void setMinorCourseList(final ObservableList<Course> initialMinorCourseList) {
    this.initialMinorCourseList = initialMinorCourseList;
    filterCbMinorCourses();
  }

  public void setMajorCourseList(final ObservableList<Course> majorCourseList) {
    cbMajor.setItems(majorCourseList);
    cbMajor.getSelectionModel().select(0);
  }

  private void filterCbMinorCourses() {
    final Course major = getSelectedMajorCourse();
    if (major == null) {
      return;
    }
    final String majorShortName = major.getShortName();
    final FilteredList<Course> minorCourseList = (initialMinorCourseList.filtered(
        course -> !course.getShortName().equals(majorShortName)));

    cbMinor.setItems(minorCourseList);
    cbMinor.getSelectionModel().select(0);
  }

  /**
   * Convert from String to Course and vice versa to be able to use the Course objects directly
   * within the combo boxes.
   */
  private static class CourseConverter extends StringConverter<Course> {
    @Override
    public String toString(final Course object) {
      return object.getFullName();
    }

    @Override
    public Course fromString(final String string) {
      throw new RuntimeException();
    }
  }
}
