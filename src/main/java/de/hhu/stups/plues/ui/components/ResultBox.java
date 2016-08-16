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

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
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

import org.xml.sax.SAXException;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import javax.swing.SwingUtilities;
import javax.xml.parsers.ParserConfigurationException;

public class ResultBox extends GridPane implements Initializable {

  private static final String ICON_SIZE = "50";
  private static final String WARNING_COLOR = "#FEEFB3";
  private static final String FAILURE_COLOR = "#FFBABA";
  private static final String SUCCESS_COLOR = "#DFF2BF";
  private static final String WORKING_COLOR = "#BDE5F8";

  private final Worker<FeasibilityResult> task;
  private final BooleanProperty feasible;
  private final ObjectProperty<Course> majorCourse;
  private final ObjectProperty<Course> minorCourse;
  private final Delayed<Store> delayedStore;

  private File temp;

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
                   @Assisted final Worker<FeasibilityResult> task,
                   @Assisted final Delayed<Store> delayedStore) {
    super();
    this.majorCourse = new SimpleObjectProperty<>();
    this.minorCourse = new SimpleObjectProperty<>();
    this.feasible = new SimpleBooleanProperty(false);
    this.task = task;
    this.delayedStore = delayedStore;
    this.feasible.bind( // set if task has a value
        Bindings.createBooleanBinding(() -> true, task.valueProperty()));

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
    //
    final BooleanBinding p = this.task.stateProperty()
        .isEqualTo(State.SUCCEEDED).not();
    //
    this.show.disableProperty().bind(p);
    this.save.disableProperty().bind(p);
    this.cancel.disableProperty().bind(this.task.runningProperty().not());
    //
    this.icon.graphicProperty().bind(this.getIconBinding());
    this.icon.styleProperty().bind(this.getStyleBinding());
  }

  /**
   * Helper method to get temporary file.
   * @param renderer Renderer object to create file
   * @param tmp Temporary file
   */
  private void getTempFile(Renderer renderer, File tmp) {
    final File temp = tmp;
    try (OutputStream out = new FileOutputStream(temp)) {
      renderer.getResult().writeTo(out);
    } catch (final IOException | ParserConfigurationException | SAXException exc) {
      exc.printStackTrace();
    }
  }

  private File getTempFile() {
    try {
      if (minorCourse.get() == null) {
        return File.createTempFile(getDocumentName(majorCourse.get()), ".pdf");
      } else {
        return File.createTempFile(getDocumentName(majorCourse.get(), minorCourse.get()), ".pdf");
      }
    } catch (IOException exc) {
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
   */
  private File handlePdf() {
    if (temp != null) {
      return this.temp;
    }

    final FeasibilityResult result = task.getValue();

    final Store store = delayedStore.get();
    final Renderer renderer
        = new Renderer(store, result.getGroupChoice(), result.getSemesterChoice(),
        result.getModuleChoice(), result.getUnitChoice(), majorCourse.get(), "true");


    File tmp = getTempFile();

    getTempFile(renderer, tmp);
    this.temp = tmp;
    return this.temp;
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
    final File file = handlePdf();
    SwingUtilities.invokeLater(() -> {
      try {
        Desktop.getDesktop().open(file);
      } catch (IOException exc) {
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
        Files.copy(Paths.get(handlePdf().getAbsolutePath()),
            Paths.get(selectedDirectory.getAbsolutePath()).resolve(documentName));
      } catch (Exception exc) {
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
}
