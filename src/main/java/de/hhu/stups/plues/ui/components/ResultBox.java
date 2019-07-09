package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.keys.CourseSelection;
import de.hhu.stups.plues.prob.ResultState;
import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.services.PdfRenderingService;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.hhu.stups.plues.ui.controller.PdfRenderingHelper;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ListBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

import javax.annotation.Nullable;

public class ResultBox extends VBox {

  // lists of actions for each possible state
  private static final ObservableList<Actions> succeededActions
      = FXCollections.observableArrayList(Actions.SHOW,
      Actions.SAVE_AS,
      Actions.OPEN_IN_TIMETABLE,
      Actions.GENERATE_PARTIAL,
      Actions.REMOVE);
  private static final ObservableList<Actions> failedActions
      = FXCollections.observableArrayList(Actions.OPEN_IN_TIMETABLE, Actions.REMOVE);
  private static final ObservableList<Actions> cancelledActions
      = FXCollections.observableArrayList(Actions.OPEN_IN_TIMETABLE,
      Actions.RESTART_COMPUTATION,
      Actions.REMOVE);
  private static final ObservableList<Actions> scheduledActions
      = FXCollections.observableArrayList(Actions.CANCEL);
  private final Course major;
  private final Course minor;
  private final Router router;
  private final PdfRenderingService pdfRenderingService;
  private final ListView<ResultBox> parent;
  private final ObjectProperty<PdfGenerationSettings> pdfGenerationSettingsProperty;
  private final StringProperty errorMsgProperty = new SimpleStringProperty();
  private final ObjectProperty<Path> pdf = new SimpleObjectProperty<>();
  private PdfRenderingTask task;
  private ResultState resultState;

  @FXML
  private ResourceBundle resources;

  @FXML
  @SuppressWarnings("unused")
  private TaskProgressIndicator taskProgressIndicator;
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
  private ComboBox<Actions> cbAction;
  @FXML
  @SuppressWarnings("unused")
  private HBox colorPreviewBox;
  @FXML
  @SuppressWarnings("unused")
  private Label lbUnitDisplayFormat;

  /**
   * Constructor for ResultBox.
   *
   * @param inflater              Inflater to handle fxml loader tasks
   * @param major                 Major course
   * @param minor                 Minor course if present, else null
   * @param parent                The parent wrapper (VBox) to remove a single result box.
   * @param pdfGenerationSettings The specific settings for generating the PDF like the color
   *                              scheme and the unit display format.
   */
  @Inject
  @SuppressWarnings("WeakerAccess")
  public ResultBox(final Inflater inflater,
                   final Router router,
                   final PdfRenderingService pdfRenderingService,
                   @Assisted("major") final Course major,
                   @Nullable @Assisted("minor") final Course minor,
                   @Assisted("parent") final ListView<ResultBox> parent,
                   @Assisted final PdfGenerationSettings pdfGenerationSettings) {
    super();
    this.router = router;
    this.pdfRenderingService = pdfRenderingService;
    this.parent = parent;
    this.pdfGenerationSettingsProperty = new SimpleObjectProperty<>(pdfGenerationSettings);

    this.major = major;
    this.minor = minor;
    inflater.inflate("components/Resultbox", this, this, "resultbox");
  }

  private Course[] buildCourses(final Course major, final Course minor) {
    if (minor == null) {
      return new Course[] {major};
    }
    return new Course[] {major, minor};
  }

  @FXML
  public final void initialize() {
    initializeCourseLabels();

    lbErrorMsg.textProperty().bind(
        Bindings.createStringBinding(() -> {
          final String errorMsg = errorMsgProperty.get();
          if (errorMsg == null) {
            return "";
          }
          return resources.getString(errorMsg);
        }, errorMsgProperty));

    cbAction.setConverter(new ActionsStringConverter(resources));
    cbAction.itemsProperty().addListener((observable, oldValue, newValue) ->
        cbAction.getSelectionModel().selectFirst());

    pdfRenderingService.pdfGenerationSettingsProperty().bind(pdfGenerationSettingsProperty);
    runSolverTask();

    showUsedPdfSettings();
  }

  private void showUsedPdfSettings() {
    final PdfGenerationSettings pdfGenerationSettings = pdfGenerationSettingsProperty.get();
    colorPreviewBox.getChildren().clear();
    pdfGenerationSettings.colorSchemeProperty().get()
        .addColorPreviews(colorPreviewBox, 5, 15.0);
    colorPreviewBox.getChildren().remove(colorPreviewBox.getChildren().size() - 1);
    lbUnitDisplayFormat.textProperty().bind(Bindings.createStringBinding(() -> {
      final String titleOrKey = (pdfGenerationSettings.unitDisplayFormatProperty().get().isTitle()
          ? resources.getString("title") : resources.getString("id"));
      return resources.getString("unitDisplayFormat") + " " + titleOrKey;
    }));
  }

  /**
   * Run or restart the solver {@link #task}.
   */
  public void runSolverTask() {
    interrupt();
    //
    initSolverTask();
    showUsedPdfSettings();
    pdfRenderingService.submit(task);
  }

  private void initializeCourseLabels() {
    lbMajor.setText(major.getFullName());
    if (this.minor != null) {
      lbMinor.setText(minor.getFullName());
    } else {
      lbMinor.setText("");
    }
  }

  private void initSolverTask() {
    task = pdfRenderingService.getTask(new CourseSelection(buildCourses(major, minor)));

    task.setOnSucceeded(event -> Platform.runLater(() -> {
      pdf.set((Path) event.getSource().getValue());
      resultState = ResultState.SUCCEEDED;
    }));

    task.setOnFailed(event -> {
      errorMsgProperty.set("error_gen");
      resultState = ResultState.FAILED;
    });

    task.setOnCancelled(event -> resultState = ResultState.FAILED);

    taskBindings();
  }

  private void taskBindings() {
    cbAction.itemsProperty().unbind();
    cbAction.itemsProperty().bind(new ActionsBinding(task.stateProperty()));
    taskProgressIndicator.taskProperty().set(task);
    taskProgressIndicator.sizeProperty().set(75.0);
  }

  @FXML
  @SuppressWarnings("unused")
  private void submitAction() {
    final Actions selectedItem = cbAction.getSelectionModel().getSelectedItem();

    switch (selectedItem) {
      case SHOW:
        showPdf();
        break;
      case SAVE_AS:
        savePdf();
        break;
      case GENERATE_PARTIAL:
        generatePartialAction();
        break;
      case OPEN_IN_TIMETABLE:
        router.transitionTo(RouteNames.TIMETABLE, buildCourses(major, minor), resultState);
        break;
      case RESTART_COMPUTATION:
        runSolverTask();
        break;
      case REMOVE:
        parent.getItems().remove(this);
        break;
      case CANCEL:
        interrupt();
        break;
      default:
        throw new IllegalArgumentException("Unexpected enum value");
    }
  }

  private void generatePartialAction() {
    if (minor == null) {
      router.transitionTo(RouteNames.PARTIAL_TIMETABLES, major);
    } else {
      router.transitionTo(RouteNames.PARTIAL_TIMETABLES, major, minor);
    }
  }

  @FXML
  private void showPdf() {
    PdfRenderingHelper.showPdf(pdf.get(), e -> errorMsgProperty.set("error_temp"));
  }

  @FXML
  private void savePdf() {
    final File destinationFile = PdfRenderingHelper.getTargetFile(major, minor);
    if (destinationFile != null) {
      PdfRenderingHelper.savePdf(pdf.get(), Paths.get(destinationFile.getAbsolutePath()),
          errorMsgProperty);
    }
  }

  @FXML
  private void interrupt() {
    if (task == null) {
      return;
    }
    task.cancel(true);
  }

  public Course getMajorCourse() {
    return major;
  }

  public Course getMinorCourse() {
    return minor;
  }

  private enum Actions {

    CANCEL("cancel"),
    GENERATE_PARTIAL("generatePartial"),
    OPEN_IN_TIMETABLE("openInTimetable"),
    REMOVE("remove"),
    RESTART_COMPUTATION("restartComputation"),
    SAVE_AS("saveAs"),
    SHOW("show");

    private final String key;

    Actions(final String key) {
      this.key = key;
    }

    public String getKey() {
      return key;
    }

  }

  private static class ActionsStringConverter extends StringConverter<Actions> {
    private final ResourceBundle resources;

    ActionsStringConverter(final ResourceBundle resources) {
      this.resources = resources;
    }

    @Override
    public String toString(final Actions value) {
      if (value == null) {
        return "";
      }
      return resources.getString(value.getKey());
    }

    @Override
    public Actions fromString(final String string) {
      throw new IllegalAccessError("not supported");
    }
  }

  private static class ActionsBinding extends ListBinding<Actions> {
    private final ReadOnlyObjectProperty<Worker.State> property;

    ActionsBinding(final ReadOnlyObjectProperty<Worker.State> property) {
      this.property = property;
      bind(property);
    }

    @Override
    protected ObservableList<Actions> computeValue() {
      final Worker.State state = property.get();
      switch (state) {
        case SUCCEEDED:
          return succeededActions;
        case CANCELLED:
          return cancelledActions;
        case FAILED:
          return failedActions;
        case READY:
        case RUNNING:
        case SCHEDULED:
        default:
          return scheduledActions;
      }

    }
  }
}
