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
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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
    initializeCourseUnsatCore();
    initializeModuleUnsatCore();
    initializeAbstractUnitUnsatCore();
    initializeGroupUnsatCore();
  }

  private void initializeCourseUnsatCore() {
    courseUnsatCore.coursesProperty().addListener((observable, oldValue, newValue) -> {
      moduleUnsatCore.setModules(FXCollections.emptyObservableList());
      moduleUnsatCore.resetTaskState();
    });

    final BooleanBinding binding = solverService.isNull()
        .or(courseUnsatCore.coursesProperty().emptyProperty())
        .or(moduleUnsatCore.getModuleProperty().emptyProperty().not());

    final UnsatCoreButtonBar unsatCoreButtonBar = courseUnsatCore.getUnsatCoreButtonBar();
    unsatCoreButtonBar.disableProperty().bind(binding);
    unsatCoreButtonBar.setOnAction(this::computeUnsatCoreModules);
  }

  private void initializeModuleUnsatCore() {
    moduleUnsatCore.getModuleProperty().addListener((observable, oldValue, newValue) -> {
      abstractUnitUnsatCore.setAbstractUnits(FXCollections.emptyObservableList());
      abstractUnitUnsatCore.resetTaskState();
    });


    final BooleanBinding binding = moduleUnsatCore.getModuleProperty().emptyProperty()
        .or(abstractUnitUnsatCore.getAbstractUnits().emptyProperty().not());

    final UnsatCoreButtonBar unsatCoreButtonBar = moduleUnsatCore.getUnsatCoreButtonBar();
    unsatCoreButtonBar.disableProperty().bind(binding);
    unsatCoreButtonBar.setOnAction(this::computeUnsatCoreAbstractUnits);
  }

  private void initializeAbstractUnitUnsatCore() {
    abstractUnitUnsatCore.getAbstractUnits().addListener((observable, oldValue, newValue) -> {
      groupUnsatCore.setGroups(FXCollections.emptyObservableList());
      groupUnsatCore.resetTaskState();
    });

    final BooleanBinding binding = abstractUnitUnsatCore.getAbstractUnits().emptyProperty()
        .or(groupUnsatCore.getGroupProperty().emptyProperty().not());

    final UnsatCoreButtonBar unsatCoreButtonBar = abstractUnitUnsatCore.getUnsatCoreButtonBar();
    unsatCoreButtonBar.disableProperty().bind(binding);
    unsatCoreButtonBar.setOnAction(this::computeUnsatCoreGroups);
  }

  private void initializeGroupUnsatCore() {
    groupUnsatCore.getGroupProperty().addListener((observable, oldValue, newValue) ->
        sessionUnsatCore.setSessions(FXCollections.emptyObservableList()));


    final BooleanBinding binding = groupUnsatCore.getGroupProperty().emptyProperty()
        .or(sessionUnsatCore.getSessionProperty().emptyProperty().not());

    final UnsatCoreButtonBar unsatCoreButtonBar = groupUnsatCore.getUnsatCoreButtonBar();
    unsatCoreButtonBar.disableProperty().bind(binding);
    unsatCoreButtonBar.setOnAction(this::computeUnsatCoreSessions);
  }

  @SuppressWarnings("unused")
  private void computeUnsatCoreGroups(final ActionEvent actionEvent) {
    final SolverTask<Set<Integer>> task
        = getSolverService().unsatCoreGroups(
          abstractUnitUnsatCore.getAbstractUnits().get(),
          moduleUnsatCore.getModuleProperty().get());

    task.setOnSucceeded(succeeded -> {
      final Set<Integer> groupIds = task.getValue();
      groupUnsatCore.setGroups(groupIds.stream().map(getStore()::getGroupById).collect(Collectors
          .collectingAndThen(Collectors.toList(), FXCollections::observableArrayList)));
    });

    abstractUnitUnsatCore.getUnsatCoreButtonBar().showTaskState(task);
    executorService.submit(task);
  }

  @SuppressWarnings("unused")
  private void computeUnsatCoreModules(final ActionEvent event) {
    final ObservableList<Course> courseList = courseUnsatCore.coursesProperty().get();
    final Course[] selectedCourses = new Course[courseList.size()];
    final SolverTask<Set<Integer>> task
        = getSolverService().unsatCoreModules(courseList.toArray(selectedCourses));

    task.setOnSucceeded(succeeded -> {
      final Set<Integer> moduleIds = task.getValue();
      moduleUnsatCore.setModules(moduleIds.stream().map(getStore()::getModuleById)
          .collect(
            Collectors.collectingAndThen(Collectors.toList(), FXCollections::observableArrayList)));
    });

    courseUnsatCore.getUnsatCoreButtonBar().showTaskState(task);

    executorService.submit(task);
  }

  @SuppressWarnings("unused")
  private void computeUnsatCoreAbstractUnits(final ActionEvent actionEvent) {
    final SolverTask<Set<Integer>> task
        = getSolverService().unsatCoreAbstractUnits(moduleUnsatCore.getModuleProperty().get());

    task.setOnSucceeded(succeeded -> {
      final Set<Integer> abstractUnitIds = task.getValue();
      abstractUnitUnsatCore.setAbstractUnits(abstractUnitIds.stream()
          .map(getStore()::getAbstractUnitById).collect(Collectors
            .collectingAndThen(Collectors.toList(), FXCollections::observableArrayList)));

    });

    moduleUnsatCore.getUnsatCoreButtonBar().showTaskState(task);
    executorService.submit(task);
  }

  @SuppressWarnings("unused")
  private void computeUnsatCoreSessions(final ActionEvent actionEvent) {
    final SolverTask<Set<Integer>> task
        = getSolverService().unsatCoreSessions(groupUnsatCore.getGroupProperty().get());

    task.setOnSucceeded(succeeded -> {
      final Set<Integer> sessionIds = task.getValue();
      sessionUnsatCore.setSessions(sessionIds.stream().map(getStore()::getSessionById).collect(
          Collectors.collectingAndThen(Collectors.toList(), FXCollections::observableArrayList)));
    });

    groupUnsatCore.getUnsatCoreButtonBar().showTaskState(task);
    executorService.submit(task);
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
  public void activateController(final Object... args) {
    courseUnsatCore.selectCourses((Course[]) args);
    computeUnsatCoreModules(null);
  }
}
