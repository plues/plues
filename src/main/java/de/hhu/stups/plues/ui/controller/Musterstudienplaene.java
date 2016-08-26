package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.components.MajorMinorCourseSelection;
import de.hhu.stups.plues.ui.components.ResultBox;
import de.hhu.stups.plues.ui.components.ResultBoxFactory;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;


import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

public class Musterstudienplaene extends GridPane implements Initializable {

  private final Delayed<Store> delayedStore;
  private final Delayed<SolverService> delayedSolverService;

  private final BooleanProperty solverProperty;
  private final BooleanProperty generationStarted;
  private final ResultBoxFactory resultBoxFactory;

  private ObjectProperty<Task<FeasibilityResult>> resultTask;

  @FXML
  @SuppressWarnings("unused")
  private MajorMinorCourseSelection courseSelection;

  @FXML
  @SuppressWarnings("unused")
  private Button btGenerate;

  @FXML
  @SuppressWarnings("unused")
  private ProgressBar progressGenerate;

  @FXML
  @SuppressWarnings("unused")
  private VBox resultBox;

  @FXML
  @SuppressWarnings("unused")
  private ScrollPane scrollPane;

  /**
   * Constructor for musterstudienplaene controller.
   *
   * @param loader               TaskLoader to load fxml file and to set controller
   * @param delayedStore         Store containing relevant data
   * @param delayedSolverService SolverService for usage of ProB solver
   * @param resultBoxFactory     Factory to create ResultBox entities
   */
  @Inject
  public Musterstudienplaene(final FXMLLoader loader, final Delayed<Store> delayedStore,
                             final Delayed<SolverService> delayedSolverService,
                             final ResultBoxFactory resultBoxFactory) {
    this.delayedStore = delayedStore;
    this.delayedSolverService = delayedSolverService;
    this.resultBoxFactory = resultBoxFactory;

    this.solverProperty = new SimpleBooleanProperty(false);
    this.generationStarted = new SimpleBooleanProperty(false);
    this.resultTask = new SimpleObjectProperty<>();

    this.setVgap(10.0);

    loader.setLocation(getClass().getResource("/fxml/musterstudienplaene.fxml"));

    loader.setRoot(this);
    loader.setController(this);

    try {
      loader.load();
    } catch (final IOException exception) {
      throw new RuntimeException(exception);
    }
  }

  /**
   * Function to handle generation of resultbox containing result for choosen major and minor.
   */
  @FXML
  @SuppressWarnings("unused")
  public void btGeneratePressed() {
    generationStarted.set(true);
    final Course selectedMajorCourse
        = courseSelection.getSelectedMajorCourse();
    final Optional<Course> optinalMinorCourse
        = courseSelection.getSelectedMinorCourse();

    Course selectedMinorCourse = null;
    if (optinalMinorCourse.isPresent()) {
      selectedMinorCourse = optinalMinorCourse.get();
    }

    ResultBox rb = resultBoxFactory.create(selectedMajorCourse, selectedMinorCourse);

    resultBox.getChildren().add(0, rb);
  }

  @Override
  public final void initialize(final URL location, final ResourceBundle resources) {
    btGenerate.setDefaultButton(true);
    btGenerate.disableProperty().bind(solverProperty.not());
    //
    scrollPane.visibleProperty().bind(generationStarted);
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
