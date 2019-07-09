package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.components.ControllerHeader;
import de.hhu.stups.plues.ui.components.unsatcore.AbstractUnitUnsatCore;
import de.hhu.stups.plues.ui.components.unsatcore.CourseUnsatCore;
import de.hhu.stups.plues.ui.components.unsatcore.GroupUnsatCore;
import de.hhu.stups.plues.ui.components.unsatcore.ModuleUnsatCore;
import de.hhu.stups.plues.ui.components.unsatcore.SessionUnsatCore;
import de.hhu.stups.plues.ui.components.unsatcore.UnsatCoreButtonBar;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;

import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class UnsatCore extends GridPane implements Activatable {

  private final ObjectProperty<SolverService> solverService;
  private final ObjectProperty<Store> store;
  private final ExecutorService executorService;

  @FXML
  private ResourceBundle resources;

  @FXML
  @SuppressWarnings("unused")
  private ControllerHeader controllerHeader;
  @FXML
  @SuppressWarnings("unused")
  private Accordion stepwisePanesAccordion;
  @FXML
  @SuppressWarnings("unused")
  private TitledPane modulesPane;
  @FXML
  @SuppressWarnings("unused")
  private TitledPane abstractUnitsPane;
  @FXML
  @SuppressWarnings("unused")
  private TitledPane groupPane;
  @FXML
  @SuppressWarnings("unused")
  private TitledPane sessionPane;
  @FXML
  @SuppressWarnings("unused")
  private CourseUnsatCore courseUnsatCore;
  @FXML
  @SuppressWarnings("unused")
  private ModuleUnsatCore moduleUnsatCore;
  @FXML
  @SuppressWarnings("unused")
  private AbstractUnitUnsatCore abstractUnitUnsatCore;
  @FXML
  @SuppressWarnings("unused")
  private GroupUnsatCore groupUnsatCore;
  @FXML
  @SuppressWarnings("unused")
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

  @FXML
  public void initialize() {
    initializeCourseUnsatCore();
    initializeModuleUnsatCore();
    initializeAbstractUnitUnsatCore();
    initializeGroupUnsatCore();
    initializeControllerHeader();

    modulesPane.visibleProperty().bind(moduleUnsatCore.moduleProperty().emptyProperty().not());
    abstractUnitsPane.visibleProperty().bind(
        abstractUnitUnsatCore.abstractUnitsProperty().emptyProperty().not());
    groupPane.visibleProperty().bind(groupUnsatCore.groupProperty().emptyProperty().not());
    sessionPane.visibleProperty().bind(sessionUnsatCore.sessionProperty().emptyProperty().not());
    sessionUnsatCore.coursesProperty().bind(courseUnsatCore.coursesProperty());
  }

  private void initializeControllerHeader() {
    controllerHeader.setTitle(resources.getString("unsatCore"));
    controllerHeader.setInfoText(resources.getString("unsatCoreExplanation"));
  }

  private void initializeCourseUnsatCore() {
    courseUnsatCore.coursesProperty().addListener((observable, oldValue, newValue) ->
        resetModuleUnsatCore());

    courseUnsatCore.courseIsInfeasibleProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        final BooleanBinding binding = solverService.isNull()
            .or(courseUnsatCore.coursesProperty().emptyProperty())
            .or(moduleUnsatCore.moduleProperty().emptyProperty().not())
            .or(courseUnsatCore.courseIsInfeasibleProperty().not());
        final UnsatCoreButtonBar unsatCoreButtonBar = courseUnsatCore.getUnsatCoreButtonBar();
        unsatCoreButtonBar.disableProperty().bind(binding);
        unsatCoreButtonBar.setOnAction(event -> computeUnsatCoreModules());
      }
    });
  }

  private void resetModuleUnsatCore() {
    moduleUnsatCore.setModules(FXCollections.emptyObservableList());
    moduleUnsatCore.resetTaskState();
  }

  private void initializeModuleUnsatCore() {
    moduleUnsatCore.moduleProperty().addListener((observable, oldValue, newValue) -> {
      abstractUnitUnsatCore.setAbstractUnits(FXCollections.emptyObservableList());
      abstractUnitUnsatCore.resetTaskState();
    });


    final BooleanBinding binding = moduleUnsatCore.moduleProperty().emptyProperty()
        .or(abstractUnitUnsatCore.abstractUnitsProperty().emptyProperty().not());

    final UnsatCoreButtonBar unsatCoreButtonBar = moduleUnsatCore.getUnsatCoreButtonBar();
    unsatCoreButtonBar.disableProperty().bind(binding);
    unsatCoreButtonBar.setOnAction(this::computeUnsatCoreAbstractUnits);
  }

  private void initializeAbstractUnitUnsatCore() {
    abstractUnitUnsatCore.abstractUnitsProperty().addListener((observable, oldValue, newValue) -> {
      groupUnsatCore.setGroups(FXCollections.emptyObservableList());
      groupUnsatCore.resetTaskState();
    });
    abstractUnitUnsatCore.modulesProperty().bind(moduleUnsatCore.moduleProperty());

    final BooleanBinding binding = abstractUnitUnsatCore.abstractUnitsProperty().emptyProperty()
        .or(groupUnsatCore.groupProperty().emptyProperty().not());

    final UnsatCoreButtonBar unsatCoreButtonBar = abstractUnitUnsatCore.getUnsatCoreButtonBar();
    unsatCoreButtonBar.disableProperty().bind(binding);
    unsatCoreButtonBar.setOnAction(this::computeUnsatCoreGroups);
  }

  private void initializeGroupUnsatCore() {
    groupUnsatCore.groupProperty().addListener((observable, oldValue, newValue) ->
        sessionUnsatCore.setSessions(FXCollections.emptyObservableList()));
    groupUnsatCore.abstractUnitsProperty().bind(abstractUnitUnsatCore.abstractUnitsProperty());

    final BooleanBinding binding = groupUnsatCore.groupProperty().emptyProperty()
        .or(sessionUnsatCore.sessionProperty().emptyProperty().not());

    final UnsatCoreButtonBar unsatCoreButtonBar = groupUnsatCore.getUnsatCoreButtonBar();
    unsatCoreButtonBar.disableProperty().bind(binding);
    unsatCoreButtonBar.setOnAction(this::computeUnsatCoreSessions);
  }

  @SuppressWarnings("unused")
  private void computeUnsatCoreModules() {
    final ObservableList<Course> courseList = courseUnsatCore.coursesProperty().get();
    final SolverTask<Set<Integer>> task
        = getSolverService().unsatCoreModules(courseList.toArray(new Course[0]));

    task.setOnSucceeded(succeeded -> {
      final Set<Integer> moduleIds = task.getValue();
      moduleUnsatCore.setCourses(courseList);
      moduleUnsatCore.setModules(moduleIds.stream().map(getStore()::getModuleById)
          .collect(Collectors.collectingAndThen(Collectors.toList(),
              FXCollections::observableArrayList)));
      stepwisePanesAccordion.setExpandedPane(modulesPane);
    });

    courseUnsatCore.registerTask(task);

    courseUnsatCore.getUnsatCoreButtonBar().taskProperty().set(task);
    executorService.submit(task);
  }

  @SuppressWarnings("unused")
  private void computeUnsatCoreAbstractUnits(final ActionEvent actionEvent) {
    final SolverTask<Set<Integer>> task
        = getSolverService().unsatCoreAbstractUnits(moduleUnsatCore.getModules());

    task.setOnSucceeded(succeeded -> {
      final Set<Integer> abstractUnitIds = task.getValue();
      abstractUnitUnsatCore.setAbstractUnits(abstractUnitIds.stream()
          .map(getStore()::getAbstractUnitById).collect(Collectors
              .collectingAndThen(Collectors.toList(), FXCollections::observableArrayList)));
      stepwisePanesAccordion.setExpandedPane(abstractUnitsPane);
    });

    courseUnsatCore.registerTask(task);

    moduleUnsatCore.getUnsatCoreButtonBar().taskProperty().set(task);
    executorService.submit(task);
  }

  @SuppressWarnings("unused")
  private void computeUnsatCoreGroups(final ActionEvent actionEvent) {
    final SolverTask<Set<Integer>> task
        = getSolverService().unsatCoreGroups(abstractUnitUnsatCore.getAbstractUnits(),
        moduleUnsatCore.getModules());

    task.setOnSucceeded(succeeded -> {
      final Set<Integer> groupIds = task.getValue();
      groupUnsatCore.setGroups(groupIds.stream().map(getStore()::getGroupById).collect(Collectors
          .collectingAndThen(Collectors.toList(), FXCollections::observableArrayList)));
      stepwisePanesAccordion.setExpandedPane(groupPane);
    });

    courseUnsatCore.registerTask(task);

    abstractUnitUnsatCore.getUnsatCoreButtonBar().taskProperty().set(task);
    executorService.submit(task);
  }

  @SuppressWarnings("unused")
  private void computeUnsatCoreSessions(final ActionEvent actionEvent) {
    final SolverTask<Set<Integer>> task
        = getSolverService().unsatCoreSessions(groupUnsatCore.groupProperty().get());

    task.setOnSucceeded(succeeded -> {
      final Set<Integer> sessionIds = task.getValue();
      sessionUnsatCore.setSessions(sessionIds.stream().map(getStore()::getSessionById).collect(
          Collectors.collectingAndThen(Collectors.toList(), FXCollections::observableArrayList)));
      stepwisePanesAccordion.setExpandedPane(sessionPane);
    });

    courseUnsatCore.registerTask(task);

    groupUnsatCore.getUnsatCoreButtonBar().taskProperty().set(task);
    executorService.submit(task);
  }

  public SolverService getSolverService() {
    return this.solverService.get();
  }

  public Store getStore() {
    return this.store.get();
  }

  /**
   * Assuming the given courses are infeasible, otherwise it should not even be possible to navigate
   * to this controller. Select the courses within the {@link #courseUnsatCore} when the user
   * navigates to the view via the {@link de.hhu.stups.plues.routes.ControllerRoute}. Furthermore,
   * set the {@link CourseUnsatCore#courseIsInfeasibleProperty()} true since we know the course is
   * infeasible.
   */
  @Override
  public void activateController(final RouteNames routeName, final Object... args) {
    resetModuleUnsatCore();
    courseUnsatCore.selectCourses(getCoursesFromArray(args));
    courseUnsatCore.courseIsInfeasibleProperty().set(true);
    computeUnsatCoreModules();
  }

  private Course[] getCoursesFromArray(final Object[] args) {
    final Course[] courseArray = new Course[args.length];
    IntStream.range(0, args.length).forEach(index -> courseArray[index] = (Course) args[index]);
    return courseArray;
  }
}
