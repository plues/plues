package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.ui.layout.Inflater;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ListBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Callback;

import org.controlsfx.control.textfield.CustomTextField;

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
  private CustomTextField txtQuery;
  @FXML
  @SuppressWarnings("unused")
  private Button btClearSelection;
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
  private TableColumn<SelectableCourse, Boolean> tableColumnMasterCheckBox;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<SelectableCourse, Boolean> tableColumnMasterCourseKey;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<SelectableCourse, Boolean> tableColumnMasterCourseTitle;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<SelectableCourse, Boolean> tableColumnBachelorCheckBox;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<SelectableCourse, Boolean> tableColumnBachelorCourseKey;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<SelectableCourse, Boolean> tableColumnBachelorCourseTitle;

  /**
   * Component that allows the user to select one or more courses. The courses need to be
   * instantiated via the {@link this#coursesProperty()}. Those are used to highlight all events
   * in the timetable view associated with the courses. Selected courses are stored in the
   * readonly list property {@link this#selectedCoursesProperty()}.
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
    txtQuery.setLeft(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.SEARCH, "12"));

    tableColumnMasterCheckBox.setCellFactory(
        CheckBoxTableCell.forTableColumn(tableColumnMasterCheckBox));

    tableColumnBachelorCheckBox.setCellFactory(
        CheckBoxTableCell.forTableColumn(tableColumnBachelorCheckBox));

    tableViewMasterCourse.setSelectionModel(null);
    tableViewBachelorCourse.setSelectionModel(null);

    tableViewMasterCourse.setId("batchListView");
    tableViewBachelorCourse.setId("batchListView");

    initializeSearch();

    selectableCourses.bind(new SelectableCoursesBinding());

    courses.addListener((observable, oldValue, newValue) -> hideEmptyCourseList());

    tableViewMasterCourse.itemsProperty().bind(newFilteredProperty(SelectableCourse::isMaster));
    tableViewBachelorCourse.itemsProperty().bind(newFilteredProperty(SelectableCourse::isBachelor));

    tableViewMasterCourse.itemsProperty().addListener(observable
        -> titledPaneMasterCourse.setExpanded(!tableViewMasterCourse.getItems().isEmpty()));
    tableViewBachelorCourse.itemsProperty().addListener(observable
        -> titledPaneBachelorCourse.setExpanded(!tableViewBachelorCourse.getItems().isEmpty()));

    // last column is bound to take the remaining space
    tableColumnBachelorCourseTitle.prefWidthProperty().bind(
        tableViewBachelorCourse.widthProperty()
            .subtract(tableColumnBachelorCheckBox.widthProperty())
            .subtract(tableColumnBachelorCourseKey.widthProperty())
            .subtract(20));

    tableColumnMasterCourseTitle.prefWidthProperty().bind(
        tableViewMasterCourse.widthProperty()
            .subtract(tableColumnMasterCheckBox.widthProperty())
            .subtract(tableColumnMasterCourseKey.widthProperty())
            .subtract(20));

    selectedCourses.bind(new SelectedCoursesBinding());
  }

  private void initializeSearch() {
    btClearSelection.graphicProperty().bind(Bindings.createObjectBinding(()
        -> FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.TIMES_CIRCLE, "15")));
  }

  private void hideEmptyCourseList() {
    final boolean hasMaster = courses.stream().anyMatch(Course::isMaster);
    final boolean hasBachelor = courses.stream().anyMatch(Course::isBachelor);
    if (!hasMaster) {
      getChildren().remove(titledPaneMasterCourse);
    } else if (!getChildren().contains(titledPaneMasterCourse)) {
      getChildren().add(2, titledPaneMasterCourse);
    }

    if (!hasBachelor) {
      getChildren().remove(titledPaneBachelorCourse);
    } else if (!getChildren().contains(titledPaneBachelorCourse)) {
      getChildren().add(titledPaneBachelorCourse);
    }
  }

  @FXML
  @SuppressWarnings("unused")
  private void btClearSelectionSubmit() {
    txtQuery.clear();
    selectableCourses.forEach(course -> course.setSelected(false));
  }

  private ListProperty<SelectableCourse> newFilteredProperty(
      final Predicate<SelectableCourse> predicate) {

    final FilteredList<SelectableCourse> filter
        = new FilteredList<>(selectableCourses.filtered(predicate));

    filter.predicateProperty().bind(Bindings.createObjectBinding(
        () -> row -> row.matches(txtQuery.getText().toLowerCase()), txtQuery.textProperty()));
    return new SimpleListProperty<>(filter);
  }

  /**
   * Initialize the lists of bachelor and master courses and the table views.
   */
  public void setCourses(final List<Course> courses) {
    this.courses.set(FXCollections.observableList(courses));
    // set the default table column width for keys
    tableColumnMasterCourseKey.setPrefWidth(getMaximumKeyStringWidth(tableViewMasterCourse));
    tableColumnBachelorCourseKey.setPrefWidth(getMaximumKeyStringWidth(tableViewBachelorCourse));
  }

  /**
   * Return the largest {@link SelectableCourse#getKey() key} width of a given table view.
   */
  private double getMaximumKeyStringWidth(final TableView<SelectableCourse> tableView) {
    final Text tempText = new Text();
    double maxWidth = 0;
    for (final SelectableCourse selectableCourse : tableView.getItems()) {
      tempText.setText(selectableCourse.getKey());
      final double keyWidth = tempText.getBoundsInLocal().getWidth();
      if (keyWidth > maxWidth) {
        maxWidth = keyWidth;
      }
    }
    return maxWidth + 10.0;
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

  /**
   * Set the list of currently selected courses (checkbox in the UI is selected).
   */
  public void setSelectedCourses(final List<Course> courses) {
    // We put the courses in a HashSet here to avoid the linear scan of the list in the membership
    // check below
    final HashSet<Course> courseSet = new HashSet<>(courses);
    selectableCourses.forEach(course -> course.setSelected(courseSet.contains(course.getCourse())));
    scrollToCourses(courses);
  }

  private void scrollToCourses(final List<Course> courses) {
    selectableCourses.stream().filter(selectableCourse ->
        courses.contains(selectableCourse.getCourse()) && selectableCourse.getCourse().isBachelor())
        .findFirst().ifPresent(tableViewBachelorCourse::scrollTo);
    selectableCourses.stream().filter(selectableCourse ->
        courses.contains(selectableCourse.getCourse()) && selectableCourse.getCourse().isMaster())
        .findFirst().ifPresent(tableViewMasterCourse::scrollTo);
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

    @SuppressWarnings("unused")
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

    public String getKey() {
      return this.course.getKey();
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

    @SuppressWarnings("unused")
    private boolean matches(final String query) {
      return this.getName().toLowerCase().contains(query)
          || this.getKey().toLowerCase().contains(query);
    }
  }

  private class SelectedCoursesBinding extends ListBinding<Course> {

    SelectedCoursesBinding() {
      bind(selectableCourses);
    }

    @Override
    public void dispose() {
      super.dispose();
      unbind(selectableCourses);
    }

    @Override
    protected ObservableList<Course> computeValue() {
      return selectableCourses.stream()
          .filter(SelectableCourse::isSelected)
          .map(SelectableCourse::getCourse)
          .collect(Collectors.collectingAndThen(Collectors.toList(),
              FXCollections::observableArrayList));
    }
  }

  private class SelectableCoursesBinding extends ListBinding<SelectableCourse> {

    SelectableCoursesBinding() {
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
          courses.stream().map(SelectableCourse::new)
              .collect(Collectors.toList()), SelectableCourse.getExtractor());
    }
  }
}
