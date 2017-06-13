package de.hhu.stups.plues.ui.components.detailview;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;


public class CourseDetailView extends VBox implements Initializable, DetailView {

  private final ObjectProperty<Course> courseProperty;
  private final Router router;

  private ResourceBundle resources;

  @FXML
  @SuppressWarnings("unused")
  private Label key;
  @FXML
  @SuppressWarnings("unused")
  private Label name;
  @FXML
  @SuppressWarnings("unused")
  private Label po;
  @FXML
  @SuppressWarnings("unused")
  private Label kzfa;
  @FXML
  @SuppressWarnings("unused")
  private Label degree;
  @FXML
  @SuppressWarnings("unused")
  private Label lbMajorOrMinorCourses;
  @FXML
  @SuppressWarnings("unused")
  private TableView<Course> tableViewMajorOrMinorCourses;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Course, String> tableColumnCourseKey;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Course, String> tableColumnCourseName;

  @FXML
  @SuppressWarnings("unused")
  private Label creditPoints;

  /**
   * Default constructor.
   */
  @Inject
  public CourseDetailView(final Inflater inflater,
                          final Router router) {
    this.router = router;
    courseProperty = new SimpleObjectProperty<>();

    inflater.inflate("components/detailview/CourseDetailView", this, this, "detailView", "Column");
  }

  public void setCourse(final Course course) {
    courseProperty.set(course);
  }

  public String getTitle() {
    return name.getText();
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    this.resources = resources;
    this.key.textProperty().bind(Bindings.when(courseProperty.isNotNull()).then(
        Bindings.selectString(courseProperty, "key")).otherwise(""));

    this.name.textProperty().bind(Bindings.when(courseProperty.isNotNull()).then(
        Bindings.selectString(courseProperty, "fullName")).otherwise(""));

    this.po.textProperty().bind(Bindings.when(courseProperty.isNotNull()).then(
        Bindings.selectString(courseProperty, "po")).otherwise(""));

    this.kzfa.textProperty().bind(Bindings.when(courseProperty.isNotNull()).then(
        Bindings.selectString(courseProperty, "kzfa")).otherwise(""));

    this.degree.textProperty().bind(Bindings.when(courseProperty.isNotNull()).then(
        Bindings.selectString(courseProperty, "degree")).otherwise(""));

    final IntegerBinding cp = Bindings.selectInteger(courseProperty, "creditPoints");
    this.creditPoints.textProperty().bind(
        Bindings.when(cp.greaterThan(0)).then(cp.asString()).otherwise("-"));

    courseProperty.addListener((observable, oldValue, newValue) -> initializeTableView());
  }

  private void initializeTableView() {
    tableColumnCourseKey.setCellValueFactory(value ->
        new ReadOnlyStringWrapper(value.getValue().getKey()));
    tableColumnCourseName.setCellValueFactory(value ->
        new ReadOnlyStringWrapper(value.getValue().getFullName()));
    tableViewMajorOrMinorCourses.setOnMouseClicked(this::tableViewMouseClicked);
    // add major or minor courses according to the current course
    final Course currentCourse = courseProperty.get();
    tableViewMajorOrMinorCourses.getItems().addAll(currentCourse.isMajor()
        ? currentCourse.getMinorCourses() : currentCourse.getMajorCourses());
    lbMajorOrMinorCourses.setText(courseProperty.get().isMajor()
        ? resources.getString("minorCourses") : resources.getString("majorCourses"));
  }

  private void tableViewMouseClicked(final MouseEvent mouseEvent) {
    if (mouseEvent.getClickCount() < 2) {
      return;
    }
    router.transitionTo(RouteNames.COURSE_DETAIL_VIEW,
        tableViewMajorOrMinorCourses.getSelectionModel().getSelectedItem());
  }
}
