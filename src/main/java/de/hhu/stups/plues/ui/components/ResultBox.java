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

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
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
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import java.awt.*;
import java.io.IOException;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.prefs.Preferences;

import javax.annotation.Nullable;
import javax.swing.*;

public class ResultBox extends GridPane implements Initializable {

  private static final String ICON_SIZE = "50";
  private static final String WARNING_COLOR = "#FEEFB3";
  private static final String FAILURE_COLOR = "#FFBABA";
  private static final String SUCCESS_COLOR = "#DFF2BF";
  private static final String WORKING_COLOR = "#BDE5F8";
  public static final String PDF_SAVE_DIR = "LAST_PDF_SAVE_DIR";

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
  private final Preferences preferences = Preferences.userNodeForPackage(this.getClass());

  /**
   * Constructor for ResultBox.
   *
   * @param loader      TaskLoader to load fxml and to set controller
   * @param renderingTaskFactory PDF Rendering task Factory
   * @param major       Major course
   * @param minor       Minor course if present, else null
   * @param parent      The parent wrapper (VBox) to remove a single result box.
   */
  @Inject
  public ResultBox(final FXMLLoader loader,
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

      final FontAwesomeIconFactory iconFactory = FontAwesomeIconFactory.get();
      return iconFactory.createIcon(symbol, ICON_SIZE);

    }, task.stateProperty());
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
    final Path file = pdf.get();
    SwingUtilities.invokeLater(() -> {
      try {
        Desktop.getDesktop().open(file.toFile());
      } catch (final IOException exc) {
        this.lbErrorMsg.setText("Error! File could not be opened.");
      }
    });
  }

  @FXML
  private void savePdf() {
    final File file = getTargetFile();

    if (file != null) {
      try {
        Files.copy(pdf.get(), Paths.get(file.getAbsolutePath()));
      } catch (final Exception exc) {
        this.lbErrorMsg.setText("Error! Copying of temporary file into target file failed.");
      }
    }
  }

  private File getTargetFile() {
    final Course minorCourse = this.minorCourse.get();
    final Course majorCourse = this.majorCourse.get();

    final String documentName;
    if (minorCourse == null) {
      documentName = getDocumentName(majorCourse);
    } else {
      documentName = getDocumentName(majorCourse, minorCourse);
    }

    final String initialDirectory = preferences.get(PDF_SAVE_DIR, System.getProperty("user.home"));

    final FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Choose the pdf file's location");
    fileChooser.setInitialDirectory(new File(initialDirectory));
    fileChooser.setInitialFileName(documentName);
    fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("PDF", "*.pdf"));

    final File file = fileChooser.showSaveDialog(null);
    if (file != null) {
      preferences.put(PDF_SAVE_DIR, file.getAbsoluteFile().getParent());
    }

    return file;
  }

  @FXML
  private void interrupt() {
    this.task.cancel();
  }
}
