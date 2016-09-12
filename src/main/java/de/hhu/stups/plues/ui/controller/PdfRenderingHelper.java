package de.hhu.stups.plues.ui.controller;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

import javax.swing.SwingUtilities;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.scene.control.Label;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

public class PdfRenderingHelper {

  private static final String ICON_SIZE = "50";
  private static final String WARNING_COLOR = "#FEEFB3";
  private static final String FAILURE_COLOR = "#FFBABA";
  private static final String SUCCESS_COLOR = "#DFF2BF";
  //  private static final String WORKING_COLOR = "#BDE5F8";
  public static final String PDF_SAVE_DIR = "LAST_PDF_SAVE_DIR";
  private static Preferences preferences;


  public static void showPdf(ObjectProperty<Path> pdf, Label lbErrorMsg) {
    final Path file = pdf.get();
    SwingUtilities.invokeLater(() -> {
      try {
        Desktop.getDesktop().open(file.toFile());
      } catch (final IOException exc) {
        if (lbErrorMsg != null) {
          lbErrorMsg.setText("Error! Copying of temporary file into target file failed.");
        } else {
          exc.printStackTrace();
        }
      }
    });
  }

  public static void savePdf(ObjectProperty<Path> pdf, Course major, Course minor,
                             Class cl, Label lbErrorMsg) {
    preferences = Preferences.userNodeForPackage(cl);
    final File file = getTargetFile(major, minor);

    if (file != null) {
      try {
        Files.copy(pdf.get(), Paths.get(file.getAbsolutePath()));
      } catch (final Exception exc) {
        if (lbErrorMsg != null) {
          lbErrorMsg.setText("Error! Copying of temporary file into target file failed.");
        } else {
          exc.printStackTrace();
        }
      }
    }
  }

  private static File getTargetFile(final Course majorCourse, final Course minorCourse) {

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

  public static ObjectBinding<Text> getIconBinding(PdfRenderingTask task) {
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

  public static StringBinding getStyleBinding(PdfRenderingTask task) {
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
}
