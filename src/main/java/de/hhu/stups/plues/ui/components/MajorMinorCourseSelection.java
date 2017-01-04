package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ListBinding;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

public class MajorMinorCourseSelection extends GridPane implements Initializable, Observable {

  private final List<InvalidationListener> listeners = new ArrayList<>();
  // input properties
  private final ListProperty<Course> majorCourseList = new SimpleListProperty<>();
  private final ListProperty<Course> minorCourseList = new SimpleListProperty<>();
  private final SetProperty<Course> impossibleCoursesProperty = new SimpleSetProperty<>();
  // output properties
  private final ObjectProperty<Course> selectedMajor = new SimpleObjectProperty<>();
  private final ObjectProperty<Course> selectedMinor = new SimpleObjectProperty<>();
  private final ListProperty<Course> selectedCourses = new SimpleListProperty<>();
  @FXML
  @SuppressWarnings("unused")
  private ComboBox<Course> cbMajor;
  @FXML
  @SuppressWarnings("unused")
  private ComboBox<Course> cbMinor;
  @FXML
  @SuppressWarnings("unused")
  private ColumnConstraints columnConstraints;

  /**
   * Create the component containing the combo boxes to choose major and minor courses. When using
   * the component we need to initially call {@link #setMajorCourseList(ObservableList)} and {@link
   * #setMinorCourseList(ObservableList)}. As soon as the solver is available the impossible courses
   * can be highlighted via the {@link #impossibleCoursesProperty} property.
   *
   * @param inflater Inflater to handle fxml.
   */
  @Inject
  MajorMinorCourseSelection(final Inflater inflater) {
    inflater.inflate("components/MajorMinorCourseSelection", this, this);
  }

  @SuppressWarnings("unused")
  public Course getSelectedMajor() {
    return selectedMajor.get();
  }

  @SuppressWarnings("unused")
  public ObjectProperty<Course> selectedMajorProperty() {
    return selectedMajor;
  }

  @SuppressWarnings("unused")
  public Course getSelectedMinor() {
    return selectedMinor.get();
  }

  @SuppressWarnings("unused")
  public ObjectProperty<Course> selectedMinorProperty() {
    return selectedMinor;
  }

  @SuppressWarnings("unused")
  public ObservableList<Course> getSelectedCourses() {
    return selectedCourses.get();
  }

  @SuppressWarnings("unused")
  public ReadOnlyListProperty<Course> selectedCoursesProperty() {
    return selectedCourses;
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    cbMajor.setConverter(new CourseConverter());
    cbMinor.setConverter(new CourseConverter());

    cbMajor.valueProperty().addListener((observable, oldValue, newValue)
        -> fireListenerEvents());
    cbMinor.valueProperty().addListener((observable, oldValue, newValue)
        -> fireListenerEvents());

    majorCourseList.addListener((observable, oldValue, newValue)
        -> cbMajor.getSelectionModel().select(0));

    cbMinor.itemsProperty().addListener((observable, oldValue, newValue)
        -> cbMinor.getSelectionModel().selectFirst());

    cbMajor.itemsProperty().bind(majorCourseList);
    cbMinor.itemsProperty().bind(new ListBinding<Course>() {
      {
        bind(cbMajor.getSelectionModel().selectedItemProperty(), minorCourseList);
      }

      @Override
      public void dispose() {
        super.dispose();
        unbind(cbMajor.getSelectionModel().selectedItemProperty(), minorCourseList);
      }

      @Override
      protected ObservableList<Course> computeValue() {
        final Course major = getSelectedMajor();
        if (major == null) {
          return minorCourseList;
        }
        return minorCourseList.filtered(major::isCombinableWith);
      }
    });

    final ReadOnlyObjectProperty<Course> selectedMajorProperty
        = this.cbMajor.getSelectionModel().selectedItemProperty();

    cbMinor.disableProperty().bind(
        Bindings.selectBoolean(selectedMajorProperty, "combinable").not());

    impossibleCoursesProperty.addListener((observable, oldValue, newValue) -> {
      cbMajor.setCellFactory(getCallbackForImpossibleCourses(newValue));
      cbMinor.setCellFactory(getCallbackForImpossibleCourses(newValue));
    });

    this.selectedMajor.bind(cbMajor.getSelectionModel().selectedItemProperty());
    this.selectedMinor.bind(Bindings.when(cbMinor.disabledProperty().not())
        .then(cbMinor.getSelectionModel().selectedItemProperty())
        .otherwise(new SimpleObjectProperty<>(null)));
    this.selectedCourses.bind(new ListBinding<Course>() {
      {
        bind(selectedMajor, selectedMinor);
      }

      @Override
      public void dispose() {
        super.dispose();
        unbind(selectedMajor, selectedMinor);
      }

      @Override
      protected ObservableList<Course> computeValue() {
        final ObservableList<Course> result
            = FXCollections.observableArrayList(selectedMajor.get());
        final Course minor = selectedMinor.get();
        if (minor != null) {
          result.add(minor);
        }
        return result;
      }
    });
  }

  /**
   * Create callback for a given set of impossible courses to use as the combo box's cell factory.
   * This is necessary to change the item's color later on.
   *
   * @param impossibleCourses The set of impossible courses to be highlighted
   * @return The callback providing the updated list cells of courses.
   */
  Callback<ListView<Course>, ListCell<Course>> getCallbackForImpossibleCourses(
      final Set<Course> impossibleCourses) {
    return new ListViewListCellCallback(impossibleCourses);
  }

  ComboBox<Course> getMajorComboBox() {
    return this.cbMajor;
  }

  ComboBox<Course> getMinorComboBox() {
    return this.cbMinor;
  }

  /**
   * Select the given course either in the {@link #cbMajor major} or the {@link #cbMinor minor}
   * course selection.
   */
  public void selectCourse(final Course course) {
    if (course.isMajor()) {
      cbMajor.getSelectionModel().select(course);
    } else {
      cbMinor.getSelectionModel().select(course);
    }
  }

  private void fireListenerEvents() {
    for (final InvalidationListener listener : listeners) {
      listener.invalidated(this);
    }
  }

  /**
   * Set the percent width of this component according to the node it is placed in.
   */
  void setPercentWidth(final double percentWidth) {
    columnConstraints.setPercentWidth(percentWidth);
  }

  @Override
  public void addListener(final InvalidationListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(final InvalidationListener listener) {
    listeners.remove(listener);
  }

  public SetProperty<Course> impossibleCoursesProperty() {
    return this.impossibleCoursesProperty;
  }

  @SuppressWarnings("unused")
  public void setImpossibleCourses(final ObservableSet<Course> impossibleCourses) {
    this.impossibleCoursesProperty.set(impossibleCourses);
  }


  @SuppressWarnings("WeakerAccess")
  public ListProperty<Course> majorCourseListProperty() {
    return this.majorCourseList;
  }

  @SuppressWarnings("WeakerAccess")
  public ListProperty<Course> minorCourseListProperty() {
    return this.minorCourseList;
  }

  public void setMajorCourseList(final ObservableList<Course> majorCourseList) {
    this.majorCourseList.set(majorCourseList);
  }

  public void setMinorCourseList(final ObservableList<Course> minorCourseList) {
    this.minorCourseList.set(minorCourseList);
  }

  private static class ListViewListCellCallback
      implements Callback<ListView<Course>, ListCell<Course>> {

    private final Set<Course> impossibleCourses;

    ListViewListCellCallback(final Set<Course> impossibleCourses) {
      this.impossibleCourses = impossibleCourses;
    }

    @Override
    public ListCell<Course> call(final ListView<Course> listView) {
      return new ListCell<Course>() {
        @Override
        protected void updateItem(final Course item, final boolean empty) {
          super.updateItem(item, empty);

          if (item != null) {
            setText(item.getFullName());

            if (impossibleCourses.contains(item)) {
              setTextFill(Color.RED);
            } else {
              setTextFill(Color.BLACK);
            }
          }
        }
      };
    }
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
      throw new UnsupportedOperationException();
    }
  }
}
