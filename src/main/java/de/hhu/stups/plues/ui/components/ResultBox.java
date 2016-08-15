package de.hhu.stups.plues.ui.components;

import static javafx.concurrent.Worker.State;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.FeasibilityResult;
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
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

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
  private Button download;

  @FXML
  private Button cancel;

  /**
   * Constructor for ResultBox.
   * @param loader TaskLoader to load fxml and to set controller
   * @param task Task which is bind to this result box
   */
  @Inject
  public ResultBox(final FXMLLoader loader,
                   @Assisted final Worker<FeasibilityResult> task) {
    super();
    this.majorCourse = new SimpleObjectProperty<>();
    this.minorCourse = new SimpleObjectProperty<>();
    this.feasible = new SimpleBooleanProperty(false);
    this.task = task;
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
    this.download.disableProperty().bind(p);
    this.cancel.disableProperty().bind(this.task.runningProperty().not());
    //
    this.icon.graphicProperty().bind(this.getIconBinding());
    this.icon.styleProperty().bind(this.getStyleBinding());
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

  @FXML
  public void showPdf() {

  }

  @FXML
  public void downloadPdf() {

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
