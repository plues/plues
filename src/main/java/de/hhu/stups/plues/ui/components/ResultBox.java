package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.hhu.stups.plues.tasks.PdfRenderingTaskFactory;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.controller.PdfRenderingHelper;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nullable;

public class ResultBox extends GridPane implements Initializable {

  private static final String WORKING_COLOR = "#BDE5F8";

  private ResourceBundle resources;
  private String remove;
  private String show;
  private String save;
  private String cancel;

  private final ObjectProperty<Course> majorCourse;
  private final ObjectProperty<Course> minorCourse;
  private PdfRenderingTask task;
  private final ExecutorService executor;
  private final Delayed<SolverService> solverService;
  private final PdfRenderingTaskFactory renderingTaskFactory;
  private final VBox parent;

  private final ObjectProperty<Path> pdf;

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
  private Label major;

  @FXML
  @SuppressWarnings("unused")
  private Label minor;

  @FXML
  @SuppressWarnings("unused")
  private Label lbErrorMsg;

  @FXML
  @SuppressWarnings("unused")
  private ComboBox<String> cbAction;

  @FXML
  @SuppressWarnings("unused")
  private Button btSubmit;

  /**
   * Constructor for ResultBox.
   *
   * @param inflater             Inflater to handle fxml loader tasks
   * @param renderingTaskFactory PDF Rendering task Factory
   * @param major                Major course
   * @param minor                Minor course if present, else null
   * @param parent               The parent wrapper (VBox) to remove a single result box.
   */
  @Inject
  public ResultBox(final Inflater inflater,
                   final Delayed<SolverService> delayedSolverService,
                   final PdfRenderingTaskFactory renderingTaskFactory,
                   final ExecutorService executorService,
                   @Assisted("major") final Course major,
                   @Nullable @Assisted("minor") final Course minor,
                   @Assisted("parent") final VBox parent) {
    super();
    this.solverService = delayedSolverService;
    this.renderingTaskFactory = renderingTaskFactory;
    this.majorCourse = new SimpleObjectProperty<>(major);
    this.minorCourse = new SimpleObjectProperty<>(minor);
    this.pdf = new SimpleObjectProperty<>();
    this.executor = executorService;
    this.parent = parent;
    this.setHgap(10.0);

    inflater.inflate("components/resultbox", this, this, "resultbox");
  }

  @Override
  public final void initialize(final URL location,
                               final ResourceBundle resources) {
    this.resources = resources;
    remove = resources.getString("remove");
    show = resources.getString("show");
    save = resources.getString("save");
    cancel = resources.getString("cancel");
    //
    this.major.textProperty()
        .bind(Bindings.selectString(this.majorCourse, "fullName"));
    this.minor.textProperty()
        .bind(Bindings.selectString(this.minorCourse, "fullName"));
    //
    solverService.whenAvailable(solver -> {
      final SolverTask<FeasibilityResult> solverTask;
      final Course cMajor = majorCourse.get();
      final Course cMinor = minorCourse.get();
      if (cMinor != null) {
        solverTask = solver.computeFeasibilityTask(cMajor, cMinor);
      } else {
        solverTask = solver.computeFeasibilityTask(cMajor);
      }
      task = renderingTaskFactory.create(cMajor, cMinor, solverTask);
      //
      this.progressIndicator.setStyle(" -fx-progress-color: " + WORKING_COLOR);
      this.progressIndicator.visibleProperty()
          .bind(task.runningProperty());
      //
      icon.graphicProperty().bind(PdfRenderingHelper.getIconBinding(task));
      icon.styleProperty().bind(PdfRenderingHelper.getStyleBinding(task));
      //
      executor.submit(task);
    });
    this.lbErrorMsg.visibleProperty().bind(this.pdf.isNull());

    task.setOnSucceeded(event -> Platform.runLater(() -> {
      pdf.set((Path) event.getSource().getValue());
      cbAction.setItems(FXCollections.observableList(Arrays.asList(show, save, remove)));
      cbAction.getSelectionModel().selectFirst();
    }));

    task.setOnFailed(event -> {
      this.cbAction.setItems(FXCollections.observableList(Collections.singletonList(remove)));
      this.cbAction.getSelectionModel().selectFirst();
      this.lbErrorMsg.setText(resources.getString("error_gen"));
    });
    //
    this.progressIndicator.setStyle(" -fx-progress-color: " + WORKING_COLOR);
    this.progressIndicator.visibleProperty()
        .bind(task.runningProperty());

    this.cbAction.setItems(FXCollections.observableList(Collections.singletonList(cancel)));
    this.cbAction.getSelectionModel().selectFirst();
  }

  @FXML
  @SuppressWarnings("unused")
  private void submitAction() {
    final String selectedItem = cbAction.getSelectionModel().getSelectedItem();

    if (selectedItem.equals(show)) {
      showPdf();
    }
    if (selectedItem.equals(save)) {
      savePdf();
    }
    if (selectedItem.equals(remove)) {
      this.parent.getChildren().remove(this);
    }
    if (selectedItem.equals(cancel)) {
      this.interrupt();
      this.cbAction.setItems(FXCollections.observableList(Collections.singletonList(remove)));
      this.cbAction.getSelectionModel().selectFirst();
    }
  }

  @FXML
  private void showPdf() {
    PdfRenderingHelper.showPdf(pdf.get(),
        e -> lbErrorMsg.setText(resources.getString("error_temp")));
  }

  @FXML
  private void savePdf() {
    PdfRenderingHelper.savePdf(pdf.get(), majorCourse.get(), minorCourse.get(), lbErrorMsg);
  }

  @FXML
  private void interrupt() {
    this.task.cancel();
  }
}
