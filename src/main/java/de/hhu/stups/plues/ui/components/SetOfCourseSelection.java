package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.ui.layout.Inflater;
import javafx.beans.Observable;
import javafx.beans.binding.ListBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
public class SetOfCourseSelection extends VBox implements Initializable {

  private final ListProperty<Course> selectedCourses;

  private final ListProperty<Course> courses;
  private final ListProperty<SelectableCourse> selectableCourses;

  @FXML
  @SuppressWarnings("unused")
  private TextField txtQuery;
  @FXML
  @SuppressWarnings("unused")
  private TitledPane titledPaneMasterCourse;
  @FXML
  @SuppressWarnings("unused")
  private TitledPane titledPaneBachelorCourse;
  @FXML
  @SuppressWarnings("unused")
  private TableView<SelectableCourse> tableViewMasterCourse;
  @FXML
  @SuppressWarnings("unused")
  private TableView<SelectableCourse> tableViewBachelorCourse;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<SelectableCourse, String> tableColumnMasterCourse;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<SelectableCourse, Boolean> tableColumnMasterCheckBox;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<SelectableCourse, String> tableColumnBachelorCourse;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<SelectableCourse, Boolean> tableColumnBachelorCheckBox;

  /**
   * Component that allows the user to select one or more courses. The courses need to be
   * instantiated via the {@link this#coursesProperty()}. Those are used to highlight all events in
   * the timetable view associated with the courses. Selected courses are stored in the readonly
   * list property {@link this#selectedCoursesProperty()}.
   */
  @Inject
  public SetOfCourseSelection(final Inflater inflater) {
    selectedCourses = new ReadOnlyListWrapper<>(FXCollections.emptyObservableList());

    courses = new SimpleListProperty<>(FXCollections.emptyObservableList());
    selectableCourses = new SimpleListProperty<>(FXCollections.emptyObservableList());

    inflater.inflate("components/SetOfCourseSelection", this, this, "filter", "Column");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {

    tableColumnMasterCheckBox.setCellFactory(
        CheckBoxTableCell.forTableColumn(tableColumnMasterCheckBox));

    tableColumnBachelorCheckBox.setCellFactory(
        CheckBoxTableCell.forTableColumn(tableColumnBachelorCheckBox));

    tableViewMasterCourse.setSelectionModel(null);
    tableViewBachelorCourse.setSelectionModel(null);

    tableViewMasterCourse.setId("batchListView");
    tableViewBachelorCourse.setId("batchListView");

    selectableCourses.bind(new ListBinding<SelectableCourse>() {
      {
        bind(courses);
      }

      @Override
      public void dispose() {
        super.dispose();
        unbind(courses);
      }


      // NOTE: A change to the courses list, this binding is bound to, will recreate all
      // SelectableCourses objects. This behaviour will loose the state of all selectedProperties.
      @Override
      protected ObservableList<SelectableCourse> computeValue() {
        return FXCollections.observableList(
            courses.parallelStream().map(SelectableCourse::new)
                .collect(Collectors.toList()), SelectableCourse.getExtractor());
      }
    });

    tableViewMasterCourse.itemsProperty().bind(newFilterBinding(SelectableCourse::isMaster));
    tableViewBachelorCourse.itemsProperty().bind(newFilterBinding(SelectableCourse::isBachelor));

    tableViewMasterCourse.itemsProperty().addListener(
        (observable, oldValue, newValue)
            -> titledPaneMasterCourse.setExpanded(!newValue.isEmpty()));
    tableViewBachelorCourse.itemsProperty().addListener(
        (observable, oldValue, newValue)
            -> titledPaneBachelorCourse.setExpanded(!newValue.isEmpty()));

    selectedCourses.bind(new ListBinding<Course>() {
      {
        bind(selectableCourses);
      }

      @Override
      public void dispose() {
        super.dispose();
        unbind(selectableCourses);
      }

      @Override
      protected ObservableList<Course> computeValue() {
        return selectableCourses.parallelStream()
            .filter(SelectableCourse::isSelected)
            .map(SelectableCourse::getCourse)
            .collect(Collectors.collectingAndThen(Collectors.toList(),
              FXCollections::observableArrayList));
      }
    });
  }

  private ListBinding<SelectableCourse> newFilterBinding(
      final Predicate<SelectableCourse> predicate) {
    return new ListBinding<SelectableCourse>() {
      {
        bind(selectableCourses, txtQuery.textProperty());
      }

      @Override
      public void dispose() {
        super.dispose();
        unbind(selectableCourses);
      }

      @Override
      protected ObservableList<SelectableCourse> computeValue() {
        return selectableCourses
            .filtered(predicate)
            .filtered(row
                -> row.getName().toLowerCase().contains(txtQuery.getText().toLowerCase()));
      }
    };
  }

  /**
   * Initialize the lists of bachelor and master courses and the table views.
   */
  public void setCourses(final List<Course> courses) {
    this.courses.set(FXCollections.observableList(courses));
  }

  @SuppressWarnings("unused")
  private ObservableList<Course> getCourses() {
    return courses.get();
  }

  @SuppressWarnings("unused")
  public ListProperty<Course> coursesProperty() {
    return courses;
  }

  public ObservableList<Course> getSelectedCourses() {
    return selectedCourses.get();
  }

  public void setSelectedCourses(final List<Course> courses) {
    final HashSet<Course> courseSet = new HashSet<>(courses);
    selectableCourses.forEach(course -> course.setSelected(courseSet.contains(course.getCourse())));
  }

  @SuppressWarnings("unused")
  public ReadOnlyListProperty<Course> selectedCoursesProperty() {
    return selectedCourses;
  }

  TableView<SelectableCourse> getTableViewMasterCourse() {
    return tableViewMasterCourse;
  }

  TableView<SelectableCourse> getTableViewBachelorCourse() {
    return tableViewBachelorCourse;
  }

  public static final class SelectableCourse {
    private final Course course;
    private final BooleanProperty selected = new SimpleBooleanProperty(false);

    SelectableCourse(final Course course) {
      this.course = course;
    }

    private static Callback<SelectableCourse, Observable[]> getExtractor() {
      return (SelectableCourse param) -> new Observable[] {param.selectedProperty()};
    }

    private boolean isSelected() {
      return selected.get();
    }

    @SuppressWarnings("unused")
    private void setSelected(final boolean selected) {
      this.selected.set(selected);
    }

    public BooleanProperty selectedProperty() {
      return selected;
    }

    public Course getCourse() {
      return this.course;
    }

    public String getName() {
      return this.course.getFullName();
    }

    public boolean isMaster() {
      return this.course.isMaster();
    }

    public boolean isBachelor() {
      return this.course.isBachelor();
    }
  }
}
