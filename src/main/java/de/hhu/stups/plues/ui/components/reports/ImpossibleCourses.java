package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.ui.layout.Inflater;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

public class ImpossibleCourses extends VBox implements Initializable {

  private final Delayed<Store> delayedStore;
  private Store store;

  @FXML
  @SuppressWarnings("unused")
  private TableView<Course> tableViewImpossibleCourses;

  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Course, String> tableColumnCourseName;

  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Course, String> tableColumnCourseFullName;

  @Inject
  public ImpossibleCourses(final Inflater inflater,
                           final Delayed<Store> delayedStore) {
    this.delayedStore = delayedStore;
    inflater.inflate("/components/reports/ImpossibleCourses", this, this, "reports");
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    tableColumnCourseName.setCellValueFactory(new PropertyValueFactory<>("key"));
    tableColumnCourseFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));

    delayedStore.whenAvailable(store -> this.store = store);
  }

  public void setData(final Set<String> impossibleCourses) {
    tableViewImpossibleCourses.getItems().addAll(
        impossibleCourses.stream().map(store::getCourseByKey).collect(Collectors.toList()));
  }
}
