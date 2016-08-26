package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.components.CheckBoxGroup;
import de.hhu.stups.plues.ui.components.CheckBoxGroupFactory;
import de.hhu.stups.plues.ui.components.MajorMinorCourseSelection;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class PartialTimeTables extends GridPane implements Initializable {

  private final Delayed<Store> delayedStore;
  private final Delayed<SolverService> delayedSolverService;

  private final BooleanProperty solverProperty;
  private final BooleanProperty generationStarted;
  private final BooleanProperty checkStarted;
  private final CheckBoxGroupFactory checkBoxGroupFactory;

  @FXML
  @SuppressWarnings("unused")
  private MajorMinorCourseSelection courseSelection;

  @FXML
  @SuppressWarnings("unused")
  private Button btGenerate;

  @FXML
  @SuppressWarnings("unused")
  private ScrollPane scrollPane;

  @FXML
  @SuppressWarnings("unused")
  private VBox modulesUnits;

  @FXML
  @SuppressWarnings("unused")
  private Button btCheck;

  @FXML
  @SuppressWarnings("unused")
  private TextField result;

  /**
   * Constructor for musterstudienplaene controller.
   *
   * @param loader               TaskLoader to load fxml file and to set controller
   * @param delayedStore         Store containing relevant data
   * @param delayedSolverService SolverService for usage of ProB solver
   * @param checkBoxGroupFactory Factory to create check box groups
   */
  @Inject
  public PartialTimeTables(final FXMLLoader loader, final Delayed<Store> delayedStore,
                           final Delayed<SolverService> delayedSolverService,
                           final CheckBoxGroupFactory checkBoxGroupFactory) {
    this.delayedStore = delayedStore;
    this.delayedSolverService = delayedSolverService;
    this.checkBoxGroupFactory = checkBoxGroupFactory;

    this.solverProperty = new SimpleBooleanProperty(false);
    this.generationStarted = new SimpleBooleanProperty(false);
    this.checkStarted = new SimpleBooleanProperty(false);

    this.setVgap(10.0);

    loader.setLocation(getClass().getResource("/fxml/PartialTimeTables.fxml"));

    loader.setRoot(this);
    loader.setController(this);

    try {
      loader.load();
    } catch (final IOException exception) {
      throw new RuntimeException(exception);
    }
  }

  /**
   * Function to generate checkboxes for modules and units
   */
  @FXML
  @SuppressWarnings("unused")
  public void btGeneratePressed() {
    generationStarted.set(true);
    checkStarted.set(false);

    // testing data for now. will be improved soon
    List<AbstractUnit> units = new ArrayList<>();
    AbstractUnit u1 = new AbstractUnit();
    u1.setTitle("Unit 1");
    AbstractUnit u2 = new AbstractUnit();
    u2.setTitle("Unit 2");
    units.add(u1); units.add(u2);

    for (int i=1;i<=3;i++) {
      Module m = new Module();
      m.setTitle("Module "+i);
      CheckBoxGroup cbg = checkBoxGroupFactory.create(m, units);
      modulesUnits.getChildren().add(cbg);
    }
  }

  /**
   * Function to pass selection to solver and check if feasible
   */
  @FXML
  @SuppressWarnings("unused")
  public void btCheckPressed() {
    checkStarted.set(true);
    result.setText("Not feasible");
  }

  @Override
  public final void initialize(final URL location, final ResourceBundle resources) {
    btGenerate.setDefaultButton(true);
    btGenerate.disableProperty().bind(solverProperty.not());
    //
    modulesUnits.visibleProperty().bind(generationStarted);
    scrollPane.visibleProperty().bind(generationStarted);
    btCheck.visibleProperty().bind(generationStarted);
    result.visibleProperty().bind(checkStarted);
    result.editableProperty().set(false);
    //
    delayedStore.whenAvailable(this::initializeCourseSelection);

    delayedSolverService.whenAvailable(s -> {
      this.solverProperty.set(true);

      final SolverTask<Set<String>> impossibleCoursesTask = s.impossibleCoursesTask();

      impossibleCoursesTask.setOnSucceeded(event ->
          courseSelection.highlightImpossibleCourses((Set<String>) event.getSource().getValue()));
      s.submit(impossibleCoursesTask);
    });
  }

  private void initializeCourseSelection(final Store store) {
    final List<Course> courses = store.getCourses();

    final List<Course> majorCourseList = courses.stream()
        .filter(Course::isMajor)
        .collect(Collectors.toList());

    final List<Course> minorCourseList = courses.stream()
        .filter(Course::isMinor)
        .collect(Collectors.toList());

    courseSelection.setMajorCourseList(FXCollections.observableList(majorCourseList));
    courseSelection.setMinorCourseList(FXCollections.observableList(minorCourseList));
    courseSelection.setInitialMinorCourseList(FXCollections.observableList(minorCourseList));
  }
}
