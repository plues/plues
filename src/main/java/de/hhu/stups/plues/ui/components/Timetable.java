package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.controller.MainController;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.stream.IntStream;

public class Timetable extends BorderPane implements Initializable {

  private final Delayed<Store> delayedStore;
  private final Delayed<SolverService> delayedSolverService;
  private final MainController mainController;

  private final ObjectProperty<Course>
      courseProperty = new SimpleObjectProperty<>();
  private final BooleanProperty
      solverProperty = new SimpleBooleanProperty(false);

  @FXML
  private GridPane foo;
  @FXML
  private Label selection;
  @FXML
  private Button checkSelection;
  @FXML
  private Label result;
  @FXML
  private CourseFilter courseFilter;

  private SolverService solverService;


  /**
   * Timetable component.
   */
  @Inject
  public Timetable(final Inflater inflater, final Delayed<Store> delayedStore,
                          final Delayed<SolverService> delayedSolverService,
                          final MainController mainController) {
    this.delayedStore = delayedStore;
    this.delayedSolverService = delayedSolverService;
    this.mainController = mainController;

    // TODO: remove controller param if possible
    // TODO: currently not possible because of dependency circle
    inflater.inflate("components/Timetable", this, this);
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    this.delayedStore.whenAvailable(s -> {
      Runtime.getRuntime().addShutdownHook(new Thread(s::close));
      System.out.println("Store Loaded " + s);
      this.courseFilter.setCourses(s.getCourses());
    });

    this.courseProperty.bind(this.courseFilter.selectedItemProperty());
    this.selection.textProperty().bind(
        Bindings.selectString(this.courseProperty, "name"));

    this.checkSelection.setDefaultButton(true);
    this.checkSelection.disableProperty().bind(
        this.courseProperty.isNull().or(this.solverProperty.not()));

    this.delayedSolverService.whenAvailable(s -> {
      this.solverService = s;
      this.solverProperty.set(true);
      System.out.println("SolverService loaded");
    });

    IntStream.range(1, 20).forEach(x -> this.foo.add(
        new Label(String.valueOf(x)),
        x % this.foo.getColumnConstraints().size(),
        x % this.foo.getRowConstraints().size()));
  }

  @FXML
  @SuppressWarnings({"UnusedParameters", "unused"})
  private void checkButtonPressed(final ActionEvent actionEvent) {
    final Course course = this.courseProperty.get();

    final SolverService s = this.solverService;
    assert s != null;

    final SolverTask<Boolean> t = s.checkFeasibilityTask(course);
    t.setOnSucceeded(event -> {
      final Boolean i = (Boolean) event.getSource().getValue();
      this.result.setText(i.toString());
      System.out.println(course.getName() + ": " + i.toString());
    });
    s.submit(t);
  }
}
