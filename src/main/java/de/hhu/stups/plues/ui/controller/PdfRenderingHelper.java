package de.hhu.stups.plues.ui.controller;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.components.MajorMinorCourseSelection;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

public class PdfRenderingHelper {

  private static final String ICON_SIZE = "50";
  private static final String WARNING_COLOR = "#FEEFB3";
  private static final String FAILURE_COLOR = "#FFBABA";
  private static final String SUCCESS_COLOR = "#DFF2BF";
  private static final String PDF_SAVE_DIR = "LAST_PDF_SAVE_DIR";

  private static final Logger logger = Logger.getLogger(PdfRenderingHelper.class.getSimpleName());

  /**
   * Unified function to show a pdf. On error callback will be invoked
   *
   * @param file       Temporary file to show pdf
   * @param callback Consumer (callback) to be invoked if opening the document fails.
   */
  public static void showPdf(final Path file, final Consumer<Exception> callback) {
    SwingUtilities.invokeLater(() -> {
      try {
        Desktop.getDesktop().open(file.toFile());
      } catch (final IOException exc) {
        logger.log(Level.INFO, "Error! Copying of temporary file into target file failed.");
        exc.printStackTrace();
        if(callback != null) {
          callback.accept(exc);
        }
      }
    });
  }

  /**
   * Function to show a pdf. Errors will be ignored
   *
   * @param file       Temporary file to show pdf
   */
  static void showPdf(final Path file) {
    showPdf(file, null);
  }

  /**
   * Unified function to save a pdf for a given major and minor course. Error messages will
   * be printed on label if present or on stack trace.
   *
   * @param pdf        Path to pdf to save
   * @param major      Major course for pdf
   * @param minor      Minor course for pdf (could be null)
   * @param lbErrorMsg Label to print error messages on. Can be null
   */
  public static void savePdf(final Path pdf, final Course major, final Course minor,
                             final Label lbErrorMsg) {
    final File file = getTargetFile(major, minor);

    if (file != null) {
      try {
        Files.copy(pdf, Paths.get(file.getAbsolutePath()));
      } catch (final Exception exc) {
        if (lbErrorMsg != null) {
          lbErrorMsg.setText("Error! Copying of temporary file into target file failed.");
        } else {
          exc.printStackTrace();
        }
      }
    }
  }

  /**
   * Find the target file choosen by the user.
   *
   * @param majorCourse Major course
   * @param minorCourse Minor course
   * @return File object representing the choosen file by user
   */
  private static File getTargetFile(final Course majorCourse, final Course minorCourse) {

    final String documentName;
    if (minorCourse == null) {
      documentName = getDocumentName(majorCourse);
    } else {
      documentName = getDocumentName(majorCourse, minorCourse);
    }

    final Preferences preferences = Preferences.userNodeForPackage(PdfRenderingHelper.class);
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

  /**
   * Collect icon bindung for a given task. Depends on how the task behaves.
   *
   * @param task Given task
   * @return Object binding depending on the tasks state
   */
  public static ObjectBinding<Text> getIconBinding(final PdfRenderingTask task) {
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

  /**
   * Collect string binding for given task.
   *
   * @param task Given task
   * @return String binding depending on the tasks state
   */
  public static StringBinding getStyleBinding(final PdfRenderingTask task) {
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

  // TODO: ggf. wieder woanders hin

  /**
   * Initialize course selection object of each class using it.
   *
   * @param store           Store object to collect courses
   * @param courseSelection Object to save selection.
   * @param delayedSolverService solverService to retrieve impossible courses
   *
   */
  static void initializeCourseSelection(final Store store,
                                        // TODO: this should not be parameter
                                        // but instead be constructed here and returned
                                        final MajorMinorCourseSelection courseSelection,
                                        final Delayed<SolverService> delayedSolverService) {
    final List<Course> courses = store.getCourses();

    final List<Course> majorCourseList = courses.stream()
      .filter(Course::isMajor)
      .collect(Collectors.toList());

    final List<Course> minorCourseList = courses.stream()
      .filter(Course::isMinor)
      .collect(Collectors.toList());

    courseSelection.setMajorCourseList(FXCollections.observableList(majorCourseList));
    courseSelection.setMinorCourseList(FXCollections.observableList(minorCourseList));

    // register task to highlight impossible courses
    delayedSolverService.whenAvailable(solverService -> {
      final SolverTask<Set<String>> impossibleCoursesTask = solverService.impossibleCoursesTask();
      impossibleCoursesTask.setOnSucceeded(event ->
        courseSelection.highlightImpossibleCourses(impossibleCoursesTask.getValue()));
      solverService.submit(impossibleCoursesTask);
    });
  }
}
