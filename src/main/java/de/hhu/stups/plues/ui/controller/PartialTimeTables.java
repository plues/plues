package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.keys.CourseSelection;
import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.services.PdfRenderingService;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.hhu.stups.plues.ui.components.CheckBoxGroup;
import de.hhu.stups.plues.ui.components.CheckBoxGroupFactory;
import de.hhu.stups.plues.ui.components.ColorSchemeSelection;
import de.hhu.stups.plues.ui.components.MajorMinorCourseSelection;
import de.hhu.stups.plues.ui.components.TaskProgressIndicator;
import de.hhu.stups.plues.ui.layout.Inflater;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;


public class PartialTimeTables extends GridPane implements Initializable, Activatable {

  private final BooleanProperty solverProperty;
  private final BooleanProperty checkRunning;
  private final BooleanProperty selectionChanged;

  private final CheckBoxGroupFactory checkBoxGroupFactory;
  private final ObjectProperty<Store> storeProperty;

  private final UiDataService uiDataService;
  private final ObjectProperty<Path> pdf;
  private final ObjectProperty<PdfRenderingTask> currentTaskProperty;
  private final Delayed<Store> delayedStore;
  private final PdfRenderingService pdfRenderingService;

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
  private Button btGenerate;
  @FXML
  @SuppressWarnings("unused")
  private Button btShow;
  @FXML
  @SuppressWarnings("unused")
  private Button btSave;
  @FXML
  @SuppressWarnings("unused")
  private Button btCancel;
  @FXML
  @SuppressWarnings("unused")
  private HBox buttonBox;
  @FXML
  @SuppressWarnings("unused")
  private TaskProgressIndicator taskProgressIndicator;
  @FXML
  @SuppressWarnings("unused")
  private ColorSchemeSelection colorSchemeSelection;

  /**
   * Constructor for partial time table controller.
   *
   * @param delayedStore         Store containing relevant data
   * @param inflater             TaskLoader to load fxml file and to set controller
   * @param checkBoxGroupFactory Factory to create check box groups
   */
  @Inject
  public PartialTimeTables(final Inflater inflater,
                           final UiDataService uiDataService,
                           final Delayed<Store> delayedStore,
                           final PdfRenderingService pdfRenderingService,
                           final CheckBoxGroupFactory checkBoxGroupFactory) {
    this.uiDataService = uiDataService;
    this.delayedStore = delayedStore;
    this.pdfRenderingService = pdfRenderingService;
    this.checkBoxGroupFactory = checkBoxGroupFactory;

    this.storeProperty = new SimpleObjectProperty<>();
    this.solverProperty = new SimpleBooleanProperty(false);
    this.checkRunning = new SimpleBooleanProperty(false);
    this.selectionChanged = new SimpleBooleanProperty(false);
    this.pdf = new SimpleObjectProperty<>();
    currentTaskProperty = new SimpleObjectProperty<>();

    inflater.inflate("PartialTimeTables", this, this, "musterstudienplaene");
  }

  @Override
  public final void initialize(final URL location, final ResourceBundle resources) {
    colorSchemeSelection.defaultInitialization();
    pdfRenderingService.colorSchemeProperty().bind(colorSchemeSelection.selectedColorScheme());

    final BooleanBinding selectionBinding = storeProperty.isNull().or(checkRunning);

    btChoose.disableProperty().bind(selectionBinding);
    courseSelection.disableProperty().bind(selectionBinding);

    courseSelection.addListener(observable -> {
      scrollPane.setVisible(false);
      btGenerate.setVisible(false);
      taskProgressIndicator.taskProperty().set(null);
    });

    btGenerate.disableProperty().bind(solverProperty.not().or(checkRunning));
    btCancel.disableProperty().bind(solverProperty.not().or(checkRunning.not()));
    btCancel.visibleProperty().bind(btGenerate.visibleProperty());
    btShow.visibleProperty().bind(btGenerate.visibleProperty());
    btSave.visibleProperty().bind(btGenerate.visibleProperty());
    btShow.disableProperty().bind(pdf.isNull().or(checkRunning).or(selectionChanged));
    btSave.disableProperty().bind(pdf.isNull().or(checkRunning).or(selectionChanged));

    delayedStore.whenAvailable(store -> {
      courseSelection.setMajorCourseList(FXCollections.observableList(store.getMajors()));

      this.storeProperty.set(store);
    });

    courseSelection.impossibleCoursesProperty().bind(uiDataService.impossibleCoursesProperty());

    this.solverProperty.bind(this.pdfRenderingService.availableProperty());
  }

  /**
   * Function to generate checkboxes for modules and units.
   */
  @FXML
  @SuppressWarnings( {"unused", "WeakerAccess"})
  public void btChoosePressed() {
    selectionChanged.set(false);
    checkRunning.set(false);
    modulesUnits.getChildren().clear();
    scrollPane.setVisible(true);
    btGenerate.setVisible(true);
    pdf.set(null);

    final Course major = courseSelection.getSelectedMajor();
    final Text majorText = new Text();
    majorText.setText(major.getFullName());
    majorText.setUnderline(true);
    modulesUnits.getChildren().add(majorText);

    for (final Module m : major.getModules()) {
      modulesUnits.getChildren().add(createCheckBoxGroup(m, major));
    }

    final Course minor = courseSelection.getSelectedMinor();
    if (minor != null) {
      final Text minorText = new Text();
      minorText.setText(minor.getFullName());
      minorText.setUnderline(true);
      modulesUnits.getChildren().add(minorText);

      for (final Module m : minor.getModules()) {
        modulesUnits.getChildren().add(createCheckBoxGroup(m, minor));
      }
    }
  }

  @FXML
  @SuppressWarnings("unused")
  public void btCancelPressed() {
    currentTaskProperty.get().cancel(true);
    currentTaskProperty.unbind();
  }

  private Node createCheckBoxGroup(final Module module, final Course course) {
    return checkBoxGroupFactory.create(course, module);
  }

  /**
   * Function to pass selection to solver and check if feasible.
   */
  @FXML
  @SuppressWarnings("unused")
  public void btGeneratePressed() throws InterruptedException {
    checkRunning.set(true);

    final List<CheckBoxGroup> cbgs = modulesUnits.getChildren().stream()
        .filter(node -> node instanceof CheckBoxGroup)
        .map(o -> (CheckBoxGroup) o)
        .peek(cbg -> cbg.setOnSelectionChanged(selectionChanged))
        .filter(cbg -> !cbg.getSelectedAbstractUnits().isEmpty())
        .collect(Collectors.toList());
    //
    final Map<Module, List<AbstractUnit>> unitChoice
        = cbgs.stream().collect(Collectors.toMap(
            CheckBoxGroup::getModule,
            CheckBoxGroup::getSelectedAbstractUnits));
    //
    final Map<Course, List<Module>> moduleChoice
        = cbgs.stream()
          .collect(Collectors.groupingBy(
              CheckBoxGroup::getCourse,
              Collectors.mapping(
                  CheckBoxGroup::getModule, Collectors.toList())));


    final CourseSelection selectedCourses
        = new CourseSelection(courseSelection.getSelectedCourses().toArray(new Course[0]));
    final PdfRenderingTask task
        = pdfRenderingService.getTask(selectedCourses, moduleChoice, unitChoice);
    wireUpTask(task);
    pdfRenderingService.submit(task);
  }

  private void wireUpTask(final PdfRenderingTask task) {
    currentTaskProperty.set(task);
    taskProgressIndicator.taskProperty().set(task);

    task.setOnSucceeded(event -> {
      pdf.set((Path) event.getSource().getValue());
      checkRunning.set(false);
      selectionChanged.set(false);
    });

    task.setOnFailed(event -> {
      pdf.set(null);
      checkRunning.set(false);
      selectionChanged.set(false);
    });

    task.setOnCancelled(event -> {
      checkRunning.set(false);
      selectionChanged.set(false);
    });
  }

  @SuppressWarnings("unused")
  @FXML
  private void showPdf() {
    PdfRenderingHelper.showPdf(pdf.get());
  }

  @SuppressWarnings("unused")
  @FXML
  private void savePdf() {
    final Course major = courseSelection.getSelectedMajor();
    final Course minor = courseSelection.getSelectedMinor();

    PdfRenderingHelper.savePdf(pdf.get(), major, minor, null);
  }

  /**
   * Select the given courses within the {@link #courseSelection} when the user navigates to the
   * view via the {@link de.hhu.stups.plues.routes.ControllerRoute}.
   */
  @Override
  public void activateController(final RouteNames routeName, final Object... courses) {
    if (courses.length > 0) {
      courseSelection.selectCourse((Course) courses[0]);
    }
    if (courses.length > 1) {
      courseSelection.selectCourse((Course) courses[1]);
    }
    if (courses.length == 2) {
      btChoosePressed();
    }
  }
}
