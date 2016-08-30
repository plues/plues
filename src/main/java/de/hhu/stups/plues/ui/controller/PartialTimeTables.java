package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.components.CheckBoxGroup;
import de.hhu.stups.plues.ui.components.CheckBoxGroupFactory;
import de.hhu.stups.plues.ui.components.MajorMinorCourseSelection;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class PartialTimeTables extends GridPane implements Initializable {

  private final Delayed<Store> delayedStore;
  private final Delayed<SolverService> delayedSolverService;

  private final BooleanProperty solverProperty;
  private final BooleanProperty generationStarted;
  private final BooleanProperty checkStarted;
  private final CheckBoxGroupFactory checkBoxGroupFactory;
  private final SimpleObjectProperty storeProperty;

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

    this.storeProperty = new SimpleObjectProperty();
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
   * Function to generate checkboxes for modules and units.
   */
  @FXML
  @SuppressWarnings("unused")
  public void btGeneratePressed() {
    generationStarted.set(true);
    checkStarted.set(false);
    modulesUnits.getChildren().clear();

    Map<Course, Map<Module, List<AbstractUnit>>> data = new HashMap<>();

    Course major = courseSelection.getSelectedMajorCourse();
    data.put(major, new HashMap<>());

    Course minor = null;
    if (courseSelection.getSelectedMinorCourse().isPresent()) {
      minor = courseSelection.getSelectedMinorCourse().get();
      data.put(minor, new HashMap<>());
    }

    Store store = (Store) storeProperty.get();

    List<Module> modules = store.getModules();

    for (Module m : modules) {
      if (m.getCourses().contains(major)) {
        if (data.get(major).containsKey(m)) {
          data.get(major).get(m).addAll(m.getAbstractUnits());
        } else {
          data.get(major).put(m, new ArrayList<>(m.getAbstractUnits()));
        }
      }
      if (m.getCourses().contains(minor)) {
        if (data.get(minor).containsKey(m)) {
          data.get(minor).get(m).addAll(m.getAbstractUnits());
        } else {
          data.get(minor).put(m, new ArrayList<>(m.getAbstractUnits()));
        }
      }
    }

    for (Map.Entry<Course, Map<Module, List<AbstractUnit>>> entry : data.entrySet()) {
      Course course = entry.getKey();
      for (Map.Entry<Module, List<AbstractUnit>> map : entry.getValue().entrySet()) {
        CheckBoxGroup cbg = checkBoxGroupFactory.create(course, map.getKey(), map.getValue());
        modulesUnits.getChildren().add(cbg);
      }
    }
  }

  /**
   * Function to pass selection to solver and check if feasible.
   */
  @FXML
  @SuppressWarnings("unused")
  public void btCheckPressed() {
    checkStarted.set(true);
    Course major = courseSelection.getSelectedMajorCourse();

    Map<Course, List<Module>> moduleChoice = new HashMap<>();
    List<AbstractUnit> unitChoice = new ArrayList<>();
    moduleChoice.put(major, new ArrayList<>());

    List<Course> courses = new ArrayList<>();
    courses.add(major);
    Course minor;
    if (courseSelection.getSelectedMinorCourse().isPresent()) {
      minor = courseSelection.getSelectedMinorCourse().get();
      moduleChoice.put(minor, new ArrayList<>());
      courses.add(minor);
    }

    for (Object o : modulesUnits.getChildren()) {
      CheckBoxGroup cbg = (CheckBoxGroup) o;
      Module module = cbg.getModule();
      if (module != null) {
        moduleChoice.get(cbg.getCourse()).add(module);
      }

      unitChoice.addAll(cbg.getBoxToUnit().entrySet().stream().filter(boxToUnit ->
          boxToUnit.getKey().isSelected()).map(Map.Entry::getValue).collect(Collectors.toList()));
    }

    delayedSolverService.whenAvailable(solverService -> {
      SolverTask<FeasibilityResult> solverResult =
          solverService.computePartialFeasibility(courses, moduleChoice, unitChoice);

      solverResult.setOnSucceeded(event -> {
        String text;
        try {
          if (solverResult.get().getModuleChoice() != null) {
            text = "Feasible";
          } else {
            text = "Not feasible";
          }
        } catch (InterruptedException exc) {
          text = "InterruptedException";
          exc.printStackTrace();
        } catch (ExecutionException exc) {
          text = "ExecutionException";
          exc.printStackTrace();
        }

        result.setText(text); // TODO: i18n
      });

      solverResult.setOnFailed(event -> {
        result.setText("Task failed");
      });

      solverService.submit(solverResult);
    });
  }

  @Override
  public final void initialize(final URL location, final ResourceBundle resources) {
    btGenerate.setDefaultButton(true);
    btGenerate.disableProperty().bind(storeProperty.isNull());
    //
    modulesUnits.visibleProperty().bind(generationStarted);
    scrollPane.visibleProperty().bind(generationStarted);
    btCheck.visibleProperty().bind(generationStarted);
    btCheck.disableProperty().bind(solverProperty.not());
    result.visibleProperty().bind(checkStarted);
    result.editableProperty().set(false);
    //
    delayedStore.whenAvailable(s -> {
      initializeCourseSelection(s);
      this.storeProperty.set(s);
    });

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
  }
}
