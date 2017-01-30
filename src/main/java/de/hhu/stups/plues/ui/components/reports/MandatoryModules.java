package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.ListBinding;
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
  public MandatoryModules(final Inflater inflater) {
    courses = new SimpleListProperty<>(FXCollections.observableArrayList());
    mandatoryModulesMap = FXCollections.observableHashMap();
    inflater.inflate("components/reports/MandatoryModules", this, this, "reports", "Column");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    tableViewCourses.itemsProperty().bind(courses);

    tableColumnElectability.setCellValueFactory(param ->
        new SimpleStringProperty("true".equals(param.getValue()) ? "✔︎" : "✗"));

    tableViewMandatoryModules.itemsProperty().bind(new ListBinding<Module>() {
      {
        bind(tableViewCourses.getSelectionModel().selectedItemProperty());
      }

      @Override
      protected ObservableList<Module> computeValue() {
        final Course course = tableViewCourses.getSelectionModel().getSelectedItem();
        return FXCollections.observableArrayList(
            mandatoryModulesMap.getOrDefault(course, Collections.emptySet()));
      }
    });

    txtExplanation.wrappingWidthProperty().bind(tableViewCourses.widthProperty().subtract(25.0));

    bindTableColumnsWidth();
  }


  private void bindTableColumnsWidth() {
    tableColumnCourseKey.prefWidthProperty().bind(
        tableViewCourses.widthProperty().multiply(0.2));
    tableColumnCourseName.prefWidthProperty().bind(
        tableViewCourses.widthProperty().multiply(0.76));
    tableColumnModulePordnr.prefWidthProperty().bind(
        tableViewMandatoryModules.widthProperty().multiply(0.2));
    tableColumnModuleTitle.prefWidthProperty().bind(
        tableViewMandatoryModules.widthProperty().multiply(0.69));
    tableColumnElectability.prefWidthProperty().bind(
        tableViewMandatoryModules.widthProperty().multiply(0.07));
  }


  public void setData(final Map<Course, Set<Module>> mandatoryModulesMap) {
    this.mandatoryModulesMap.putAll(mandatoryModulesMap);
    courses.addAll(mandatoryModulesMap.keySet());
  }
}
