package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

public class MandatoryModules extends VBox implements Initializable {

  private Map<Course, Set<Module>> mandatoryModules;
  private SimpleListProperty<Course> courses;
  private SimpleListProperty<Module> modules;

  @FXML
  @SuppressWarnings("unused")
  private ListView<Course> listViewCourses;
  @FXML
  @SuppressWarnings("unused")
  private TableView<Module> listViewMandatoryModules;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, String> columnModuleTitle;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, Boolean> columnModuleElectability;

  /**
   * Default constructor.
   * @param inflater Handle fxml and resources
   */
  @Inject
  public MandatoryModules(final Inflater inflater) {
    courses = new SimpleListProperty<>(FXCollections.observableArrayList());
    modules = new SimpleListProperty<>(FXCollections.observableArrayList());

    inflater.inflate("/components/reports/MandatoryModules", this, this, "reports");
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    listViewCourses.itemsProperty().bind(courses);
    listViewCourses.setCellFactory(param -> new ListCell<Course>() {
      @Override
      protected void updateItem(Course course, boolean empty) {
        super.updateItem(course, empty);
        if (!empty) {
          setText(course.getKey());
        }
      }
    });
    listViewMandatoryModules.itemsProperty().bind(modules);
    columnModuleTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
    columnModuleElectability.setCellValueFactory(new PropertyValueFactory<>("mandatory"));

    listViewCourses.getSelectionModel().selectedItemProperty()
        .addListener((observable, oldValue, newValue) ->
          modules.setAll(mandatoryModules.get(newValue)));
  }

  public void setData(final Map<Course, Set<Module>> mandatoryModules) {
    this.mandatoryModules = mandatoryModules;
    courses.addAll(mandatoryModules.keySet());
  }
}
