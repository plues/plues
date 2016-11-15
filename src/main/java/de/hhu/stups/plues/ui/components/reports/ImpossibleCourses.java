package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.ui.layout.Inflater;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.beans.binding.ListBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

public class ImpossibleCourses extends VBox implements Initializable {

  private SimpleListProperty<Course> impossibleCoursesList;
  private SimpleListProperty<Course> impossibleCoursesBecauseOfImpossibleModulesList;
  private SimpleListProperty<Course> impossibleCoursesBecauseOfImpossibleModuleCombinationsList;

  @FXML
  @SuppressWarnings("unused")
  private Label explanation;

  @FXML
  @SuppressWarnings("unused")
  private TableView<Course> tableViewImpossibleCourses;

  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Course, String> tableColumnCourseName;

  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Course, String> tableColumnCourseFullName;

  @FXML
  @SuppressWarnings("unused")
  private ToggleButton buttonImpossibleCourses;

  @FXML
  @SuppressWarnings("unused")
  private ToggleButton buttonImpossibleCoursesBecauseOfImpossibleModules;

  @FXML
  @SuppressWarnings("unused")
  private ToggleButton buttonImpossibleCoursesBecauseOfImpossibleModuleCombinations;

  @Inject
  public ImpossibleCourses(final Inflater inflater) {
    impossibleCoursesList = new SimpleListProperty<>(FXCollections.observableArrayList());
    impossibleCoursesBecauseOfImpossibleModulesList =
        new SimpleListProperty<>(FXCollections.observableArrayList());
    impossibleCoursesBecauseOfImpossibleModuleCombinationsList =
        new SimpleListProperty<>(FXCollections.observableArrayList());

    inflater.inflate("/components/reports/ImpossibleCourses", this, this, "reports");
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    ListBinding<Course> binding = new ListBinding<Course>() {
      {
        bind(buttonImpossibleCourses.selectedProperty());
        bind(buttonImpossibleCoursesBecauseOfImpossibleModules.selectedProperty());
        bind(buttonImpossibleCoursesBecauseOfImpossibleModuleCombinations.selectedProperty());
        bind(impossibleCoursesList);
        bind(impossibleCoursesBecauseOfImpossibleModulesList);
        bind(impossibleCoursesBecauseOfImpossibleModuleCombinationsList);
      }

      @Override
      protected ObservableList<Course> computeValue() {
        if (buttonImpossibleCourses.isSelected()) {
          return impossibleCoursesList;
        } else {
          if (buttonImpossibleCoursesBecauseOfImpossibleModules.isSelected()) {
            return impossibleCoursesBecauseOfImpossibleModulesList;
          } else {
            if (buttonImpossibleCoursesBecauseOfImpossibleModuleCombinations.isSelected()) {
              return impossibleCoursesBecauseOfImpossibleModuleCombinationsList;
            }
          }
        }

        return null;
      }
    };
    tableViewImpossibleCourses.itemsProperty().bind(binding);

    tableColumnCourseName.setCellValueFactory(new PropertyValueFactory<>("key"));
    tableColumnCourseFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));

    StringBinding stringBinding = new StringBinding() {
      {
        bind(buttonImpossibleCourses.selectedProperty());
        bind(buttonImpossibleCoursesBecauseOfImpossibleModules.selectedProperty());
        bind(buttonImpossibleCoursesBecauseOfImpossibleModuleCombinations.selectedProperty());
      }
      @Override
      protected String computeValue() {
        String string;
        if (buttonImpossibleCourses.isSelected()) {
          string = resources.getString("explainImpossibleCourses");
        } else {
          if (buttonImpossibleCoursesBecauseOfImpossibleModules.isSelected()) {
            string = resources.getString("explainImpossibleCoursesBecauseOfImpossibleModules");
          } else {
            if (buttonImpossibleCoursesBecauseOfImpossibleModuleCombinations.isSelected()) {
              string = resources.getString(
                  "explainImpossibleCoursesBecauseOfImpossibleModuleCombinations");
            } else {
              string = null;
            }
          }
        }
        return string;
      }
    };
    explanation.textProperty().bind(stringBinding);
  }

  public void setData(final List<Course> impossibleCourses,
                      final List<Course> impossibleCoursesBecauseOfImpossibleModules,
                      final List<Course> impossibleCoursesBecauseOfImpossibleModuleCombinations) {
    impossibleCoursesList.setAll(impossibleCourses);
    impossibleCoursesBecauseOfImpossibleModulesList.setAll(
      impossibleCoursesBecauseOfImpossibleModules);
    impossibleCoursesBecauseOfImpossibleModuleCombinationsList.setAll(
      impossibleCoursesBecauseOfImpossibleModuleCombinations);
  }
}
