package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.controller.PdfRenderingHelper;
import de.hhu.stups.plues.ui.layout.Inflater;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nullable;

public class FeasibilityBox extends GridPane implements Initializable {

  private static final String WORKING_COLOR = "#BDE5F8";

  private String remove;
  private String highlight;
  private String cancel;

  private final ObjectProperty<Course> majorCourse;
  private final ObjectProperty<Course> minorCourse;
  private SolverTask<Boolean> task;
  private final ExecutorService executorService;
  private final Delayed<SolverService> delayedSolverService;
  private final VBox parent;

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
  private ComboBox<String> cbAction;
  @FXML
  @SuppressWarnings("unused")
  private Button btSubmit;

  /**
   * A container to present a computed feasibility result for a given major and/or minor course like
   * a {@link ResultBox} but with slightly different behavior.
   */
  @Inject
  public FeasibilityBox(final Inflater inflater,
                        final Delayed<SolverService> delayedSolverService,
                        final ExecutorService executorService,
                        @Assisted("major") final Course major,
                        @Nullable @Assisted("minor") final Course minor,
                        @Assisted("parent") final VBox parent) {
    super();
    this.delayedSolverService = delayedSolverService;
    this.executorService = executorService;
    this.parent = parent;

    majorCourse = new SimpleObjectProperty<>(major);
    minorCourse = new SimpleObjectProperty<>(minor);

    setHgap(10.0);

    inflater.inflate("components/resultbox", this, this, "resultbox");
  }

  @Override
  public final void initialize(final URL location, final ResourceBundle resources) {
    remove = resources.getString("remove");
    highlight = resources.getString("highlight");
    cancel = resources.getString("cancel");

    major.textProperty()
        .bind(Bindings.selectString(majorCourse, "fullName"));
    minor.textProperty()
        .bind(Bindings.selectString(minorCourse, "fullName"));

    delayedSolverService.whenAvailable(solver -> {
      final Course cMajor = majorCourse.get();
      final Course cMinor = minorCourse.get();
      if (cMinor != null) {
        task = solver.checkFeasibilityTask(cMajor, cMinor);
      } else {
        task = solver.checkFeasibilityTask(cMajor);
      }

      progressIndicator.setStyle("-fx-progress-color: " + WORKING_COLOR);
      progressIndicator.visibleProperty().bind(task.runningProperty());

      executorService.submit(task);
    });

    final String bgColorCommand = "-fx-background-color:";
    task.setOnSucceeded(event -> Platform.runLater(() -> {
      cbAction.setItems(task.getValue()
          ? FXCollections.observableList(Arrays.asList(highlight, remove))
          : FXCollections.observableList(Collections.singletonList(remove)));
      cbAction.getSelectionModel().selectFirst();
      icon.setGraphic(FontAwesomeIconFactory.get().createIcon(task.getValue()
          ? FontAwesomeIcon.CHECK : FontAwesomeIcon.REMOVE, "50"));
      icon.setStyle(bgColorCommand + (task.getValue()
          ? PdfRenderingHelper.SUCCESS_COLOR : PdfRenderingHelper.FAILURE_COLOR));
    }));

    task.setOnFailed(event -> {
      cbAction.setItems(FXCollections.observableList(Collections.singletonList(remove)));
      cbAction.getSelectionModel().selectFirst();
      icon.setGraphic(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.REMOVE, "50"));
      icon.setStyle(bgColorCommand + PdfRenderingHelper.FAILURE_COLOR);
    });

    task.setOnCancelled(event -> {
      icon.setGraphic(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.QUESTION, "50"));
      icon.setStyle(bgColorCommand + PdfRenderingHelper.WARNING_COLOR);
    });

    progressIndicator.setStyle("-fx-progress-color: " + WORKING_COLOR);
    progressIndicator.visibleProperty().bind(task.runningProperty());

    cbAction.setItems(FXCollections.observableList(Collections.singletonList(cancel)));
    cbAction.getSelectionModel().selectFirst();
  }

  @FXML
  @SuppressWarnings("unused")
  private void submitAction() {
    final String selectedItem = cbAction.getSelectionModel().getSelectedItem();

    if (selectedItem.equals(highlight)) {
      // Todo: do something
    }
    if (selectedItem.equals(remove)) {
      parent.getChildren().remove(this);
    }
    if (selectedItem.equals(cancel)) {
      interrupt();
      cbAction.setItems(FXCollections.observableList(Collections.singletonList(remove)));
      cbAction.getSelectionModel().selectFirst();
    }
  }

  @FXML
  private void interrupt() {
    task.cancel();
  }
}