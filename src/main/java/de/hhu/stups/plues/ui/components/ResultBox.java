package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.hhu.stups.plues.tasks.PdfRenderingTaskFactory;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.controller.PdfRenderingHelper;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nullable;

public class ResultBox extends GridPane implements Initializable {

  private static final String WORKING_COLOR = "#BDE5F8";

  private final ObjectProperty<Course> majorCourse;
  private final ObjectProperty<Course> minorCourse;
  private PdfRenderingTask task;
  private final ExecutorService executor;
  private final Delayed<SolverService> solverService;
  private final PdfRenderingTaskFactory renderingTaskFactory;
  private final VBox parent;
  private ObjectProperty<Path> pdf;

  @FXML
  @SuppressWarnings("unused")
  private StackPane statePane;

  @FXML
  @SuppressWarnings("unused")
  private ProgressIndicator progressIndicator;

  @FXML
  @SuppressWarnings("unused")
  private Label icon;

  @FXML
  @SuppressWarnings("unused")
  private Label major;

  @FXML
  @SuppressWarnings("unused")
  private Label minor;

  @FXML
  @SuppressWarnings("unused")
  private Label lbErrorMsg;

  @FXML
  @SuppressWarnings("unused")
  private ComboBox<String> cbAction;

  @FXML
  @SuppressWarnings("unused")
  private Button btSubmit;

  /**
   * Constructor for ResultBox.
   *
   * @param inflater    Inflater to handle fxml loader tasks
   * @param renderingTaskFactory PDF Rendering task Factory
   * @param major       Major course
   * @param minor       Minor course if present, else null
   * @param parent      The parent wrapper (VBox) to remove a single result box.
   */
  @Inject
  public ResultBox(final Inflater inflater,
                   final Delayed<SolverService> delayedSolverService,
                   final PdfRenderingTaskFactory renderingTaskFactory,
                   final ExecutorService executorService,
                   @Assisted("major") final Course major,
                   @Nullable @Assisted("minor") final Course minor,
                   @Assisted("parent") final VBox parent) {
    super();

    this.solverService = delayedSolverService;
    this.renderingTaskFactory = renderingTaskFactory;

    this.majorCourse = new SimpleObjectProperty<>(major);
    this.minorCourse = new SimpleObjectProperty<>(minor);
    this.pdf = new SimpleObjectProperty<>();
    this.executor = executorService;
    this.parent = parent;
    this.setHgap(10.0);

    inflater.inflate("components/resultbox", null, null, "resultbox");
  }

  /**
   * Helper function to find the file name containing major and minor name.
   *
   * @param major Course object representing the chosen major course
   * @param minor Course object representing the chosen minor course
   * @return String representing the file name
   */
  private static String getDocumentName(final Course major, final Course minor) {
    return "musterstudienplan_" + major.getName() + "_" + minor.getName()
      + ".pdf";
  }

  /**
   * Helper function to find file name containing major name and no minor existing.
   *
   * @param course Course object representing the choosen major course
   * @return String representing the file name
   */
  private static String getDocumentName(final Course course) {
    return "musterstudienplan_" + course.getName() + ".pdf";
  }

  @Override
  public final void initialize(final URL location,
                               final ResourceBundle resources) {
    this.major.textProperty()
      .bind(Bindings.selectString(this.majorCourse, "fullName"));
    this.minor.textProperty()
      .bind(Bindings.selectString(this.minorCourse, "fullName"));
    //
    solverService.whenAvailable(solverService -> {
      final SolverTask<FeasibilityResult> solverTask;
      if (minorCourse.isNotNull().get()) {
        solverTask = solverService.computeFeasibilityTask(majorCourse.get(), minorCourse.get());
        task = renderingTaskFactory.create(majorCourse.get(), minorCourse.get(), solverTask);
      } else {
        solverTask = solverService.computeFeasibilityTask(majorCourse.get());
        task = renderingTaskFactory.create(majorCourse.get(), null, solverTask);
      }

      //
      this.progressIndicator.setStyle(" -fx-progress-color: " + WORKING_COLOR);
      this.progressIndicator.visibleProperty()
        .bind(task.runningProperty());
      //
      icon.graphicProperty().bind(PdfRenderingHelper.getIconBinding(task));
      icon.styleProperty().bind(PdfRenderingHelper.getStyleBinding(task));
      //
      executor.submit(task);
    });
    this.lbErrorMsg.visibleProperty().bind(this.pdf.isNull());

    task.setOnSucceeded(event -> Platform.runLater(() -> {
      pdf.set((Path) event.getSource().getValue());
      cbAction.setItems(FXCollections.observableList(Arrays.asList("Show", "Save", "Remove")));
      cbAction.getSelectionModel().selectFirst();
    }));

    // TODO: Add status bar later to let the user know whats going on
    //    final Task<FeasibilityResult> task;
    //    if (selectedMinorCourse.isPresent()) {
    //      task = solverService.computeFeasibilityTask(
    //          selectedMajorCourse, selectedMinorCourse.get());
    //    } else {
    //      task = solverService.computeFeasibilityTask(selectedMajorCourse);
    //    }
    //    resultTask.set(task);


    //  task.setOnFailed(event -> {
    //    final Alert alert = new Alert(Alert.AlertType.ERROR);
    //    alert.setTitle("Generation failed");
    //    alert.setHeaderText("Invalid course combination");
    //    alert.setContentText("The chosen combination of major and minor course is not possible.");
    //    alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
    //    alert.showAndWait();
    //  });


    task.setOnFailed(event -> {
      this.cbAction.setItems(FXCollections.observableList(Collections.singletonList("Remove")));
      this.cbAction.getSelectionModel().selectFirst();
      this.lbErrorMsg.setText("Error! Could not generate PDF");
    });
    //
    this.progressIndicator.setStyle(" -fx-progress-color: " + WORKING_COLOR);
    this.progressIndicator.visibleProperty()
      .bind(task.runningProperty());
    //
    // Binding the progress property of the indicator shows a the percentage
    // of completion which in this case is arbitrary since we do not know how
    // long the process will take.
    //
    // progressIndicator.progressProperty().bind(this.task.progressProperty());
    //
    this.cbAction.setItems(FXCollections.observableList(Collections.singletonList("Cancel")));
    this.cbAction.getSelectionModel().selectFirst();
  }

  @FXML
  @SuppressWarnings("unused")
  private void submitAction() {
    final String selectedItem = cbAction.getSelectionModel().getSelectedItem();
    switch (selectedItem) {
      case "Show":
        showPdf();
        break;
      case "Save":
        savePdf();
        break;
      case "Remove":
        this.parent.getChildren().remove(this);
        break;
      case "Cancel":
        this.interrupt();
        this.cbAction.setItems(FXCollections.observableList(Collections.singletonList("Remove")));
        this.cbAction.getSelectionModel().selectFirst();
        break;
      default:
        break;
    }
  }

  @FXML
  private void showPdf() {
    PdfRenderingHelper.showPdf(pdf.get(),
        e -> lbErrorMsg.setText("Error! Copying of temporary file into target file failed."));
  }

  @FXML
  private void savePdf() {
    PdfRenderingHelper.savePdf(pdf.get(), majorCourse.get(), minorCourse.get(), lbErrorMsg);
  }

  @FXML
  private void interrupt() {
    this.task.cancel();
  }
}
