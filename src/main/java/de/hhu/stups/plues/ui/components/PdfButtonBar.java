package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Course;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
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

import javax.swing.SwingUtilities;

public class PdfButtonBar extends ButtonBar implements Initializable {

  private Course major;
  private Course minor;
  private Task task;
  private final ObjectProperty<Path> pdf;

  /**
   * Constructor for pdf button bar.
   * @param loader FXMLLoader
   */
  @Inject
  public PdfButtonBar(final FXMLLoader loader) {
    super();
    pdf = new SimpleObjectProperty<>();

    loader.setLocation(this.getClass()
        .getResource("/fxml/components/pdfButtonBar.fxml"));

    loader.setRoot(this);
    loader.setController(this);

    try {
      loader.load();
    } catch (final IOException exception) {
      throw new RuntimeException(exception);
    }
  }

  @FXML
  @SuppressWarnings("unused")
  private Button show;

  @FXML
  @SuppressWarnings("unused")
  private Button save;

  @FXML
  @SuppressWarnings("unused")
  private Button cancel;

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    final BooleanBinding p = this.pdf.isNull();
    //
    show.disableProperty().bind(p);
    save.disableProperty().bind(p);
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
  private void showPdf() {
    final Path file = pdf.get();
    SwingUtilities.invokeLater(() -> {
      try {
        Desktop.getDesktop().open(file.toFile());
      } catch (final IOException exc) {
        final Notifications message = Notifications.create();
        message.text("Error! File could not be opened.");
        message.show();
        exc.printStackTrace();
      }
    });
  }

  @FXML
  private void savePdf() {
    final DirectoryChooser directoryChooser = new DirectoryChooser();
    directoryChooser.setTitle("Choose the pdf file's location");
    final File selectedDirectory = directoryChooser.showDialog(null);

    final String documentName;
    if (minor == null) {
      documentName = getDocumentName(major);
    } else {
      documentName = getDocumentName(major, minor);
    }

    if (selectedDirectory != null) {
      try {
        Files.copy(pdf.get(), Paths.get(selectedDirectory.getAbsolutePath()).resolve(documentName));
      } catch (final Exception exc) {
        final Notifications message = Notifications.create();
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

  /**
   * Set major course.
   * @param major Major
   */
  public void setMajor(Course major) {
    this.major = major;
  }

  /**
   * Set minor course.
   * @param minor Minor
   */
  public void setMinor(Course minor) {
    this.minor = minor;
  }

  /**
   * Set task for this button bar.
   * @param task Given task
   */
  public void setTask(Task<Path> task) {
    this.task = task;
    cancel.disableProperty().bind(task.runningProperty().not());

    task.setOnSucceeded(event
        -> Platform.runLater(() -> pdf.set((Path) event.getSource().getValue())));

    task.setOnFailed(event -> {
      final Notifications message = Notifications.create();
      message.title("Error! Could not generate PDF");
      Throwable exception = event.getSource().getException();
      message.text(exception.getMessage());
      message.show();
      exception.printStackTrace();
    });
  }

  /**
   * Set task for this button bar if partial timetables are used.
   * @param task Given task
   */
  public void setTask(SolverTask<FeasibilityResult> task) {
    this.task = task;
    cancel.disableProperty().bind(task.runningProperty().not());
    // TODO: was passiert mit der Task? wie wird das pdf hier erzeugt?
  }
}
