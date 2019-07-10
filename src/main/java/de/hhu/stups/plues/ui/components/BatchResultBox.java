package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.hhu.stups.plues.ui.controller.PdfRenderingHelper;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import java.nio.file.Path;
import java.util.ResourceBundle;

public class BatchResultBox extends GridPane {

  private final PdfRenderingTask task;
  private final ObjectProperty<Path> pdfPathProperty;

  @FXML
  private ResourceBundle resources;

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

    pdfPathProperty = new SimpleObjectProperty<>();
    task.setOnSucceeded(event -> pdfPathProperty.set((Path) event.getSource().getValue()));

    inflater.inflate("components/BatchResultBox", this, this, "batchTimetable");
  }

  @FXML
  public final void initialize() {
    taskProgressIndicator.sizeProperty().set(30.0);
    taskProgressIndicator.taskProperty().set(task);

    lbMajor.textProperty().bind(Bindings.selectString(task.getMajor(), "fullName"));

    final Course minor = task.getMinor();
    if (minor != null) {
      lbMinor.textProperty().bind(Bindings.selectString(minor, "fullName"));
    }
  }

  /**
   * Show the generated pdf located at {@link #pdfPathProperty}.
   */
  public void showPdf() {
    if (pdfPathProperty.isNotNull().get()) {
      PdfRenderingHelper.showPdf(pdfPathProperty.get());
    }
  }

  @SuppressWarnings("unused")
  public String getMajorCourseName() {
    return task.getMajor().getFullName();
  }

  /**
   * Return the full name of the {@link #task}'s minor course or an empty string if null.
   */
  @SuppressWarnings("unused")
  public String getMinorCourseName() {
    if (task.getMinor() == null) {
      return "";
    }
    return task.getMinor().getFullName();
  }

  /**
   * Return a string describing the final state of the {@link #task}. Used in the Jtwig template,
   * and thus, might be considered to be unused in the java code.
   */
  @SuppressWarnings("unused")
  public String getTaskStateString() {
    switch (task.getState()) {
      case SUCCEEDED:
        return resources.getString("succeeded");
      case CANCELLED:
        return resources.getString("cancelled");
      case FAILED:
        return resources.getString("failed");
      default:
        return "";
    }
  }
}
