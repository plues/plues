package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.hhu.stups.plues.tasks.PdfRenderingTaskFactory;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.components.CheckBoxGroup;
import de.hhu.stups.plues.ui.components.CheckBoxGroupFactory;
import de.hhu.stups.plues.ui.components.MajorMinorCourseSelection;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

public class PartialTimeTables extends GridPane implements Initializable {

  private final Delayed<Store> delayedStore;
  private final Delayed<SolverService> delayedSolverService;

  private final BooleanProperty solverProperty;
  private final BooleanProperty generationStarted;
  private final BooleanProperty checkStarted;
  private final CheckBoxGroupFactory checkBoxGroupFactory;
  private final ObjectProperty<Store> storeProperty;
  private final BooleanProperty storeAvailable;

  private final PdfRenderingTaskFactory renderingTaskFactory;
  private final ExecutorService executor;
  private PdfRenderingTask task;
  private final ObjectProperty<Path> pdf;

  @FXML
  @SuppressWarnings("unused")
  private MajorMinorCourseSelection courseSelection;

  @FXML
  @SuppressWarnings("unused")
  private Button btChoose;

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
  private Label icon;

  @FXML
  @SuppressWarnings("unused")
  private HBox buttons;

  /**
   * Constructor for partial time table controller.
   *
   * @param loader               TaskLoader to load fxml file and to set controller
   * @param delayedStore         Store containing relevant data
   * @param delayedSolverService SolverService for usage of ProB solver
   * @param checkBoxGroupFactory Factory to create check box groups
   */
  @Inject
  public PartialTimeTables(final FXMLLoader loader, final Delayed<Store> delayedStore,
                           final Delayed<SolverService> delayedSolverService,
                           final PdfRenderingTaskFactory renderingTaskFactory,
                           final ExecutorService executor,
                           final CheckBoxGroupFactory checkBoxGroupFactory) {
    this.delayedStore = delayedStore;
    this.delayedSolverService = delayedSolverService;
    this.checkBoxGroupFactory = checkBoxGroupFactory;
    this.renderingTaskFactory = renderingTaskFactory;
    this.executor = executor;

    this.storeProperty = new SimpleObjectProperty<>();
    this.storeAvailable = new SimpleBooleanProperty(false);
    this.solverProperty = new SimpleBooleanProperty(false);
    this.generationStarted = new SimpleBooleanProperty(false);
    this.checkStarted = new SimpleBooleanProperty(false);
    this.pdf = new SimpleObjectProperty<>();

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
  public void btChoosePressed() {
    generationStarted.set(true);
    checkStarted.set(false);
    modulesUnits.getChildren().clear();

    final Course major = courseSelection.getSelectedMajorCourse();
    final Text majorText = new Text();
    majorText.setText(major.getFullName());
    majorText.setUnderline(true);
    modulesUnits.getChildren().add(majorText);

    Course minor = null;
    if (courseSelection.getSelectedMinorCourse().isPresent()) {
      minor = courseSelection.getSelectedMinorCourse().get();
      final Text minorText = new Text();
      minorText.setText(minor.getFullName());
      minorText.setUnderline(true);
      modulesUnits.getChildren().add(minorText);
    }

    for (final Module m : major.getModules()) {
      modulesUnits.getChildren().add(1, createCheckBoxGroup(m, major));
    }
    if (minor != null) {
      for (final Module m : minor.getModules()) {
        modulesUnits.getChildren().add(createCheckBoxGroup(m, minor));
      }
    }
  }

  private Node createCheckBoxGroup(final Module module, final Course course) {
    return checkBoxGroupFactory.create(course, module, module.getAbstractUnits());
  }

  /**
   * Function to pass selection to solver and check if feasible.
   */
  @FXML
  @SuppressWarnings("unused")
  public void btCheckPressed() throws InterruptedException {
    checkStarted.set(true);
    final Course major = courseSelection.getSelectedMajorCourse();

    final Map<Course, List<Module>> moduleChoice = new HashMap<>();
    final List<AbstractUnit> unitChoice = new ArrayList<>();
    moduleChoice.put(major, new ArrayList<>());

    final List<Course> courses = new ArrayList<>();
    courses.add(major);

    final Course minor;
    if (courseSelection.getSelectedMinorCourse().isPresent()) {
      minor = courseSelection.getSelectedMinorCourse().get();
      moduleChoice.put(minor, new ArrayList<>());
      courses.add(minor);
    } else {
      minor = null;
    }

    for (final Object o : modulesUnits.getChildren()) {
      final CheckBoxGroup cbg;
      try {
        cbg = (CheckBoxGroup) o;
      } catch (final ClassCastException exc) {
        continue;
      }
      final Module module = cbg.getModule();
      if (module != null) {
        moduleChoice.get(cbg.getCourse()).add(module);
      }

      unitChoice.addAll(cbg.getSelectedAbstractUnits());
    }

    delayedSolverService.whenAvailable(solverService -> {
      final SolverTask<FeasibilityResult> solverTask =
          solverService.computePartialFeasibility(courses, moduleChoice, unitChoice);

      task = renderingTaskFactory.create(major, minor, solverTask);

      task.setOnSucceeded(event -> pdf.set((Path) event.getSource().getValue()));
      task.setOnFailed(event -> pdf.set(null));

      icon.styleProperty().bind(PdfRenderingHelper.getStyleBinding(task));
      icon.graphicProperty().bind(PdfRenderingHelper.getIconBinding(task));

      executor.submit(task);
    });
  }

  @Override
  public final void initialize(final URL location, final ResourceBundle resources) {
    btChoose.setDefaultButton(true);
    btChoose.disableProperty().bind(storeAvailable.not());
    //
    modulesUnits.visibleProperty().bind(generationStarted);
    scrollPane.visibleProperty().bind(generationStarted);
    btCheck.visibleProperty().bind(generationStarted);
    btCheck.disableProperty().bind(solverProperty.not());
    //
    icon.visibleProperty().bind(checkStarted);
    buttons.visibleProperty().bind(checkStarted);
    buttons.disableProperty().bind(pdf.isNull().and(checkStarted));
    //
    delayedStore.whenAvailable(s -> {
      PdfRenderingHelper.initializeCourseSelection(s, courseSelection);
      this.storeProperty.set(s);
      this.storeAvailable.set(true);
    });

    delayedSolverService.whenAvailable(s -> {
      this.solverProperty.set(true);
      PdfRenderingHelper.impossibleCourses(s, courseSelection);
    });
  }

  @FXML
  private void showPdf() {
    PdfRenderingHelper.showPdf(pdf.get(), null);
  }

  @FXML
  private void savePdf() {
    final Course major = courseSelection.getSelectedMajorCourse();
    final Course minor = (courseSelection.getSelectedMinorCourse().isPresent())
        ? courseSelection.getSelectedMinorCourse().get() : null;

    PdfRenderingHelper.savePdf(pdf.get(), major, minor, this.getClass(), null);
  }
}
