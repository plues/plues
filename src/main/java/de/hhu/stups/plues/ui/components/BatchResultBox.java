package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.hhu.stups.plues.tasks.PdfRenderingTaskFactory;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.binding.StringExpression;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.Set;

import javax.annotation.Nullable;

public class BatchResultBox extends GridPane implements Initializable {

  private static final String ICON_SIZE = "15";
  private static final String WARNING_COLOR = "#FEEFB3";
  private static final String FAILURE_COLOR = "#FFBABA";
  private static final String SUCCESS_COLOR = "#DFF2BF";
  private static final String WORKING_COLOR = "#BDE5F8";

  private final Course majorCourse;
  private final Course minorCourse;
  private final PdfRenderingTask task;
  private Set<PdfRenderingTask> taskPool;
  private final Path tempDirectoryPath;

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
  private Label lbMajor;
  @FXML
  @SuppressWarnings("unused")
  private Label lbMinor;

  /**
   * A light version of the {@link ResultBox} used to generate the pdf files for all possible
   * combinations of major and minor courses. The files are temporarily stored in a separate folder
   * in the system's default temporary directory. This result box should just give information on
   * whether the generation succeeded or not. The {@link PdfRenderingTask} is added to the parent's
   * task pool which is executed later on within
   * {@link de.hhu.stups.plues.ui.controller.BatchTimetableGeneration}
   *
   * @param loader            TaskLoader to load fxml and to set controller
   * @param taskFactory       PDF Rendering task Factory
   * @param major             Major course
   * @param minor             Minor course if present, else null
   * @param tempDirectoryPath The path to the created temporary directory to store the generated pdf
   *                          files in.
   * @param taskPool          The pool of tasks that are executed successively when all tasks are
   *                          available.
   */
  @Inject
  public BatchResultBox(final FXMLLoader loader,
                        final PdfRenderingTaskFactory taskFactory,
                        @Assisted("major") final Course major,
                        @Nullable @Assisted("minor") final Course minor,
                        @Assisted final Path tempDirectoryPath,
                        @Assisted final Set<PdfRenderingTask> taskPool) {
    super();
    this.task = taskFactory.create(major, minor);
    this.majorCourse = major;
    this.minorCourse = minor;
    this.tempDirectoryPath = tempDirectoryPath;
    this.taskPool = taskPool;
    this.setHgap(10.0);

    loader.setLocation(this.getClass()
        .getResource("/fxml/components/BatchResultBox.fxml"));

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
    StringExpression pdfName;

    if (minorCourse != null) {
      pdfName = Bindings.concat(Bindings.selectString(this.majorCourse, "name"),
          "_", Bindings.selectString(this.minorCourse, "name"));
      this.lbMinor.textProperty().bind(Bindings.selectString(this.minorCourse, "fullName"));
    } else {
      pdfName = Bindings.selectString(this.majorCourse, "name");
    }
    this.lbMajor.textProperty().bind(Bindings.selectString(this.majorCourse, "fullName"));

    task.setOnSucceeded(event -> Platform.runLater(() -> {
      if (event.getSource().getValue() != null) {
        try {
          Files.copy((Path) event.getSource().getValue(),
              Paths.get(tempDirectoryPath.toString() + "/" + pdfName.getValue() + ".pdf"));
        } catch (final Exception exception) {
          exception.printStackTrace();
        }
      }
    }));

    this.progressIndicator.setStyle(" -fx-progress-color: " + WORKING_COLOR);
    this.progressIndicator.visibleProperty()
        .bind(task.runningProperty());
    this.icon.graphicProperty().bind(this.getIconBinding());
    this.icon.styleProperty().bind(this.getStyleBinding());

    this.taskPool.add(task);
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
