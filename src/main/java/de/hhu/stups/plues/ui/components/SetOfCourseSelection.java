package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ListBinding;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
public class SetOfCourseSelection extends VBox implements Initializable {

  private final ObservableList<TableRowPair<Node, Course>> masterCourses;
  private final ObservableList<TableRowPair<Node, Course>> bachelorCourses;
  private final ReadOnlyListProperty<Course> selectedCourses;

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
  private TableView<TableRowPair<Node, Course>> tableViewMasterCourse;
  @FXML
  @SuppressWarnings("unused")
  private TableView<TableRowPair<Node, Course>> tableViewBachelorCourse;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<TableRowPair<Node, Course>, String> tableColumnMasterCourse;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<TableRowPair<Node, Course>, String> tableColumnMasterCheckBox;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<TableRowPair<Node, Course>, String> tableColumnBachelorCourse;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<TableRowPair<Node, Course>, String> tableColumnBachelorCheckBox;

  /**
   * Component that allows the user to select one or more courses. The courses need to be
   * instantiated by calling {@link this#setCourses(List)}. Those are used to highlight all events
   * in the timetable view associated with the courses. Selected courses are stored in the readonly
   * list property {@link this#selectedCourses} and can be accessed via {@link
   * this#getSelectedCourses()}.
   */
  @Inject
  public SetOfCourseSelection(final Inflater inflater) {
    bachelorCourses = FXCollections.observableArrayList();
    masterCourses = FXCollections.observableArrayList();
    selectedCourses = new ReadOnlyListWrapper<>(FXCollections.observableArrayList());

    inflater.inflate("components/SetOfCourseSelection", this, this, "filter");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {

    final String first = "first";

    tableColumnMasterCheckBox.setResizable(false);
    tableColumnMasterCheckBox.setSortable(false);
    tableColumnMasterCourse.setSortable(false);

    tableColumnMasterCheckBox.setCellValueFactory(new PropertyValueFactory<>(first));
    tableColumnMasterCourse.setCellValueFactory(
        param -> new SimpleStringProperty(param.getValue().getSecond().getFullName()));


    tableColumnBachelorCheckBox.setResizable(false);
    tableColumnBachelorCheckBox.setSortable(false);
    tableColumnBachelorCourse.setSortable(false);

    tableColumnBachelorCheckBox.setCellValueFactory(new PropertyValueFactory<>(first));
    tableColumnBachelorCourse.setCellValueFactory(
        param -> new SimpleStringProperty(param.getValue().getSecond().getFullName()));

    tableViewMasterCourse.setSelectionModel(null);
    tableViewBachelorCourse.setSelectionModel(null);

    tableViewMasterCourse.setId("batchListView");
    tableViewBachelorCourse.setId("batchListView");
    initializeTableViews();
  }

  /**
   * Initialize the lists of bachelor and master courses and the table views.
   *
   * @param courses The list of courses.
   */
  public void setCourses(final List<Course> courses) {
    masterCourses.addAll(FXCollections.observableArrayList(courses.stream()
        .filter(Course::isMaster).map(this::getTableViewItem).collect(Collectors.toList())));
    bachelorCourses.addAll(FXCollections.observableArrayList(courses.stream()
        .filter(Course::isBachelor).map(this::getTableViewItem).collect(Collectors.toList())));

    titledPaneMasterCourse.setExpanded(!masterCourses.isEmpty());
    titledPaneBachelorCourse.setExpanded(!bachelorCourses.isEmpty());
  }

  private void initializeTableViews() {
    tableViewMasterCourse.itemsProperty().bind(new TableRowPairListBinding(masterCourses));
    tableViewBachelorCourse.itemsProperty().bind(new TableRowPairListBinding(bachelorCourses));

    tableViewMasterCourse.prefHeightProperty().bind(
        Bindings.createIntegerBinding(() -> masterCourses.isEmpty() ? 50 : 300, masterCourses));
    tableViewBachelorCourse.prefHeightProperty().bind(
        Bindings.createIntegerBinding(() -> bachelorCourses.isEmpty() ? 50 : 300, bachelorCourses));

    titledPaneMasterCourse.setExpanded(false);
    titledPaneBachelorCourse.setExpanded(false);
  }

  private TableRowPair<Node, Course> getTableViewItem(final Course course) {
    final CheckBox checkBox = new CheckBox();
    final Tooltip tooltip = new Tooltip(course.getFullName());
    checkBox.setTooltip(tooltip);
    checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        selectedCourses.add(course);
      } else {
        selectedCourses.remove(course);
      }
    });
    return new TableRowPair<>(checkBox, course);
  }

  public ReadOnlyListProperty<Course> getSelectedCourses() {
    return selectedCourses;
  }

  TableView<TableRowPair<Node, Course>> getTableViewMasterCourse() {
    return tableViewMasterCourse;
  }

  TableView<TableRowPair<Node, Course>> getTableViewBachelorCourse() {
    return tableViewBachelorCourse;
  }

  public static final class TableRowPair<T1, T2> {
    private final T1 first;
    private final T2 second;

    /**
     * An object to obtain two values to use within a table view.
     */
    TableRowPair(final T1 first, final T2 second) {
      this.first = first;
      this.second = second;
    }

    @Override
    public boolean equals(final Object other) {
      if (this == other) {
        return true;
      }
      if (other == null || getClass() != other.getClass()) {
        return false;
      }
      final TableRowPair<?, ?> pair = (TableRowPair<?, ?>) other;
      return Objects.equals(second, pair.second)
          && Objects.equals(first, pair.first);
    }

    @Override
    public int hashCode() {
      return Objects.hash(second, first);
    }

    @SuppressWarnings("unused")
    public T1 getFirst() {
      return first;
    }

    @SuppressWarnings("unused")
    public T2 getSecond() {
      return second;
    }
  }

  private class TableRowPairListBinding extends ListBinding<TableRowPair<Node, Course>> {

    private final ObservableList<TableRowPair<Node, Course>> courses;

    public TableRowPairListBinding(final ObservableList<TableRowPair<Node, Course>> courses) {
      this.courses = courses;
      bind(courses, txtQuery.textProperty());
    }

    @Override
    protected ObservableList<TableRowPair<Node, Course>> computeValue() {
      return FXCollections.observableArrayList(courses.stream()
        .filter(row -> row.getSecond().getFullName().toLowerCase().contains(txtQuery.getText()))
        .collect(Collectors.toList()));
    }
  }
}
