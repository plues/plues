package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.Unit;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public class Reports extends VBox implements Initializable {

  private final BooleanProperty solverProperty = new SimpleBooleanProperty(false);
  private final List<String> abstractUnitsWithoutUnits;
  private final List<Unit> units;
  private final List<AbstractUnit> abstractUnits;
  private final List<Course> courses;
  private final Set<String> impossibleCourses;
  private final List<Module> mandatoryModules;

  @FXML
  @SuppressWarnings("unused")
  private Pane paneHeader;
  @FXML
  @SuppressWarnings("unused")
  private TitledPane paneImpossibleCourses;
  @FXML
  @SuppressWarnings("unused")
  private TitledPane paneMandatoryModules;
  @FXML
  @SuppressWarnings("unused")
  private TitledPane paneAbstractUnits;
  @FXML
  @SuppressWarnings("unused")
  private Label lbCourseAmount;
  @FXML
  @SuppressWarnings("unused")
  private Label lbUnitAmount;
  @FXML
  @SuppressWarnings("unused")
  private Label lbImpossibleCoursesAmount;
  @FXML
  @SuppressWarnings("unused")
  private Label lbAbstractUnitAmount;
  @FXML
  @SuppressWarnings("unused")
  private ListView<String> listViewImpossibleCourses;
  @FXML
  @SuppressWarnings("unused")
  private ListView<String> listViewAbstractUnits;
  @FXML
  @SuppressWarnings("unused")
  private ListView<String> listViewCourses;
  @FXML
  @SuppressWarnings("unused")
  private ListView<String> listViewMandatoryModules;

  /**
   * Reports view to present several reports and information about the loaded data, statistics,
   * etc.
   */
  @Inject
  public Reports(final Inflater inflater, final Delayed<Store> delayedStore,
                 final Delayed<SolverService> delayedSolverService,
                 final ExecutorService executor) {
    impossibleCourses = new HashSet<>();
    courses = new ArrayList<>();
    units = new ArrayList<>();
    abstractUnits = new ArrayList<>();
    abstractUnitsWithoutUnits = new ArrayList<>();
    mandatoryModules = new ArrayList<>();

    delayedStore.whenAvailable(store -> {
      store.getCourses().forEach(courses::add);
      store.getUnits().forEach(units::add);
      store.getModules().forEach(mandatoryModules::add);
      // get the titles of the units
      final List<String> unitTitles = units.stream()
          .map(Unit::getTitle).collect(Collectors.toList());
      store.getAbstractUnits().forEach(abstractUnits::add);
      // remove the units from the abstract ones
      abstractUnits.stream()
          .filter(abstractUnit -> !unitTitles.contains(abstractUnit.getTitle()))
          .map(AbstractUnit::getTitle).collect(Collectors.toList())
          .forEach(abstractUnitsWithoutUnits::add);
    });

    delayedSolverService.whenAvailable(solverService -> {
      final SolverTask<Set<String>> impossibleCoursesTask = solverService.impossibleCoursesTask();
      impossibleCoursesTask.setOnSucceeded(event -> {
        impossibleCoursesTask.getValue().forEach(impossibleCourses::add);
        impossibleCourses.forEach(listViewImpossibleCourses.getItems()::add);
        solverProperty.set(true);
        lbImpossibleCoursesAmount.setText(String.valueOf(impossibleCourses.size()));
      });
      executor.submit(impossibleCoursesTask);
    });

    inflater.inflate("Reports", this, this, "reports");
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    paneHeader.visibleProperty().bind(solverProperty);
    paneImpossibleCourses.visibleProperty().bind(solverProperty);
    paneAbstractUnits.visibleProperty().bind(solverProperty);
    paneAbstractUnits.setExpanded(false);
    paneMandatoryModules.visibleProperty().bind(solverProperty);
    paneMandatoryModules.setExpanded(false);

    final String listStyle = "batchListView";
    listViewImpossibleCourses.setId(listStyle);
    listViewAbstractUnits.setId(listStyle);
    listViewCourses.setId(listStyle);
    listViewMandatoryModules.setId(listStyle);

    listViewCourses.getSelectionModel().selectedItemProperty()
        .addListener((observable, oldValue, newValue) -> {
          listViewMandatoryModules.getItems().clear();
          showMandatoryModulesOfCourse(listViewCourses.getSelectionModel().getSelectedItem());
        });

    abstractUnitsWithoutUnits.forEach(listViewAbstractUnits.getItems()::add);
    courses.stream().map(Course::getName).forEach(listViewCourses.getItems()::add);

    listViewCourses.getSelectionModel().select(0);

    lbCourseAmount.setText(String.valueOf(courses.size()));
    lbUnitAmount.setText(String.valueOf(units.size()));
    lbAbstractUnitAmount.setText(String.valueOf(abstractUnits.size()));
  }

  /**
   * Update {@link Reports#listViewMandatoryModules} to show the mandatory modules of the currently
   * selected course in {@link Reports#listViewCourses}.
   *
   * @param selectedCourse The currently selected course within {@link Reports#listViewCourses}.
   */
  @SuppressWarnings("unused")
  private void showMandatoryModulesOfCourse(String selectedCourse) {
    mandatoryModules.forEach(module -> {
      if (module.getCourses().stream()
          .map(Course::getName).collect(Collectors.toList())
          .contains(selectedCourse)) {
        listViewMandatoryModules.getItems().add(module.getTitle());
      }
    });
  }
}
