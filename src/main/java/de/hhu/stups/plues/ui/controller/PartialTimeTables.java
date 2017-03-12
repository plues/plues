package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.hhu.stups.plues.tasks.PdfRenderingTaskFactory;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.components.CheckBoxGroup;
import de.hhu.stups.plues.ui.components.CheckBoxGroupFactory;
import de.hhu.stups.plues.ui.components.MajorMinorCourseSelection;
import de.hhu.stups.plues.ui.components.TaskProgressIndicator;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
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
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;


public class PartialTimeTables extends GridPane implements Initializable, Activatable {

  private final Delayed<Store> delayedStore;
  private final Delayed<SolverService> delayedSolverService;

  private final BooleanProperty solverProperty;
  private final BooleanProperty checkRunning;
  private final BooleanProperty selectionChanged;

  private final CheckBoxGroupFactory checkBoxGroupFactory;
  private final ObjectProperty<Store> storeProperty;

  private final PdfRenderingTaskFactory renderingTaskFactory;
  private final ExecutorService executor;
  private final UiDataService uiDataService;
  private final ObjectProperty<Path> pdf;
  private final ObjectProperty<PdfRenderingTask> currentTaskProperty;

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
  private Button btGenerate;
  @FXML
  @SuppressWarnings("unused")
  private Button btShow;
  @FXML
  @SuppressWarnings("unused")
  private Button btSave;
  @FXML
  @SuppressWarnings("unused")
  private Button btCancel;
  @FXML
  @SuppressWarnings("unused")
  private HBox buttonBox;
  @FXML
  @SuppressWarnings("unused")
  private TaskProgressIndicator taskProgressIndicator;

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
                           final UiDataService uiDataService,
                           final PdfRenderingTaskFactory renderingTaskFactory,
                           final ExecutorService executor,
                           final CheckBoxGroupFactory checkBoxGroupFactory) {
    this.delayedStore = delayedStore;
    this.delayedSolverService = delayedSolverService;
    this.uiDataService = uiDataService;
    this.checkBoxGroupFactory = checkBoxGroupFactory;
    this.renderingTaskFactory = renderingTaskFactory;
    this.executor = executor;

    this.storeProperty = new SimpleObjectProperty<>();
    this.solverProperty = new SimpleBooleanProperty(false);
    this.checkRunning = new SimpleBooleanProperty(false);
    this.selectionChanged = new SimpleBooleanProperty(false);
    this.pdf = new SimpleObjectProperty<>();
    currentTaskProperty = new SimpleObjectProperty<>();

    inflater.inflate("PartialTimeTables", this, this, "musterstudienplaene");
  }

  @Override
  public final void initialize(final URL location, final ResourceBundle resources) {
    final BooleanBinding selectionBinding = storeProperty.isNull().or(checkRunning);

    btChoose.disableProperty().bind(selectionBinding);
    courseSelection.disableProperty().bind(selectionBinding);

    courseSelection.addListener(observable -> {
      scrollPane.setVisible(false);
      btGenerate.setVisible(false);
      taskProgressIndicator.taskProperty().set(null);
    });

    btGenerate.disableProperty().bind(solverProperty.not().or(checkRunning));
    btCancel.disableProperty().bind(solverProperty.not().or(checkRunning.not()));
    btCancel.visibleProperty().bind(btGenerate.visibleProperty());
    btShow.visibleProperty().bind(btGenerate.visibleProperty());
    btSave.visibleProperty().bind(btGenerate.visibleProperty());
    btShow.disableProperty().bind(pdf.isNull().or(checkRunning).or(selectionChanged));
    btSave.disableProperty().bind(pdf.isNull().or(checkRunning).or(selectionChanged));

    delayedStore.whenAvailable(store -> {
      courseSelection.setMajorCourseList(FXCollections.observableList(store.getMajors()));
      courseSelection.setMinorCourseList(FXCollections.observableList(store.getMinors()));

      this.storeProperty.set(store);
    });

    courseSelection.impossibleCoursesProperty().bind(uiDataService.impossibleCoursesProperty());

    delayedSolverService.whenAvailable(s -> this.solverProperty.set(true));

    taskProgressIndicator.prefWidthProperty().set(25.0);
    taskProgressIndicator.prefHeightProperty().set(25.0);
  }

  /**
   * Function to generate checkboxes for modules and units.
   */
  @FXML
  @SuppressWarnings( {"unused", "WeakerAccess"})
  public void btChoosePressed() {
    selectionChanged.set(false);
    checkRunning.set(false);
    modulesUnits.getChildren().clear();
    scrollPane.setVisible(true);
    btGenerate.setVisible(true);
    pdf.set(null);

    final Course major = courseSelection.getSelectedMajor();
    final Text majorText = new Text();
    majorText.setText(major.getFullName());
    majorText.setUnderline(true);
    modulesUnits.getChildren().add(majorText);

    for (final Module m : major.getModules()) {
      modulesUnits.getChildren().add(createCheckBoxGroup(m, major));
    }

    final Course minor = courseSelection.getSelectedMinor();
    if (minor != null) {
      final Text minorText = new Text();
      minorText.setText(minor.getFullName());
      minorText.setUnderline(true);
      modulesUnits.getChildren().add(minorText);

      for (final Module m : minor.getModules()) {
        modulesUnits.getChildren().add(createCheckBoxGroup(m, minor));
      }
    }
  }

  @FXML
  @SuppressWarnings("unused")
  public void btCancelPressed() {
    currentTaskProperty.get().cancel(true);
    currentTaskProperty.unbind();
  }

  private Node createCheckBoxGroup(final Module module, final Course course) {
    return checkBoxGroupFactory.create(course, module);
  }

  /**
   * Function to pass selection to solver and check if feasible.
   */
  @FXML
  @SuppressWarnings("unused")
  public void btGeneratePressed() throws InterruptedException {
    checkRunning.set(true);

    final ObservableList<Course> courses = courseSelection.getSelectedCourses();
    final Course major = courseSelection.getSelectedMajor();
    final Course minor = courseSelection.getSelectedMinor();

    final Map<Course, List<Module>> moduleChoice = new HashMap<>();
    final List<AbstractUnit> unitChoice = new ArrayList<>();

    for (final Course course : courses) {
      moduleChoice.put(course, new ArrayList<>());
    }

    for (final Object o : modulesUnits.getChildren()) {
      if (!(o instanceof CheckBoxGroup)) {
        continue;
      }
      final CheckBoxGroup cbg = (CheckBoxGroup) o;

      cbg.setOnSelectionChanged(selectionChanged);

      final Module module = cbg.getModule();
      if (module != null) {
        moduleChoice.get(cbg.getCourse()).add(module);
      }
      unitChoice.addAll(cbg.getSelectedAbstractUnits());
    }

    delayedSolverService.whenAvailable(solverService -> {
      final SolverTask<FeasibilityResult> solverTask =
          solverService.computePartialFeasibility(courses, moduleChoice, unitChoice);
      final PdfRenderingTask task = getPdfRenderingTask(major, minor, solverTask);
      currentTaskProperty.set(task);
      taskProgressIndicator.taskProperty().set(task);

      executor.submit(task);
    });
  }

  private PdfRenderingTask getPdfRenderingTask(final Course major, final Course minor,
                                               final SolverTask<FeasibilityResult> solverTask) {
    final PdfRenderingTask task = renderingTaskFactory.create(major, minor, solverTask);
    task.setOnSucceeded(event -> {
      pdf.set((Path) event.getSource().getValue());
      checkRunning.set(false);
      selectionChanged.set(false);
    });

    task.setOnFailed(event -> {
      pdf.set(null);
      checkRunning.set(false);
      selectionChanged.set(false);
    });

    task.setOnCancelled(event -> {
      checkRunning.set(false);
      selectionChanged.set(false);
    });
    return task;
  }

  @SuppressWarnings("unused")
  @FXML
  private void showPdf() {
    PdfRenderingHelper.showPdf(pdf.get());
  }

  @SuppressWarnings("unused")
  @FXML
  private void savePdf() {
    final Course major = courseSelection.getSelectedMajor();
    final Course minor = courseSelection.getSelectedMinor();

    PdfRenderingHelper.savePdf(pdf.get(), major, minor, null);
  }

  /**
   * Select the given courses within the {@link #courseSelection} when the user navigates to the
   * view via the {@link de.hhu.stups.plues.routes.ControllerRoute}.
   */
  @Override
  public void activateController(final RouteNames routeName, final Object... courses) {
    if (courses.length > 0) {
      courseSelection.selectCourse((Course) courses[0]);
    }
    if (courses.length > 1) {
      courseSelection.selectCourse((Course) courses[1]);
    }
    if (courses.length == 2) {
      btChoosePressed();
    }
  }
}
