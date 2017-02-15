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
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nullable;

public class ResultBox extends VBox implements Initializable {

  private static final String WORKING_COLOR = "#BDE5F8";
  private static final String ICON_SIZE = "50";
  private ResourceBundle resources;

  private String removeString;
  private String cancelString;
  private String openInTimetable;
  private String generatePartial;
  private String restartComputation;
  private String show;
  private String saveAs;
  private PdfRenderingTask task;
  private ResultState resultState;

  private final Router router;
  private final ExecutorService executorService;
  private final ListView<ResultBox> parent;
  private final Delayed<SolverService> delayedSolverService;
  private final PdfRenderingTaskFactory renderingTaskFactory;

  private final ObjectProperty<Course> majorCourseProperty = new SimpleObjectProperty<>();
  private final ObjectProperty<ObservableList<String>> cbActionItemsProperty
      = new SimpleObjectProperty<>();
  private final StringProperty errorMsgProperty = new SimpleStringProperty();
  private final ObjectProperty<Course> minorCourseProperty = new SimpleObjectProperty<>();
  private final ObjectProperty<Path> pdf = new SimpleObjectProperty<>();

  // lists of actions for each possible state
  private final ObservableList<String> succeededActions = FXCollections.observableArrayList();
  private final ObservableList<String> failedActions = FXCollections.observableArrayList();
  private final ObservableList<String> cancelledActions = FXCollections.observableArrayList();
  private final ObservableList<String> scheduledActions = FXCollections.observableArrayList();

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
    this.parent = parent;
    this.delayedSolverService = delayedSolverService;
    this.executorService = executorService;
    this.renderingTaskFactory = renderingTaskFactory;

    majorCourseProperty.set(major);
    minorCourseProperty.set(minor);

    inflater.inflate("components/Resultbox", this, this, "resultbox");
  }

  @Override
  public final void initialize(final URL location, final ResourceBundle resources) {
    this.resources = resources;
    removeString = resources.getString("remove");
    openInTimetable = resources.getString("openInTimetable");
    generatePartial = resources.getString("generatePartial");
    restartComputation = resources.getString("restartComputation");
    show = resources.getString("show");
    saveAs = resources.getString("saveAs");
    cancelString = resources.getString("cancel");

    lbMajor.textProperty().bind(Bindings.selectString(majorCourseProperty, "fullName"));
    lbMinor.textProperty().bind(Bindings.selectString(minorCourseProperty, "fullName"));

    // initialize the lists of actions
    succeededActions.addAll(show, saveAs, openInTimetable, generatePartial, removeString);
    failedActions.addAll(openInTimetable, removeString);
    cancelledActions.addAll(openInTimetable, restartComputation, removeString);
    scheduledActions.addAll(cancelString);

    delayedSolverService.whenAvailable(solver -> {
      initSolverTask(solver);
      executorService.submit(task);
    });
    lbErrorMsg.visibleProperty().bind(pdf.isNull());
    lbErrorMsg.textProperty().bind(errorMsgProperty);

    cbAction.itemsProperty().bind(cbActionItemsProperty);
    cbActionItemsProperty.addListener((observable, oldValue, newValue) ->
        cbAction.getSelectionModel().selectFirst());
    cbActionItemsProperty.setValue(scheduledActions);
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
      cbActionItemsProperty.setValue(succeededActions);
      resultState = ResultState.SUCCEEDED;
    }));

    task.setOnFailed(event -> {
      cbActionItemsProperty.setValue(failedActions);
      errorMsgProperty.setValue(resources.getString("error_gen"));
      resultState = ResultState.FAILED;
    });

    task.setOnCancelled(event -> {
      cbActionItemsProperty.setValue(cancelledActions);
      resultState = ResultState.FAILED;
    });

    task.setOnScheduled(event -> cbActionItemsProperty.setValue(scheduledActions));

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
      initSolverTask(delayedSolverService.get());
      executorService.submit(task);
    }
    if (selectedItem.equals(show)) {
      showPdf();
    }
    if (selectedItem.equals(saveAs)) {
      savePdf();
    }
    if (selectedItem.equals(removeString)) {
      parent.getItems().remove(this);
    }
    if (selectedItem.equals(cancelString)) {
      interrupt();
    }
  }

  @FXML
  private void showPdf() {
    PdfRenderingHelper.showPdf(pdf.get(),
        e -> errorMsgProperty.setValue(resources.getString("error_temp")));
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
