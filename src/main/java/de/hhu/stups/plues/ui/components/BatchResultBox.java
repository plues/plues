package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.util.ResourceBundle;

public class BatchResultBox extends GridPane implements Initializable {

  private final PdfRenderingTask task;

  @FXML
  @SuppressWarnings("unused")
  private TaskProgressIndicator taskProgressIndicator;
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

    inflater.inflate("components/BatchResultBox", this, this, "batchTimetable");
  }

  @Override
  public final void initialize(final URL location, final ResourceBundle resources) {
    taskProgressIndicator.sizeProperty().set(30.0);
    taskProgressIndicator.taskProperty().set(task);

    lbMajor.textProperty().bind(Bindings.selectString(task.getMajor(), "fullName"));

    final Course minor = task.getMinor();
    if (minor != null) {
      lbMinor.textProperty().bind(Bindings.selectString(minor, "fullName"));
    }
  }
}
