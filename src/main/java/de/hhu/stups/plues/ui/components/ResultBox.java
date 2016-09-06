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
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nullable;

public class ResultBox extends GridPane implements Initializable {

  private static final String ICON_SIZE = "50";
  private static final String WARNING_COLOR = "#FEEFB3";
  private static final String FAILURE_COLOR = "#FFBABA";
  private static final String SUCCESS_COLOR = "#DFF2BF";
  private static final String WORKING_COLOR = "#BDE5F8";

  private final ObjectProperty<Course> majorCourse;
  private final ObjectProperty<Course> minorCourse;
  private PdfRenderingTask task;
  private final ExecutorService executor;
  private final Delayed<SolverService> solverService;
  private final PdfRenderingTaskFactory renderingTaskFactory;

  @FXML
  private StackPane statePane;

  @FXML
  private ProgressIndicator progressIndicator;

  @FXML
  private Label icon;

  @FXML
  private Label major;

  @FXML
  private Label minor;

  @FXML
  private PdfButtonBar pdfButtonBar;

  /**
   * Constructor for ResultBox.
   *
   * @param loader      TaskLoader to load fxml and to set controller
   * @param renderingTaskFactory PDF Rendering task Factory
   * @param major       Major course
   * @param minor       Minor course if present, else null
   */
  @Inject
  public ResultBox(final FXMLLoader loader,
                   final Delayed<SolverService> delayedSolverService,
                   final PdfRenderingTaskFactory renderingTaskFactory,
                   final ExecutorService executorService,
                   @Assisted("major") final Course major,
                   @Nullable @Assisted("minor") final Course minor) {
    super();

    this.solverService = delayedSolverService;
    this.renderingTaskFactory = renderingTaskFactory;

    this.majorCourse = new SimpleObjectProperty<>(major);
    this.minorCourse = new SimpleObjectProperty<>(minor);
    this.executor = executorService;

    loader.setLocation(this.getClass()
        .getResource("/fxml/components/resultbox.fxml"));

    loader.setRoot(this);
    loader.setController(this);

    try {
      loader.load();
    } catch (final IOException exception) {
      throw new RuntimeException(exception);
    }
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
      SolverTask<FeasibilityResult> solverTask;
      if (minorCourse.isNotNull().get()) {
        solverTask = solverService.computeFeasibilityTask(majorCourse.get(), minorCourse.get());
        task = renderingTaskFactory.create(majorCourse.get(), minorCourse.get(), solverTask);
      } else {
        solverTask = solverService.computeFeasibilityTask(majorCourse.get());
        task = renderingTaskFactory.create(majorCourse.get(), null, solverTask);
      }

//    TODO: Add status bar later to let the user know whats going on
//        final Task<FeasibilityResult> task;
//        if (selectedMinorCourse.isPresent()) {
//          task = solverService.computeFeasibilityTask(
//              selectedMajorCourse, selectedMinorCourse.get());
//        } else {
//            task = solverService.computeFeasibilityTask(selectedMajorCourse);
//          }
//            resultTask.set(task);

//          task.setOnFailed(event -> {
//            final Alert alert = new Alert(Alert.AlertType.ERROR);
//            alert.setTitle("Generation failed");
//            alert.setHeaderText("Invalid course combination");
//            alert.setContentText("The chosen combination of major and minor course is not possible.");
//            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
//            alert.showAndWait();
//          });
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
      this.icon.graphicProperty().bind(this.getIconBinding());
      this.icon.styleProperty().bind(this.getStyleBinding());
      //
      pdfButtonBar.setMajor(majorCourse.get());
      pdfButtonBar.setMinor(minorCourse.get());
      pdfButtonBar.setTask(task);
      //
      executor.submit(task);
    });
  }

  private StringBinding getStyleBinding() {
    return Bindings.createStringBinding(() -> {
      String color = null;

      switch (task.getState()) {
        case READY:
        case SCHEDULED:
        case RUNNING:
          return "";

        case SUCCEEDED:
          color = SUCCESS_COLOR;
          break;
        case CANCELLED:
          color = WARNING_COLOR;
          break;
        case FAILED:
          color = FAILURE_COLOR;
          break;
        default:
          break;
      }

      return "-fx-background-color: " + color;

    }, task.stateProperty());
  }

  private ObjectBinding<Text> getIconBinding() {
    return Bindings.createObjectBinding(() -> {
      FontAwesomeIcon symbol = null;

      switch (task.getState()) {
        case READY:
        case SCHEDULED:
        case RUNNING:
          return null;

        case SUCCEEDED:
          symbol = FontAwesomeIcon.CHECK;
          break;
        case CANCELLED:
          symbol = FontAwesomeIcon.QUESTION;
          break;
        case FAILED:
          symbol = FontAwesomeIcon.REMOVE;
          break;
        default:
          break;
      }

      final FontAwesomeIconFactory iconFactory = FontAwesomeIconFactory.get();
      return iconFactory.createIcon(symbol, ICON_SIZE);

    }, task.stateProperty());
  }
}
