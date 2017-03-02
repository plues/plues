package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.ui.components.detailview.DetailViewHelper;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.ListBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

public class MandatoryModules extends VBox implements Initializable {

  private final ObservableMap<Course, Set<Module>> mandatoryModulesMap;
  private final SimpleListProperty<Course> courses;
  private final Router router;

  @FXML
  @SuppressWarnings("unused")
  private TableView<Course> tableViewCourses;
  @FXML
  @SuppressWarnings("unused")
  private TableView<Module> tableViewMandatoryModules;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Course, String> tableColumnCourseKey;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Course, String> tableColumnCourseName;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Course, String> tableColumnModulePordnr;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Course, String> tableColumnModuleTitle;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<String, String> tableColumnElectability;
  @FXML
  @SuppressWarnings("unused")
  private Text txtExplanation;

  /**
   * Default constructor.
   */
  @Inject
  public MandatoryModules(final Inflater inflater, final Router router) {
    this.router = router;
    this.courses = new SimpleListProperty<>(FXCollections.observableArrayList());
    this.mandatoryModulesMap = FXCollections.observableHashMap();
    inflater.inflate("components/reports/MandatoryModules", this, this, "reports", "Column");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    tableViewCourses.itemsProperty().bind(courses);
    tableViewCourses.setOnMouseClicked(
        DetailViewHelper.getCourseMouseHandler(tableViewCourses, router));
    //
    tableColumnElectability.setCellValueFactory(param ->
        new SimpleStringProperty("true".equals(param.getValue()) ? "✔︎" : "✗"));
    tableViewMandatoryModules.itemsProperty().bind(
        new ModuleListBinding(tableViewCourses.getSelectionModel().selectedItemProperty(),
            mandatoryModulesMap));
    tableViewMandatoryModules.setOnMouseClicked(
        DetailViewHelper.getModuleMouseHandler(tableViewMandatoryModules, router));
    //
    txtExplanation.wrappingWidthProperty().bind(tableViewCourses.widthProperty().subtract(25.0));
  }

  public void setData(final Map<Course, Set<Module>> mandatoryModulesMap) {
    this.mandatoryModulesMap.putAll(mandatoryModulesMap);
    courses.addAll(mandatoryModulesMap.keySet());
  }

  private static class ModuleListBinding extends ListBinding<Module> {
    private final ReadOnlyObjectProperty<Course> property;
    private final ObservableMap<Course, Set<Module>> map;

    private ModuleListBinding(final ReadOnlyObjectProperty<Course> property,
                              final ObservableMap<Course, Set<Module>> map) {
      this.property = property;
      this.map = map;
      //
      bind(property);
    }

    @Override
    protected ObservableList<Module> computeValue() {
      final Course course = this.property.get();
      return FXCollections.observableArrayList(
          this.map.getOrDefault(course, Collections.emptySet()));
    }
  }
}
