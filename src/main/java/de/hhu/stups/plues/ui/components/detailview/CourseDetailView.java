package de.hhu.stups.plues.ui.components.detailview;

import com.google.common.base.Joiner;
import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.ModuleAbstractUnitSemester;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ListBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;


public class CourseDetailView extends VBox implements Initializable {

  private final Router router;
  private final ObjectProperty<Course> courseProperty;

  @FXML
  @SuppressWarnings("unused")
  private Label key;
  @FXML
  @SuppressWarnings("unused")
  private Label name;
  @FXML
  @SuppressWarnings("unused")
  private TableView<Module> tableViewModules;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, String> semesters;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, String> type;

  /**
   * Default constructor.
   */
  @Inject
  public CourseDetailView(final Inflater inflater, final Router router) {
    courseProperty = new SimpleObjectProperty<>();
    this.router = router;

    inflater.inflate("components/detailview/CourseDetailView", this, this, "detailView", "Column");
  }

  public void setCourse(final Course course) {
    courseProperty.set(course);
  }

  public String getTitle() {
    return name.getText();
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    this.key.textProperty().bind(Bindings.when(courseProperty.isNotNull()).then(
        Bindings.selectString(courseProperty, "key")).otherwise(""));
    this.name.textProperty().bind(Bindings.when(courseProperty.isNotNull()).then(
        Bindings.selectString(courseProperty, "fullName")).otherwise(""));

    this.tableViewModules.itemsProperty().bind(new ListBinding<Module>() {
      {
        bind(courseProperty);
      }

      @Override
      protected ObservableList<Module> computeValue() {
        Course course = courseProperty.get();
        if (course == null) {
          return FXCollections.emptyObservableList();
        }

        return FXCollections.observableArrayList(course.getModules());
      }
    });

    this.semesters.setCellValueFactory(param -> {
      final Set<ModuleAbstractUnitSemester> entries = param.getValue()
          .getModuleAbstractUnitSemesters().stream().filter(moduleAbstractUnitSemester ->
            this.courseProperty.get().getModules().contains(moduleAbstractUnitSemester.getModule()))
          .collect(Collectors.toSet());
      final Set<Integer> semesters = entries.stream()
          .map(ModuleAbstractUnitSemester::getSemester).collect(Collectors.toSet());

      return new ReadOnlyObjectWrapper<>(Joiner.on(",").join(semesters));
    });
    this.semesters.setCellFactory(param -> DetailViewHelper.createTableCell());
    this.type.setCellValueFactory(param -> {
      if (param.getValue().getMandatory()) {
        return new ReadOnlyObjectWrapper<>("m");
      }

      return new ReadOnlyObjectWrapper<>("e");
    });
    this.type.setCellFactory(param -> DetailViewHelper.createTableCell());

    tableViewModules.setOnMouseClicked(DetailViewHelper.getModuleMouseHandler(
        tableViewModules, router));
  }
}
