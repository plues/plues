package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.prob.ResultState;
import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.hhu.stups.plues.tasks.PdfRenderingTaskFactory;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.TaskBindings;
import de.hhu.stups.plues.ui.controller.PdfRenderingHelper;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nullable;

public class ResultBox extends VBox implements Initializable {

  private static final String WORKING_COLOR = "#BDE5F8";
  private static final String ICON_SIZE = "50";
  private ResourceBundle resources;
  private String remove;

  private String openInTimetable;
  private String generatePartial;
  private String restartComputation;
  private String show;
  private String saveAs;
  private String cancel;
  private PdfRenderingTask task;
  private ResultState resultState;

  private final Router router;
  private final ObjectProperty<Course> majorCourseProperty;
  private final ExecutorService executorService;
  private final ObjectProperty<Course> minorCourseProperty;
  private final ExecutorService executor;
  private final Delayed<SolverService> solverService;
  private final PdfRenderingTaskFactory renderingTaskFactory;
  private final ListView<ResultBox> parent;
  private final ObjectProperty<Path> pdf;

  @FXML
  @SuppressWarnings("unused")
  private ProgressIndicator progressIndicator;
  @FXML
  @SuppressWarnings("unused")
  private Label lbIcon;
  @FXML
  @SuppressWarnings("unused")
  private Label lbMajor;
  @FXML
  @SuppressWarnings("unused")
  private Label lbMinor;
  @FXML
  @SuppressWarnings("unused")
  private Label lbErrorMsg;
  @FXML
  @SuppressWarnings("unused")
  private ComboBox<String> cbAction;

  /**
   * Constructor for ResultBox.
   *
   * @param inflater             Inflater to handle fxml loader tasks
   * @param renderingTaskFactory PDF Rendering task Factory
   * @param major                Major course
   * @param minor                Minor course if present, else null
   * @param parent               The parent wrapper (VBox) to remove a single result box.
   */
  @Inject
  @SuppressWarnings("WeakerAccess")
  public ResultBox(final Inflater inflater,
                   final Router router,
                   final Delayed<SolverService> delayedSolverService,
                   final PdfRenderingTaskFactory renderingTaskFactory,
                   final ExecutorService executorService,
                   @Assisted("major") final Course major,
                   @Nullable @Assisted("minor") final Course minor,
                   @Assisted("parent") final ListView<ResultBox> parent) {
    super();
    this.router = router;
    this.solverService = delayedSolverService;
    this.executorService = executorService;
    this.renderingTaskFactory = renderingTaskFactory;
    this.majorCourseProperty = new SimpleObjectProperty<>(major);
    this.minorCourseProperty = new SimpleObjectProperty<>(minor);
    this.pdf = new SimpleObjectProperty<>();
    this.executor = executorService;
    this.parent = parent;

    inflater.inflate("components/resultbox", this, this, "resultbox");
  }

  @Override
  public final void initialize(final URL location, final ResourceBundle resources) {
    this.resources = resources;
    remove = resources.getString("remove");
    openInTimetable = resources.getString("openInTimetable");
    generatePartial = resources.getString("generatePartial");
    restartComputation = resources.getString("restartComputation");
    show = resources.getString("show");
    saveAs = resources.getString("saveAs");
    cancel = resources.getString("cancel");

    lbMajor.textProperty().bind(Bindings.selectString(majorCourseProperty, "fullName"));
    lbMinor.textProperty().bind(Bindings.selectString(minorCourseProperty, "fullName"));

    solverService.whenAvailable(solver -> {
      initSolverTask(solver);
      executor.submit(task);
    });
    lbErrorMsg.visibleProperty().bind(pdf.isNull());

    cbAction.setItems(FXCollections.observableList(Collections.singletonList(cancel)));
    cbAction.getSelectionModel().selectFirst();
  }

  private void initSolverTask(final SolverService solverService) {
    final SolverTask<FeasibilityResult> solverTask;
    final Course cMajor = majorCourseProperty.get();
    final Course cMinor = minorCourseProperty.get();
    if (cMinor != null) {
      solverTask = solverService.computeFeasibilityTask(cMajor, cMinor);
    } else {
      solverTask = solverService.computeFeasibilityTask(cMajor);
    }
    task = renderingTaskFactory.create(cMajor, cMinor, solverTask);
    task.setOnSucceeded(event -> Platform.runLater(() -> {
      pdf.set((Path) event.getSource().getValue());
      cbAction.setItems(FXCollections.observableList(
          Arrays.asList(show, saveAs, openInTimetable, generatePartial, remove)));
      cbAction.getSelectionModel().selectFirst();
      resultState = ResultState.SUCCEEDED;
    }));

    task.setOnFailed(event -> {
      cbAction.setItems(FXCollections.observableList(Arrays.asList(openInTimetable, remove)));
      cbAction.getSelectionModel().selectFirst();
      lbErrorMsg.setText(resources.getString("error_gen"));
      resultState = ResultState.FAILED;
    });

    task.setOnCancelled(event -> {
      cbAction.setItems(FXCollections.observableList(
          Arrays.asList(openInTimetable, restartComputation, remove)));
      cbAction.getSelectionModel().selectFirst();
      resultState = ResultState.FAILED;
    });

    task.setOnScheduled(event -> {
      cbAction.setItems(FXCollections.observableList(Collections.singletonList(cancel)));
      cbAction.getSelectionModel().selectFirst();
    });

    progressIndicator.setStyle(" -fx-progress-color: " + WORKING_COLOR);
    progressIndicator.visibleProperty().bind(task.runningProperty());

    lbIcon.visibleProperty().bind(task.runningProperty().not());
    lbIcon.graphicProperty().bind(TaskBindings.getIconBinding(ICON_SIZE, task));
    lbIcon.styleProperty().bind(TaskBindings.getStyleBinding(task));
  }

  @FXML
  @SuppressWarnings("unused")
  private void submitAction() {
    final String selectedItem = cbAction.getSelectionModel().getSelectedItem();
    final Course majorCourse = majorCourseProperty.get();
    final Course minorCourse = minorCourseProperty.get();

    if (selectedItem.equals(openInTimetable)) {
      router.transitionTo(RouteNames.TIMETABLE, new Course[] {majorCourse, minorCourse},
          resultState);
    }
    if (selectedItem.equals(generatePartial)) {
      router.transitionTo(RouteNames.PARTIAL_TIMETABLES, majorCourse, minorCourse);
    }
    if (selectedItem.equals(restartComputation)) {
      initSolverTask(solverService.get());
      executorService.submit(task);
    }
    if (selectedItem.equals(show)) {
      showPdf();
    }
    if (selectedItem.equals(saveAs)) {
      savePdf();
    }
    if (selectedItem.equals(remove)) {
      parent.getItems().remove(this);
    }
    if (selectedItem.equals(cancel)) {
      interrupt();
    }
  }

  @FXML
  private void showPdf() {
    PdfRenderingHelper.showPdf(pdf.get(),
        e -> lbErrorMsg.setText(resources.getString("error_temp")));
  }

  @FXML
  private void savePdf() {
    PdfRenderingHelper.savePdf(pdf.get(), majorCourseProperty.get(),
        minorCourseProperty.get(), lbErrorMsg);
  }

  @FXML
  private void interrupt() {
    task.cancel();
  }
}
