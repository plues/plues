package de.hhu.stups.plues.ui.controller.unsatcore;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.ui.components.CombinationOrSingleCourseSelection;
import de.hhu.stups.plues.ui.controller.Activatable;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class CourseUnsatCore extends VBox implements Initializable, Activatable {
  private final ObjectProperty<Store> store;

  private final ListProperty<Course> courses;
  private final UiDataService uiDataService;
  private ResourceBundle resources;

  @FXML
  @SuppressWarnings("unused")
  private CombinationOrSingleCourseSelection courseSelection;
  @FXML
  @SuppressWarnings("unused")
  private UnsatCoreButtonBar unsatCoreButtonBar;
  @FXML
  @SuppressWarnings("unused")
  private ModuleUnsatCore moduleUnsatCore;

  /**
   * Constructor.
   *
   * @param inflater             Inflater to load FXML
   * @param delayedStore         Delayed Store
   * @param uiDataService        UiDataService
   */
  @Inject
  public CourseUnsatCore(final Inflater inflater,
                         final Delayed<Store> delayedStore,
                         final UiDataService uiDataService) {
    this.uiDataService = uiDataService;
    this.store = new SimpleObjectProperty<>();
    delayedStore.whenAvailable(this.store::set);

    this.courses = new SimpleListProperty<>(FXCollections.emptyObservableList());

    inflater.inflate("components/unsatcore/CourseUnsatCore", this, this, "unsatCore", "Column");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    this.resources = resources;

    unsatCoreButtonBar.configureButton(resources.getString("button.unsatCoreModules"),
        courses, moduleUnsatCore);
    store.addListener((observable, oldValue, store)
        -> courseSelection.setCourses(store.getCourses()));

    courseSelection.disableProperty().bind(store.isNull());
    courseSelection.impossibleCoursesProperty().bind(uiDataService.impossibleCoursesProperty());
    courses.bind(courseSelection.selectedCoursesProperty());
    courses.addListener((observable, oldValue, newValue) -> {
      moduleUnsatCore.setModules(FXCollections.emptyObservableList());
      moduleUnsatCore.resetTaskState();
    });
  }

  /**
   * Select the given courses within the {@link #courseSelection} when the user navigates to the
   * view via the {@link de.hhu.stups.plues.routes.ControllerRoute}.
   */
  @Override
  public void activateController(final Object... courses) {
    courseSelection.selectCourses((Course[]) courses);
    unsatCoreButtonBar.configureButton(resources.getString("button.unsatCoreModules"),
      this.courses, moduleUnsatCore);
  }
}
