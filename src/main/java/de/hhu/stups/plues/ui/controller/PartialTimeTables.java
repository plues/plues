package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.hhu.stups.plues.tasks.PdfRenderingTaskFactory;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.components.CheckBoxGroup;
import de.hhu.stups.plues.ui.components.CheckBoxGroupFactory;
import de.hhu.stups.plues.ui.components.MajorMinorCourseSelection;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javax.swing.*;

public class PartialTimeTables extends GridPane implements Initializable {

  private static final String ICON_SIZE = "50";
  private static final String WARNING_COLOR = "#FEEFB3";
  private static final String FAILURE_COLOR = "#FFBABA";
  private static final String SUCCESS_COLOR = "#DFF2BF";
//  private static final String WORKING_COLOR = "#BDE5F8";
  public static final String PDF_SAVE_DIR = "LAST_PDF_SAVE_DIR";

  private final Delayed<Store> delayedStore;
  private final Delayed<SolverService> delayedSolverService;

  private final BooleanProperty solverProperty;
  private final BooleanProperty generationStarted;
  private final BooleanProperty checkStarted;
  private final CheckBoxGroupFactory checkBoxGroupFactory;
  private final SimpleObjectProperty storeProperty;
  private final BooleanProperty resultProperty;
  private final SimpleBooleanProperty storeAvailable;

  private final PdfRenderingTaskFactory renderingTaskFactory;
  private final ExecutorService executor;
  private PdfRenderingTask task;
  private ObjectProperty<Path> pdf;
  private Course major;
  private Course minor;

  private final Preferences preferences = Preferences.userNodeForPackage(this.getClass());

  @FXML
  @SuppressWarnings("unused")
  private MajorMinorCourseSelection courseSelection;

  @FXML
  @SuppressWarnings("unused")
  private Button btChoose;

  @FXML
  @SuppressWarnings("unused")
  private ScrollPane scrollPane;

  @FXML
  @SuppressWarnings("unused")
  private VBox modulesUnits;

  @FXML
  @SuppressWarnings("unused")
  private Button btCheck;

  @FXML
  @SuppressWarnings("unused")
  private Label icon;
  @FXML
  @SuppressWarnings("unused")
  private VBox buttons;

  /**
   * Constructor for partial time table controller.
   *
   * @param loader               TaskLoader to load fxml file and to set controller
   * @param delayedStore         Store containing relevant data
   * @param delayedSolverService SolverService for usage of ProB solver
   * @param checkBoxGroupFactory Factory to create check box groups
   */
  @Inject
  public PartialTimeTables(final FXMLLoader loader, final Delayed<Store> delayedStore,
                           final Delayed<SolverService> delayedSolverService,
                           final PdfRenderingTaskFactory renderingTaskFactory,
                           final ExecutorService executor,
                           final CheckBoxGroupFactory checkBoxGroupFactory) {
    this.delayedStore = delayedStore;
    this.delayedSolverService = delayedSolverService;
    this.checkBoxGroupFactory = checkBoxGroupFactory;
    this.renderingTaskFactory = renderingTaskFactory;
    this.executor = executor;

    this.storeProperty = new SimpleObjectProperty();
    this.storeAvailable = new SimpleBooleanProperty(false);
    this.solverProperty = new SimpleBooleanProperty(false);
    this.generationStarted = new SimpleBooleanProperty(false);
    this.checkStarted = new SimpleBooleanProperty(false);
    this.resultProperty = new SimpleBooleanProperty(false);
    this.pdf = new SimpleObjectProperty<>();

    this.setVgap(10.0);

    loader.setLocation(getClass().getResource("/fxml/PartialTimeTables.fxml"));

    loader.setRoot(this);
    loader.setController(this);

    try {
      loader.load();
    } catch (final IOException exception) {
      throw new RuntimeException(exception);
    }
  }

  /**
   * Function to generate checkboxes for modules and units.
   */
  @FXML
  @SuppressWarnings("unused")
  public void btChoosePressed() {
    generationStarted.set(true);
    checkStarted.set(false);
    modulesUnits.getChildren().clear();
    resultProperty.set(false);

    Course major = courseSelection.getSelectedMajorCourse();
    Text majorText = new Text();
    majorText.setText(major.getFullName());
    majorText.setUnderline(true);
    modulesUnits.getChildren().add(majorText);

    Course minor = null;
    if (courseSelection.getSelectedMinorCourse().isPresent()) {
      minor = courseSelection.getSelectedMinorCourse().get();
      Text minorText = new Text();
      minorText.setText(minor.getFullName());
      minorText.setUnderline(true);
      modulesUnits.getChildren().add(minorText);
    }

    for (Module m : major.getModules()) {
      modulesUnits.getChildren().add(1, createCheckBoxGroup(m, major));
    }
    if (minor != null) {
      for (Module m : minor.getModules()) {
        modulesUnits.getChildren().add(createCheckBoxGroup(m, minor));
      }
    }
  }

  private Node createCheckBoxGroup(Module module, Course course) {
    CheckBoxGroup cbg = checkBoxGroupFactory.create(course, module, module.getAbstractUnits());
    return cbg;
  }

  /**
   * Function to pass selection to solver and check if feasible.
   */
  @FXML
  @SuppressWarnings("unused")
  public void btCheckPressed() throws InterruptedException {
    checkStarted.set(true);
    this.major = courseSelection.getSelectedMajorCourse();

    Map<Course, List<Module>> moduleChoice = new HashMap<>();
    List<AbstractUnit> unitChoice = new ArrayList<>();
    moduleChoice.put(major, new ArrayList<>());

    List<Course> courses = new ArrayList<>();
    courses.add(major);
    this.minor = null;
    if (courseSelection.getSelectedMinorCourse().isPresent()) {
      minor = courseSelection.getSelectedMinorCourse().get();
      moduleChoice.put(minor, new ArrayList<>());
      courses.add(minor);
    }

    for (Object o : modulesUnits.getChildren()) {
      CheckBoxGroup cbg;
      try {
        cbg = (CheckBoxGroup) o;
      } catch (ClassCastException exc) {
        continue;
      }
      Module module = cbg.getModule();
      if (module != null) {
        moduleChoice.get(cbg.getCourse()).add(module);
      }

      unitChoice.addAll(cbg.getSelectedAbstractUnits());
    }

    Course finalMinor = minor;
    delayedSolverService.whenAvailable(solverService -> {
      SolverTask<FeasibilityResult> solverTask =
          solverService.computePartialFeasibility(courses, moduleChoice, unitChoice);

      task = renderingTaskFactory.create(major, finalMinor, solverTask);

      task.setOnSucceeded(event -> {
        pdf.set((Path) event.getSource().getValue());
        buttons.setDisable(false);
      });

      icon.styleProperty().bind(this.getStyleBinding());
      icon.graphicProperty().bind(this.getIconBinding());

      executor.submit(task);
    });
  }

  @Override
  public final void initialize(final URL location, final ResourceBundle resources) {
    btChoose.setDefaultButton(true);
    btChoose.disableProperty().bind(storeAvailable.not());
    //
    modulesUnits.visibleProperty().bind(generationStarted);
    scrollPane.visibleProperty().bind(generationStarted);
    btCheck.visibleProperty().bind(generationStarted);
    btCheck.disableProperty().bind(solverProperty.not());
    //
    icon.visibleProperty().bind(checkStarted);
    buttons.visibleProperty().bind(checkStarted);
    buttons.setDisable(true);
    //
    delayedStore.whenAvailable(s -> {
      initializeCourseSelection(s);
      this.storeProperty.set(s);
      this.storeAvailable.set(true);
    });

    delayedSolverService.whenAvailable(s -> {
      this.solverProperty.set(true);

      final SolverTask<Set<String>> impossibleCoursesTask = s.impossibleCoursesTask();

      impossibleCoursesTask.setOnSucceeded(event ->
          courseSelection.highlightImpossibleCourses((Set<String>) event.getSource().getValue()));
      s.submit(impossibleCoursesTask);
    });
  }

  private void initializeCourseSelection(final Store store) {
    final List<Course> courses = store.getCourses();

    final List<Course> majorCourseList = courses.stream()
        .filter(Course::isMajor)
        .collect(Collectors.toList());

    final List<Course> minorCourseList = courses.stream()
        .filter(Course::isMinor)
        .collect(Collectors.toList());

    courseSelection.setMajorCourseList(FXCollections.observableList(majorCourseList));
    courseSelection.setMinorCourseList(FXCollections.observableList(minorCourseList));
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

  @FXML
  private void showPdf() {
    final Path file = pdf.get();
    SwingUtilities.invokeLater(() -> {
      try {
        Desktop.getDesktop().open(file.toFile());
      } catch (final IOException exc) {
        exc.printStackTrace();
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
        exc.printStackTrace();
      }
    }
  }

  private File getTargetFile() {
    final Course minorCourse = this.minor;
    final Course majorCourse = this.major;

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
}
