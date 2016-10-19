package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class SetOfCourseSelection extends VBox implements Initializable {

  private final SimpleBooleanProperty storeProperty;
  private final List<Course> masterCourses;
  private final List<Course> bachelorCourses;
  private final ObservableList<Course> selectedCourses;

  @FXML
  @SuppressWarnings("unused")
  private TitledPane titledPaneMasterCourse;
  @FXML
  @SuppressWarnings("unused")
  private TitledPane titledPaneBachelorCourse;
  @FXML
  @SuppressWarnings("unused")
  private TableView<TableRowPair<Node, String>> tableViewMasterCourse;
  @FXML
  @SuppressWarnings("unused")
  private TableView<TableRowPair<Node, String>> tableViewBachelorCourse;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<TableRowPair<Node, String>, String> tableColumnMasterCourse;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<TableRowPair<Node, String>, String> tableColumnMasterCheckBox;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<TableRowPair<Node, String>, String> tableColumnBachelorCourse;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<TableRowPair<Node, String>, String> tableColumnBachelorCheckBox;

  /**
   * Component that allows the user to select one or more courses. Those are used to highlight all
   * events in the timetable view associated with the courses. Selected courses are stored in the
   * observable list {@link this#selectedCourses}.
   */
  @Inject
  public SetOfCourseSelection(final Inflater inflater,
                              final Delayed<Store> delayedStore) {
    storeProperty = new SimpleBooleanProperty(false);
    bachelorCourses = new ArrayList<>();
    masterCourses = new ArrayList<>();
    selectedCourses = FXCollections.observableArrayList();

    delayedStore.whenAvailable(store -> {
      bachelorCourses.addAll(store.getCourses().stream()
          .filter(Course::isBachelor).collect(Collectors.toList()));
      masterCourses.addAll(store.getCourses().stream()
          .filter(Course::isMaster).collect(Collectors.toList()));
      storeProperty.set(true);
      initializeTableViews();
    });

    inflater.inflate("components/SetOfCourseSelection", this, this);
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    final String first = "first";
    final String second = "second";
    tableColumnMasterCheckBox.setResizable(false);
    tableColumnMasterCheckBox.setCellValueFactory(new PropertyValueFactory<>(first));
    tableColumnMasterCourse.setCellValueFactory(new PropertyValueFactory<>(second));

    tableColumnBachelorCheckBox.setResizable(false);
    tableColumnBachelorCheckBox.setCellValueFactory(new PropertyValueFactory<>(first));
    tableColumnBachelorCourse.setCellValueFactory(new PropertyValueFactory<>(second));

    tableViewMasterCourse.setSelectionModel(null);
    tableViewBachelorCourse.setSelectionModel(null);

    tableViewMasterCourse.setId("batchListView");
    tableViewBachelorCourse.setId("batchListView");
  }

  private void initializeTableViews() {
    masterCourses.forEach(course -> tableViewMasterCourse.getItems()
        .add(getTableViewItem(course)));
    bachelorCourses.forEach(course -> tableViewBachelorCourse.getItems()
        .add(getTableViewItem(course)));
    tableViewMasterCourse.setPrefHeight(masterCourses.isEmpty() ? 50 : 300);
    tableViewBachelorCourse.setPrefHeight(bachelorCourses.isEmpty() ? 50 : 300);
  }

  private TableRowPair<Node, String> getTableViewItem(Course course) {
    CheckBox checkBox = new CheckBox();
    checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        selectedCourses.add(course);
      } else {
        selectedCourses.remove(course);
      }
    });
    return new TableRowPair<>(checkBox, course.getFullName());
  }

  public ObservableList<Course> getSelectedCourses() {
    return selectedCourses;
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
}
