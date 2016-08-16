package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.studienplaene.Renderer;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
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
import org.xml.sax.SAXException;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import javax.swing.SwingUtilities;
import javax.xml.parsers.ParserConfigurationException;

public class ResultBox extends GridPane implements Initializable {

  private static final String ICON_SIZE = "50";
  private static final String WARNING_COLOR = "#FEEFB3";
  private static final String FAILURE_COLOR = "#FFBABA";
  private static final String SUCCESS_COLOR = "#DFF2BF";
  private static final String WORKING_COLOR = "#BDE5F8";

  private final Task<FeasibilityResult> task;
  private final BooleanProperty feasible;
  private final ObjectProperty<Course> majorCourse;
  private final ObjectProperty<Course> minorCourse;
  private final Delayed<Store> delayedStore;
  private final ExecutorService executor;
  private final ObjectProperty<Path> pdf;

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
   * @param task   Task which is bind to this result box
   */
  @Inject
  public ResultBox(final FXMLLoader loader,
                   final ExecutorService executor,
                   final Delayed<Store> delayedStore,
                   @Assisted final Task<FeasibilityResult> task) {
    super();
    this.majorCourse = new SimpleObjectProperty<>();
    this.minorCourse = new SimpleObjectProperty<>();
    this.feasible = new SimpleBooleanProperty(false);
    this.pdf = new SimpleObjectProperty<>();
    this.task = task;
    this.executor = executor;
    this.delayedStore = delayedStore;
    this.feasible.bind( // set if task has a value
        Bindings.createBooleanBinding(() -> true, task.valueProperty()));

    loader.setLocation(this.getClass()
        .getResource("/fxml/components/resultbox.fxml"));

    loader.setRoot(this);
    loader.setController(this);

    task.setOnSucceeded(event -> {
      if (feasible.get()) {
        final Course major = majorCourse.get();
        final Course minor = minorCourse.get();
        FeasibilityResult result = null;
        try {
          result = task.get();
        } catch (InterruptedException | ExecutionException exc) {
          exc.printStackTrace();
        }
        final FeasibilityResult finalResult = result;
        Task<Path> pdfCreationTask = new Task<Path>() {
          @Override
          protected Path call() throws Exception {
            return handlePdf(major, minor, finalResult);
          }
        };
        pdfCreationTask.setOnSucceeded(creationEvent ->
            Platform.runLater(() ->
              pdf.set((Path) creationEvent.getSource().getValue())));
        submit(pdfCreationTask);
      }
    });

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
    this.progressIndicator.setStyle(
        " -fx-progress-color: " + WORKING_COLOR);
    this.progressIndicator.visibleProperty()
        .bind(this.task.runningProperty());
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
  }

  /**
   * Helper method to get temporary file.
   * @param renderer Renderer object to create file
   * @param temp Temporary file
   */
  private void getTempFile(Renderer renderer, File temp) {
    try (OutputStream out = new FileOutputStream(temp)) {
      renderer.getResult().writeTo(out);
    } catch (final IOException exc) {
      Notifications message = Notifications.create();
      message.text("Error! Temporary file was not found.");
      message.show();
      exc.printStackTrace();
    } catch (final ParserConfigurationException exc) {
      Notifications message = Notifications.create();
      message.text("Error! Parsing failed.");
      message.show();
      exc.printStackTrace();
    } catch (final SAXException exc) {
      Notifications message = Notifications.create();
      message.text("Error! API throws an exception. Contact development.");
      message.show();
      exc.printStackTrace();
    }
  }

  private File getTempFile(Course major, Course minor) {
    try {
      if (minor == null) {
        return File.createTempFile(getDocumentName(major), ".pdf");
      } else {
        return File.createTempFile(getDocumentName(major, minor), ".pdf");
      }
    } catch (IOException exc) {
      Notifications message = Notifications.create();
      message.text("Error! Creation of temporary file failed");
      message.show();
      exc.printStackTrace();
      return null;
    }
  }

  private StringBinding getStyleBinding() {
    return Bindings.createStringBinding(() -> {
      String color = null;

      switch (this.task.getState()) {
        case READY:
        case SCHEDULED:
        case RUNNING:
          return "";

        case SUCCEEDED:
          if (this.feasible.get()) {
            color = SUCCESS_COLOR;
          } else {
            color = FAILURE_COLOR;
          }
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

    }, this.task.stateProperty(), this.feasible);
  }

  private ObjectBinding<Text> getIconBinding() {
    return Bindings.createObjectBinding(() -> {
      FontAwesomeIcon symbol = null;

      switch (this.task.getState()) {
        case READY:
        case SCHEDULED:
        case RUNNING:
          return null;

        case SUCCEEDED:
          if (this.feasible.get()) {
            symbol = FontAwesomeIcon.CHECK;
          } else {
            symbol = FontAwesomeIcon.REMOVE;
          }
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

    }, this.task.stateProperty(), this.feasible);
  }

  /**
   * Handle pdf file. If show is true, create a temporary file and open it, if false, save this temp
   * file where ever the user wants to to be saved.
   * @param major Course object representing major course
   * @param minor Course object representing minor course. Could be null if major is integrated
   * @param result Instance of FeasiblityResult containing choice maps
   */
  private Path handlePdf(Course major, Course minor, FeasibilityResult result) {
    final Store store = delayedStore.get();
    final Renderer renderer
        = new Renderer(store, result.getGroupChoice(), result.getSemesterChoice(),
        result.getModuleChoice(), result.getUnitChoice(), major, "true");

    File tmp = getTempFile(major, minor);

    getTempFile(renderer, tmp);
    return Paths.get(tmp.getAbsolutePath());
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

  public final void setMajorCourse(final Course majorCourse) {
    this.majorCourse.set(majorCourse);
  }

  public final void setMinorCourse(final Course minorCourse) {
    this.minorCourse.set(minorCourse);
  }

  public final void submit(final Task<?> command) {
    this.executor.submit(command);
  }
}
