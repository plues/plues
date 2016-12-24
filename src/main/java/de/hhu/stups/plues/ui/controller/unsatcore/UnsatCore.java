package de.hhu.stups.plues.ui.controller.unsatcore;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.controller.Activatable;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public class UnsatCore extends VBox implements Initializable, Activatable {

  private final ObjectProperty<SolverService> solverService;
  private final ObjectProperty<Store> store;
  private final ExecutorService executorService;
  private ResourceBundle resources;

  @FXML
  private CourseUnsatCore courseUnsatCore;
  @FXML
  private ModuleUnsatCore moduleUnsatCore;
  @FXML
  private AbstractUnitUnsatCore abstractUnitUnsatCore;
  @FXML
  private GroupUnsatCore groupUnsatCore;
  @FXML
  private SessionUnsatCore sessionUnsatCore;

  /**
   * Default constructor.
   */
  @Inject
  public UnsatCore(final Inflater inflater,
                   final Delayed<SolverService> delayedSolverService,
                   final Delayed<Store> delayedStore,
                   final ExecutorService executorService) {
    solverService = new SimpleObjectProperty<>();
    delayedSolverService.whenAvailable(this.solverService::set);

    store = new SimpleObjectProperty<>();
    delayedStore.whenAvailable(this.store::set);

    this.executorService = executorService;

    inflater.inflate("components/unsatcore/UnsatCore", this, this, "unsatCore");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    this.resources = resources;

    configureCourseUnsatCore(resources);
    configureModuleUnsatCore(resources);
    configureAbstractUnitUnsatCore(resources);
    configureGroupUnsatCore(resources);
  }

  private void configureCourseUnsatCore(final ResourceBundle resources) {
    final EventHandler<ActionEvent> eventHandler = event -> {
      final ObservableList<Course> courseList = courseUnsatCore.coursesProperty().get();
      final Course[] selectedCourses = new Course[courseList.size()];
      final SolverTask<Set<Integer>> task =
          getSolverService().unsatCoreModules(courseList.toArray(selectedCourses));

      task.setOnSucceeded(succeeded -> {
        final Set<Integer> moduleIds = task.getValue();
        moduleUnsatCore.setModules(moduleIds.stream().map(getStore()::getModuleById)
            .collect(Collectors.collectingAndThen(Collectors.toList(),
              FXCollections::observableArrayList)));
      });

      courseUnsatCore.showTaskState(task, resources);

      executorService.submit(task);
    };

    courseUnsatCore.coursesProperty().addListener((observable, oldValue, newValue) -> {
      moduleUnsatCore.setModules(FXCollections.emptyObservableList());
      moduleUnsatCore.resetTaskState();
    });

    final BooleanBinding binding = solverService.isNull()
        .or(courseUnsatCore.coursesProperty().emptyProperty())
        .or(moduleUnsatCore.getModuleProperty().emptyProperty().not());

    courseUnsatCore.configureButton(binding, eventHandler);
  }

  private void configureModuleUnsatCore(final ResourceBundle resources) {
    final EventHandler<ActionEvent> eventHandler = event -> {
      final SolverTask<Set<Integer>> task =
          getSolverService().unsatCoreAbstractUnits(moduleUnsatCore.getModuleProperty().get());

      task.setOnSucceeded(succeeded -> {
        final Set<Integer> abstractUnitIds = task.getValue();
        abstractUnitUnsatCore.setAbstractUnits(abstractUnitIds.stream()
            .map(getStore()::getAbstractUnitById).collect(Collectors
              .collectingAndThen(Collectors.toList(), FXCollections::observableArrayList)));

      });

      moduleUnsatCore.showTaskState(task, resources);
      executorService.submit(task);
    };

    moduleUnsatCore.getModuleProperty().addListener((observable, oldValue, newValue) -> {
      abstractUnitUnsatCore.setAbstractUnits(FXCollections.emptyObservableList());
      abstractUnitUnsatCore.resetTaskState();
    });


    final BooleanBinding binding = moduleUnsatCore.getModuleProperty().emptyProperty()
        .or(abstractUnitUnsatCore.getAbstractUnits().emptyProperty().not());

    moduleUnsatCore.configureButton(binding, eventHandler);
  }

  private void configureAbstractUnitUnsatCore(final ResourceBundle resources) {
    final EventHandler<ActionEvent> eventHandler = event -> {
      final SolverTask<Set<Integer>> task =
          getSolverService().unsatCoreGroups(abstractUnitUnsatCore.getAbstractUnits().get(),
              moduleUnsatCore.getModuleProperty().get());

      task.setOnSucceeded(succeeded -> {
        final Set<Integer> groupIds = task.getValue();
        groupUnsatCore.setGroups(groupIds.stream().map(getStore()::getGroupById).collect(Collectors
            .collectingAndThen(Collectors.toList(), FXCollections::observableArrayList)));
      });

      abstractUnitUnsatCore.showTaskState(task, resources);
      executorService.submit(task);
    };

    abstractUnitUnsatCore.getAbstractUnits().addListener((observable, oldValue, newValue) -> {
      groupUnsatCore.setGroups(FXCollections.emptyObservableList());
      groupUnsatCore.resetTaskState();
    });


    final BooleanBinding binding = abstractUnitUnsatCore.getAbstractUnits().emptyProperty()
        .or(groupUnsatCore.getGroupProperty().emptyProperty().not());

    abstractUnitUnsatCore.configureButton(binding, eventHandler);
  }

  private void configureGroupUnsatCore(final ResourceBundle resources) {
    final EventHandler<ActionEvent> eventHandler = event -> {
      final SolverTask<Set<Integer>> task = getSolverService().unsatCoreSessions(
          groupUnsatCore.getGroupProperty().get());

      task.setOnSucceeded(succeeded -> {
        final Set<Integer> sessionIds = task.getValue();
        sessionUnsatCore.setSessions(sessionIds.stream().map(getStore()::getSessionById).collect(
            Collectors.collectingAndThen(Collectors.toList(), FXCollections::observableArrayList)));
      });

      groupUnsatCore.showTaskState(task, resources);
      executorService.submit(task);
    };

    groupUnsatCore.getGroupProperty().addListener((observable, oldValue, newValue) ->
        sessionUnsatCore.setSessions(FXCollections.emptyObservableList()));


    final BooleanBinding binding = groupUnsatCore.getGroupProperty().emptyProperty()
        .or(sessionUnsatCore.getSessionProperty().emptyProperty().not());

    groupUnsatCore.configureButton(binding, eventHandler);
  }

  public SolverService getSolverService() {
    return this.solverService.get();
  }

  public Store getStore() {
    return this.store.get();
  }

  /**
   * Select the given courses within the {@link #courseUnsatCore} when the user navigates to the
   * view via the {@link de.hhu.stups.plues.routes.ControllerRoute}.
   */
  @Override
  public void activateController(final Object... courses) {
    courseUnsatCore.selectCourses((Course[]) courses);
    configureAbstractUnitUnsatCore(resources);
  }
}
