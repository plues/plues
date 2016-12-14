package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.ui.layout.Inflater;
import javafx.beans.binding.ListBinding;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

public class MandatoryModules extends VBox implements Initializable {

  private final ObservableMap<Course, Set<Module>> mandatoryModules;
  private final SimpleListProperty<Course> courses;

  @FXML
  private TableView<Course> tableViewcourses;
  @FXML
  private TableColumn<Course, String> tableColumnCourseName;
  @FXML
  private TableColumn<Course, String> tableColumnCourseFullName;
  @FXML
  private TableView<Module> tableViewMandatoryModules;
  @FXML
  private TableColumn<Module, String> columnModuleTitle;
  @FXML
  private TableColumn<Module, Boolean> columnModuleElectability;
  @FXML
  private TableColumn<Module, String> columnModulePordnr;

  /**
   * Default constructor.
   *
   * @param inflater Handle fxml and resources
   */
  @Inject
  public MandatoryModules(final Inflater inflater) {
    courses = new SimpleListProperty<>(FXCollections.observableArrayList());
    mandatoryModules = FXCollections.observableHashMap();
    inflater.inflate("components/reports/MandatoryModules", this, this, "reports", "Column");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    tableColumnCourseName.setCellValueFactory(new PropertyValueFactory<>("key"));
    tableColumnCourseFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));

    columnModuleTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
    columnModuleElectability.setCellValueFactory(new PropertyValueFactory<>("mandatory"));
    columnModulePordnr.setCellValueFactory(new PropertyValueFactory<>("pordnr"));

    tableViewcourses.itemsProperty().bind(courses);
    tableViewMandatoryModules.itemsProperty().bind(new ListBinding<Module>() {
      {
        bind(tableViewcourses.getSelectionModel().selectedItemProperty());
      }

      @Override
      protected ObservableList<Module> computeValue() {
        final Course course =  tableViewcourses.getSelectionModel().getSelectedItem();
        return FXCollections.observableArrayList(
            mandatoryModules.getOrDefault(course, Collections.emptySet()));
      }
    });

  }

  public void setData(final Map<Course, Set<Module>> mandatoryModules) {
    this.mandatoryModules.putAll(mandatoryModules);
    courses.addAll(mandatoryModules.keySet());
  }
}
