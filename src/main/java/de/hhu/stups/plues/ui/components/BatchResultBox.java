package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.hhu.stups.plues.ui.TaskBindings;
import de.hhu.stups.plues.ui.layout.Inflater;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

public class BatchResultBox extends GridPane implements Initializable {

  private static final String ICON_SIZE = "15";
  private static final String WORKING_COLOR = "#BDE5F8";

  private final PdfRenderingTask task;

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
   * @param inflater Inflater to handle TaskLoader for fxml and to set controller
   */
  @Inject
  public BatchResultBox(final Inflater inflater, @Assisted final PdfRenderingTask task) {
    super();
    assert task != null;
    this.task = task;
    this.setHgap(10.0);

    inflater.inflate("components/BatchResultBox", this, this, "batchTimetable");
  }

  @Override
  public final void initialize(final URL location, final ResourceBundle resources) {
    this.lbMajor.textProperty().bind(Bindings.selectString(task.getMajor(), "fullName"));
    //
    final Course minor = task.getMinor();
    if (minor != null) {
      this.lbMinor.textProperty().bind(Bindings.selectString(minor, "fullName"));
    }

    this.progressIndicator.setStyle(" -fx-progress-color: " + WORKING_COLOR);
    this.progressIndicator.visibleProperty().bind(task.runningProperty());
    this.icon.visibleProperty().bind(task.runningProperty().not());
    this.icon.graphicProperty().bind(TaskBindings.getIconBinding(ICON_SIZE, this.task));
    this.icon.styleProperty().bind(TaskBindings.getStyleBinding(this.task));
  }
}
