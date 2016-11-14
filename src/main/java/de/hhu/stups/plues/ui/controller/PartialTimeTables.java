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
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.components.CheckBoxGroup;
import de.hhu.stups.plues.ui.components.CheckBoxGroupFactory;
import de.hhu.stups.plues.ui.components.MajorMinorCourseSelection;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
   * @param inflater             TaskLoader to load fxml file and to set controller
   * @param delayedStore         Store containing relevant data
   * @param delayedSolverService SolverService for usage of ProB solver
   * @param checkBoxGroupFactory Factory to create check box groups
   */
  @Inject
  public PartialTimeTables(final Inflater inflater,
                           final Delayed<Store> delayedStore,
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
    this.solverProperty = new SimpleBooleanProperty(false);
    this.generationStarted = new SimpleBooleanProperty(false);
    this.checkStarted = new SimpleBooleanProperty(false);
    this.pdf = new SimpleObjectProperty<>();

    this.setVgap(10.0);

    inflater.inflate("PartialTimeTables", this, this, "musterstudienplaene");
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
    scrollPane.setVisible(true);
    btCheck.setVisible(true);

    final Course major = courseSelection.getSelectedMajorCourse();
    final Text majorText = new Text();
    majorText.setText(major.getFullName());
    majorText.setUnderline(true);
    modulesUnits.getChildren().add(majorText);

    for (final Module m : major.getModules()) {
      modulesUnits.getChildren().add(createCheckBoxGroup(m, major));
    }

    if (courseSelection.getSelectedMinorCourse().isPresent()) {
      Course minor = courseSelection.getSelectedMinorCourse().get();
      final Text minorText = new Text();
      minorText.setText(minor.getFullName());
      minorText.setUnderline(true);
      modulesUnits.getChildren().add(minorText);

      for (final Module m : minor.getModules()) {
        modulesUnits.getChildren().add(createCheckBoxGroup(m, minor));
      }
    }
  }

  private Node createCheckBoxGroup(final Module module, final Course course) {
    return checkBoxGroupFactory.create(course, module);
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
    final Optional<Course> selectedMinorCourse = courseSelection.getSelectedMinorCourse();

    if (selectedMinorCourse.isPresent()) {
      minor = selectedMinorCourse.get();
      moduleChoice.put(minor, new ArrayList<>());
      courses.add(minor);
    } else {
      minor = null;
    }

    for (final Object o : modulesUnits.getChildren()) {
      if (!(o instanceof CheckBoxGroup)) {
        continue;
      }
      final CheckBoxGroup cbg = (CheckBoxGroup) o;

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
    btChoose.disableProperty().bind(storeProperty.isNull());

    scrollPane.setVisible(false);
    btCheck.setVisible(false);
    //
    courseSelection.addListener(observable -> {
      scrollPane.setVisible(false);
      btCheck.setVisible(false);
      buttons.setVisible(false);
    });
    btCheck.disableProperty().bind(solverProperty.not());
    buttons.visibleProperty().bindBidirectional(checkStarted);
    buttons.disableProperty().bind(pdf.isNull().and(checkStarted));
    //
    delayedStore.whenAvailable(s -> {
      PdfRenderingHelper.initializeCourseSelection(s, courseSelection, delayedSolverService);
      this.storeProperty.set(s);
    });

    delayedSolverService.whenAvailable(s -> this.solverProperty.set(true));
  }

  @SuppressWarnings("unused")
  @FXML
  private void showPdf() {
    PdfRenderingHelper.showPdf(pdf.get());
  }

  @SuppressWarnings("unused")
  @FXML
  private void savePdf() {
    final Course major = courseSelection.getSelectedMajorCourse();
    final Course minor = courseSelection.getSelectedMinorCourse().orElse(null);

    PdfRenderingHelper.savePdf(pdf.get(), major, minor, null);
  }
}
