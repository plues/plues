package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;

import org.controlsfx.control.Notifications;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nullable;
import javax.swing.SwingUtilities;

public class ResultBox extends GridPane implements Initializable {

  private static final String ICON_SIZE = "50";
  private static final String WARNING_COLOR = "#FEEFB3";
  private static final String FAILURE_COLOR = "#FFBABA";
  private static final String SUCCESS_COLOR = "#DFF2BF";
  private static final String WORKING_COLOR = "#BDE5F8";

  private final ObjectProperty<Course> majorCourse;
  private final ObjectProperty<Course> minorCourse;
  private final ObjectProperty<Path> pdf;
  private final PdfRenderingTask task;
  private final ExecutorService executor;

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
  private Button show;

  @FXML
  private Button cancel;

  @FXML
  private Button save;

  /**
   * Constructor for ResultBox.
   *
   * @param loader TaskLoader to load fxml and to set controller
   * @param task Rendering task used handling tasks
   * @param major Major course
   * @param minor Minor course if present, else null
   */
  @Inject
  public ResultBox(final FXMLLoader loader,
                   final PdfRenderingTask task,
                   ExecutorService executorService,
                   @Assisted("major") final Course major,
                   @Nullable @Assisted("minor") final Course minor) {
    super();
    this.majorCourse = new SimpleObjectProperty<>(major);
    this.minorCourse = new SimpleObjectProperty<>(minor);
    this.pdf = new SimpleObjectProperty<>();
    this.task = task;
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
    task.majorProperty().bind(this.majorCourse);
    task.minorProperty().bind(this.minorCourse);
    task.setOnSucceeded(event -> {
      Platform.runLater(() ->
          pdf.set((Path) event.getSource().getValue()));
    });

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
      Notifications message = Notifications.create();
      message.title("Error! Could not generate PDF");
      message.text(event.getSource().getException().getMessage());
      message.show();
    });
    //
    this.progressIndicator.setStyle(
        " -fx-progress-color: " + WORKING_COLOR);
    this.progressIndicator.visibleProperty()
        .bind(task.runningProperty());
    //
    // Binding the progress property of the indicator shows a the percentage
    // of completion which in this case is arbitrary since we do not know how
    // long the process will take.
    //
    // progressIndicator.progressProperty().bind(this.task.progressProperty());
    final BooleanBinding p = this.pdf.isNull();
    //
    this.show.disableProperty().bind(p);
    this.save.disableProperty().bind(p);
    this.cancel.disableProperty().bind(task.runningProperty().not());
    //
    this.icon.graphicProperty().bind(this.getIconBinding());
    this.icon.styleProperty().bind(this.getStyleBinding());
    //
    executor.submit(task);
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

      final FontAwesomeIconFactory iconFactory
          = FontAwesomeIconFactory.get();
      return iconFactory.createIcon(symbol, ICON_SIZE);

    }, task.stateProperty());
  }

  /**
   * Helper function to find the file name containing major and minor name.
   *
   * @param major Course object representing the choosen major course
   * @param minor Course object representing the choosen minor course
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

  @FXML
  private final void showPdf() {
    final Path file = pdf.get();
    SwingUtilities.invokeLater(() -> {
      try {
        Desktop.getDesktop().open(file.toFile());
      } catch (IOException exc) {
        Notifications message = Notifications.create();
        message.text("Error! File could not be opened.");
        message.show();
        exc.printStackTrace();
      }
    });
  }

  @FXML
  private final void savePdf() {
    final DirectoryChooser directoryChooser = new DirectoryChooser();
    directoryChooser.setTitle("Choose the pdf file's location");
    final File selectedDirectory = directoryChooser.showDialog(null);

    String documentName;
    if (minorCourse.get() == null) {
      documentName = getDocumentName(majorCourse.get());
    } else {
      documentName = getDocumentName(majorCourse.get(), minorCourse.get());
    }

    if (selectedDirectory != null) {
      try {
        Files.copy(pdf.get(),
            Paths.get(selectedDirectory.getAbsolutePath()).resolve(documentName));
      } catch (Exception exc) {
        Notifications message = Notifications.create();
        message.text("Error! Copying of temporary file into target file failed.");
        message.show();
        exc.printStackTrace();
      }
    }
  }

  @FXML
  final void interrupt() {
    this.task.cancel();
  }
}
