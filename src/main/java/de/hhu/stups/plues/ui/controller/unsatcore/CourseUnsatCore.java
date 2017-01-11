package de.hhu.stups.plues.ui.controller.unsatcore;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.ui.components.CombinationOrSingleCourseSelection;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class CourseUnsatCore extends VBox implements Initializable {

  private final ObjectProperty<Store> storeProperty;
  private final ListProperty<Course> coursesProperty;
  private final UiDataService uiDataService;

  @FXML
  @SuppressWarnings("unused")
  private CombinationOrSingleCourseSelection courseSelection;
  @FXML
  @SuppressWarnings("unused")
  private UnsatCoreButtonBar unsatCoreButtonBar;

  /**
   * Initialize the component.
   */
  @Inject
  public CourseUnsatCore(final Inflater inflater,
                         final Delayed<Store> delayedStore,
                         final UiDataService uiDataService) {
    this.uiDataService = uiDataService;
    this.storeProperty = new SimpleObjectProperty<>();
    delayedStore.whenAvailable(this.storeProperty::set);

    coursesProperty = new SimpleListProperty<>(FXCollections.emptyObservableList());

    inflater.inflate("components/unsatcore/CourseUnsatCore", this, this, "unsatCore", "Column");
  }

  UnsatCoreButtonBar getUnsatCoreButtonBar() {
    return unsatCoreButtonBar;
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    storeProperty.addListener((observable, oldValue, store)
        -> courseSelection.setCourses(store.getCourses()));

    courseSelection.disableProperty().bind(storeProperty.isNull());
    courseSelection.impossibleCoursesProperty().bind(uiDataService.impossibleCoursesProperty());
    coursesProperty.bind(courseSelection.selectedCoursesProperty());

    unsatCoreButtonBar.setText(resources.getString("button.unsatCoreModules"));
  }

  ListProperty<Course> getCoursesProperty() {
    return coursesProperty;
  }

  void selectCourses(final Course... courses) {
    courseSelection.selectCourses(courses);
  }
}
