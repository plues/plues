package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.ui.layout.Inflater;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.binding.ListBinding;
import javafx.beans.binding.StringBinding;
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

  private final Delayed<Store> delayedStore;
  private Store store;
  private List<Course> impossibleCoursesList;
  private List<Course> impossibleCoursesBecauseOfImpossibleModulesList;
  private List<Course> impossibleCoursesBecauseOfImpossibleModuleCombinationsList;

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
  public ImpossibleCourses(final Inflater inflater,
                           final Delayed<Store> delayedStore) {
    this.delayedStore = delayedStore;

    impossibleCoursesList = new ArrayList<>();
    impossibleCoursesBecauseOfImpossibleModulesList = new ArrayList<>();
    impossibleCoursesBecauseOfImpossibleModuleCombinationsList = new ArrayList<>();

    inflater.inflate("/components/reports/ImpossibleCourses", this, this, "reports");
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    ListBinding<Course> binding = new ListBinding<Course>() {
      {
        bind(buttonImpossibleCourses.selectedProperty());
        bind(buttonImpossibleCoursesBecauseOfImpossibleModules.selectedProperty());
        bind(buttonImpossibleCoursesBecauseOfImpossibleModuleCombinations.selectedProperty());
      }

      @Override
      protected ObservableList<Course> computeValue() {
        if (buttonImpossibleCourses.isSelected()) {
          return FXCollections.observableList(impossibleCoursesList);
        } else {
          if (buttonImpossibleCoursesBecauseOfImpossibleModules.isSelected()) {
            return FXCollections.observableList(impossibleCoursesBecauseOfImpossibleModulesList);
          } else {
            if (buttonImpossibleCoursesBecauseOfImpossibleModuleCombinations.isSelected()) {
              return FXCollections.observableList(
                  impossibleCoursesBecauseOfImpossibleModuleCombinationsList);
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

    delayedStore.whenAvailable(store -> this.store = store);
  }

  public void setData(final Set<String> impossibleCourses,
                      final Set<String> impossibleCoursesBecauseOfImpossibleModules,
                      final Set<String> impossibleCoursesBecauseOfImpossibleModuleCombinations) {
    this.impossibleCoursesList =
        impossibleCourses.stream().map(store::getCourseByKey).collect(Collectors.toList());
    this.impossibleCoursesBecauseOfImpossibleModulesList =
        impossibleCoursesBecauseOfImpossibleModules.stream().map(
            store::getCourseByKey).collect(Collectors.toList());
    this.impossibleCoursesBecauseOfImpossibleModuleCombinationsList =
      impossibleCoursesBecauseOfImpossibleModuleCombinations.stream().map(
        store::getCourseByKey).collect(Collectors.toList());
  }
}
