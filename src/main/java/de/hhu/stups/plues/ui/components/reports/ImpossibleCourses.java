package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.ui.layout.Inflater;

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

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ImpossibleCourses extends VBox implements Initializable {

  private final SimpleListProperty<Course> impossibleCoursesList;
  private final SimpleListProperty<Course> impossibleCoursesBecauseOfImpossibleModulesList;
  private final SimpleListProperty<Course>
      impossibleCoursesBecauseOfImpossibleModuleCombinationsList;

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

  /**
   * Default constructor.
   * @param inflater Handle fxml and resources
   */
  @Inject
  public ImpossibleCourses(final Inflater inflater) {
    impossibleCoursesList = new SimpleListProperty<>(FXCollections.observableArrayList());
    impossibleCoursesBecauseOfImpossibleModulesList =
        new SimpleListProperty<>(FXCollections.observableArrayList());
    impossibleCoursesBecauseOfImpossibleModuleCombinationsList =
        new SimpleListProperty<>(FXCollections.observableArrayList());

    inflater.inflate("/components/reports/ImpossibleCourses", this, this, "reports", "Column");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    final ListBinding<Course> binding = new ListBinding<Course>() {
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

    final StringBinding stringBinding = new StringBinding() {
      {
        bind(buttonImpossibleCourses.selectedProperty());
        bind(buttonImpossibleCoursesBecauseOfImpossibleModules.selectedProperty());
        bind(buttonImpossibleCoursesBecauseOfImpossibleModuleCombinations.selectedProperty());
      }

      @Override
      protected String computeValue() {
        final String string;
        if (buttonImpossibleCourses.isSelected()) {
          string = resources.getString("explain.ImpossibleCourses");
        } else {
          if (buttonImpossibleCoursesBecauseOfImpossibleModules.isSelected()) {
            string = resources.getString("explain.ImpossibleCoursesBecauseOfImpossibleModules");
          } else {
            if (buttonImpossibleCoursesBecauseOfImpossibleModuleCombinations.isSelected()) {
              string = resources.getString(
                "explain.ImpossibleCoursesBecauseOfImpossibleModuleCombinations");
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

  /**
   * Fill list with content.
   * @param impossibleCourses Courses with missing data
   * @param impossibleCoursesBecauseOfImpossibleModules Courses with impossible modules
   * @param impossibleCoursesBecauseOfImpossibleModuleCombinations Courses with impossible module
   *                                                               combinations
   */
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
