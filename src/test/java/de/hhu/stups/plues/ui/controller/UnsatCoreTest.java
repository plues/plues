package de.hhu.stups.plues.ui.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testfx.api.FxToolkit.setupStage;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Group;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.ui.UiTestDataCreator;
import de.hhu.stups.plues.ui.components.CombinationOrSingleCourseSelection;
import de.hhu.stups.plues.ui.components.ControllerHeader;
import de.hhu.stups.plues.ui.components.MajorMinorCourseSelection;
import de.hhu.stups.plues.ui.components.TaskProgressIndicator;
import de.hhu.stups.plues.ui.components.unsatcore.AbstractUnitUnsatCore;
import de.hhu.stups.plues.ui.components.unsatcore.CourseUnsatCore;
import de.hhu.stups.plues.ui.components.unsatcore.GroupUnsatCore;
import de.hhu.stups.plues.ui.components.unsatcore.ModuleUnsatCore;
import de.hhu.stups.plues.ui.components.unsatcore.SessionUnsatCore;
import de.hhu.stups.plues.ui.components.unsatcore.UnsatCoreButtonBar;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Scene;
import javafx.scene.control.TitledPane;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class UnsatCoreTest extends ApplicationTest {

  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  private final Store store;
  private final ObservableList<Module> modules = FXCollections.observableArrayList(new Module());
  private final ObservableList<AbstractUnit> abstractUnits =
      FXCollections.observableArrayList(new AbstractUnit());
  private final ObservableList<Group> groups = FXCollections.observableArrayList(new Group());
  private final ObservableList<Session> sessions = FXCollections.observableArrayList(new Session());
  private final ObservableList<Course> courseList = UiTestDataCreator.createCourseList();

  private CombinationOrSingleCourseSelection courseSelection;

  private CourseUnsatCore courseUnsatCore;
  private ModuleUnsatCore moduleUnsatCore;
  private AbstractUnitUnsatCore abstractUnitUnsatCore;
  private GroupUnsatCore groupUnsatCore;
  private SessionUnsatCore sessionUnsatCore;

  public UnsatCoreTest() {
    store = mock(Store.class);
  }

  /**
   * When a task is running the course selection has to be disabled.
   */
  @Test
  public void testDisableCourseSelectionTaskRunning() {
    // don't run this test in headless mode since it fails for unknown reasons, nevertheless, the
    // test succeeds in a headful testing environment
    Assume.assumeFalse("true".equals(System.getenv("HEADLESS")));

    Assert.assertFalse(courseSelection.isDisabled());
    Assert.assertFalse(courseUnsatCore.courseIsInfeasibleProperty().get());
    Assert.assertFalse(courseUnsatCore.taskRunningProperty().get());
    final UnsatCoreButtonBar checkFeasibilityButtonBar =
        lookup("#checkFeasibilityButtonBar").query();
    clickOn(checkFeasibilityButtonBar.getBtSubmitTask());
    sleep(1000, TimeUnit.MILLISECONDS);
    //Race Condition?
    Assert.assertTrue(courseUnsatCore.taskRunningProperty().get());
    Assert.assertTrue(courseSelection.isDisabled());
  }

  /**
   * When the current task is waiting for other tasks to finish the course selection should be
   * disabled and enabled when the task is cancelled especially before it enters the running state.
   */
  @Test
  public void testDisableCourseSelectionTaskWaiting() {
    executorService.submit(UiTestDataCreator.getSimpleTask(10));
    final UnsatCoreButtonBar checkFeasibilityButtonBar =
        lookup("#checkFeasibilityButtonBar").query();
    Assert.assertFalse(courseSelection.isDisabled());
    clickOn(checkFeasibilityButtonBar.getBtSubmitTask());
    Assert.assertTrue(courseSelection.isDisabled());
    clickOn(checkFeasibilityButtonBar.getCancelTask());
    Assert.assertFalse(courseSelection.isDisabled());
  }

  /**
   * Test that the unsat core computation is only enabled when the selected courses are infeasible,
   * i.e. a check feasibility task has to run beforehand.
   */
  @Test
  public void testCheckFeasibilityBeforeModuleUnsatCore() {
    final UnsatCoreButtonBar checkFeasibilityButtonBar =
        lookup("#checkFeasibilityButtonBar").query();
    final UnsatCoreButtonBar unsatCoreButtonBar = lookup("#unsatCoreButtonBar").query();
    Assert.assertFalse(checkFeasibilityButtonBar.isDisabled());
    Assert.assertFalse(unsatCoreButtonBar.isVisible());
    Assert.assertTrue(unsatCoreButtonBar.isDisabled());
    clickOn(checkFeasibilityButtonBar.getBtSubmitTask());
    sleep(2, TimeUnit.SECONDS);   // task runs 2 seconds
    courseUnsatCore.courseIsInfeasibleProperty().set(true);
    Assert.assertTrue(checkFeasibilityButtonBar.isDisabled());
    Assert.assertFalse(unsatCoreButtonBar.isDisabled());
    Assert.assertTrue(unsatCoreButtonBar.isVisible());
    courseUnsatCore.courseIsInfeasibleProperty().set(false);

    // course is feasible
    clickOn(courseSelection.getRbSingleSelection());
    clickOn(checkFeasibilityButtonBar.getBtSubmitTask());
    sleep(2, TimeUnit.SECONDS);   // task runs 2 seconds
    Assert.assertFalse(checkFeasibilityButtonBar.isDisabled());
    Assert.assertTrue(unsatCoreButtonBar.isDisabled());
    Assert.assertFalse(unsatCoreButtonBar.isVisible());
  }

  @Test
  public void testCourseSelection() {
    clickOn(courseSelection.getRbCombination());
    clickOn(courseSelection.getMajorMinorCourseSelection().getMajorComboBox())
        .type(KeyCode.DOWN)
        .type(KeyCode.DOWN)
        .type(KeyCode.ENTER);
    clickOn(courseSelection.getMajorMinorCourseSelection().getMinorComboBox())
        .type(KeyCode.DOWN)
        .type(KeyCode.ENTER);
    Assert.assertEquals(courseList.get(4), courseSelection.getSelectedCourses().get(0));
    Assert.assertEquals(courseList.get(3), courseSelection.getSelectedCourses().get(1));

    clickOn(courseSelection.getMajorMinorCourseSelection().getMajorComboBox())
        .type(KeyCode.DOWN)
        .type(KeyCode.ENTER);
    clickOn(courseSelection.getMajorMinorCourseSelection().getMinorComboBox())
        .type(KeyCode.UP)
        .type(KeyCode.ENTER);
    Assert.assertEquals(courseList.get(8), courseSelection.getSelectedCourses().get(0));
    Assert.assertEquals(courseList.get(2), courseSelection.getSelectedCourses().get(1));

    Assert.assertFalse(courseSelection.getMajorMinorCourseSelection().isDisabled());
    Assert.assertTrue(courseSelection.getSingleCourseSelection().isDisabled());
    clickOn(courseSelection.getRbSingleSelection());
    Assert.assertTrue(courseSelection.getMajorMinorCourseSelection().isDisabled());
    Assert.assertFalse(courseSelection.getSingleCourseSelection().isDisabled());
  }

  /**
   * Test the visibility of the panes and disable states of the button bars as well as the used
   * properties' states.
   */
  @Test
  public void testPaneVisibility() {
    final TitledPane modulesPane = lookup("#modulesPane").query();
    final TitledPane abstractUnitsPane = lookup("#abstractUnitsPane").query();
    final TitledPane groupPane = lookup("#groupPane").query();
    final TitledPane sessionPane = lookup("#sessionPane").query();

    Assert.assertFalse(modulesPane.isVisible());
    Assert.assertFalse(abstractUnitsPane.isVisible());
    Assert.assertFalse(groupPane.isVisible());
    Assert.assertFalse(sessionPane.isVisible());

    Assert.assertTrue(moduleUnsatCore.moduleProperty().isEmpty());
    Assert.assertTrue(abstractUnitUnsatCore.abstractUnitsProperty().isEmpty());
    Assert.assertTrue(groupUnsatCore.groupProperty().isEmpty());
    Assert.assertTrue(sessionUnsatCore.sessionProperty().isEmpty());

    // module unsat core step
    moduleUnsatCore.setCourses(courseList);
    moduleUnsatCore.setModules(modules);

    Assert.assertTrue(courseUnsatCore.getUnsatCoreButtonBar().isDisabled());
    Assert.assertFalse(moduleUnsatCore.getUnsatCoreButtonBar().isDisabled());

    Assert.assertTrue(modulesPane.isVisible());
    Assert.assertFalse(abstractUnitsPane.isVisible());
    Assert.assertFalse(groupPane.isVisible());
    Assert.assertFalse(sessionPane.isVisible());

    // abstract unit unsat core step
    abstractUnitUnsatCore.modulesProperty().bind(moduleUnsatCore.moduleProperty());
    abstractUnitUnsatCore.setAbstractUnits(abstractUnits);

    Assert.assertTrue(courseUnsatCore.getUnsatCoreButtonBar().isDisabled());
    Assert.assertTrue(moduleUnsatCore.getUnsatCoreButtonBar().isDisabled());
    Assert.assertFalse(abstractUnitUnsatCore.getUnsatCoreButtonBar().isDisabled());

    Assert.assertTrue(modulesPane.isVisible());
    Assert.assertTrue(abstractUnitsPane.isVisible());
    Assert.assertFalse(groupPane.isVisible());
    Assert.assertFalse(sessionPane.isVisible());

    // group unsat core step
    groupUnsatCore.abstractUnitsProperty().bind(abstractUnitUnsatCore.abstractUnitsProperty());
    groupUnsatCore.setGroups(groups);

    Assert.assertTrue(courseUnsatCore.getUnsatCoreButtonBar().isDisabled());
    Assert.assertTrue(moduleUnsatCore.getUnsatCoreButtonBar().isDisabled());
    Assert.assertTrue(abstractUnitUnsatCore.getUnsatCoreButtonBar().isDisabled());
    Assert.assertFalse(groupUnsatCore.getUnsatCoreButtonBar().isDisabled());

    Assert.assertTrue(modulesPane.isVisible());
    Assert.assertTrue(abstractUnitsPane.isVisible());
    Assert.assertTrue(groupPane.isVisible());
    Assert.assertFalse(sessionPane.isVisible());

    // in the last step, i.e. session unsat core computation, all panes are visible and all
    // unsat core button bars are disabled
    sessionUnsatCore.setSessions(sessions);

    Assert.assertTrue(courseUnsatCore.getUnsatCoreButtonBar().isDisabled());
    Assert.assertTrue(moduleUnsatCore.getUnsatCoreButtonBar().isDisabled());
    Assert.assertTrue(abstractUnitUnsatCore.getUnsatCoreButtonBar().isDisabled());
    Assert.assertTrue(groupUnsatCore.getUnsatCoreButtonBar().isDisabled());

    Assert.assertTrue(modulesPane.isVisible());
    Assert.assertTrue(abstractUnitsPane.isVisible());
    Assert.assertTrue(groupPane.isVisible());
    Assert.assertTrue(sessionPane.isVisible());

    // all panes invisible if the course selection has changed
    clickOn(courseSelection.getRbCombination());
    clickOn(courseSelection.getMajorMinorCourseSelection().getMajorComboBox())
        .type(KeyCode.DOWN)
        .type(KeyCode.DOWN)
        .type(KeyCode.ENTER);
    Assert.assertFalse(modulesPane.isVisible());
    Assert.assertFalse(abstractUnitsPane.isVisible());
    Assert.assertFalse(groupPane.isVisible());
    Assert.assertFalse(sessionPane.isVisible());
    // and all properties are empty
    Assert.assertTrue(moduleUnsatCore.moduleProperty().isEmpty());
    Assert.assertTrue(abstractUnitUnsatCore.abstractUnitsProperty().isEmpty());
    Assert.assertTrue(groupUnsatCore.groupProperty().isEmpty());
    Assert.assertTrue(sessionUnsatCore.sessionProperty().isEmpty());
  }

  /**
   * Test the visibility of the panes and disable states of the button bars when the unsat core
   * search is interrupted by changing the course selection.
   */
  @Test
  public void testPaneVisibilityInterrupted() {
    final TitledPane modulesPane = lookup("#modulesPane").query();
    final TitledPane abstractUnitsPane = lookup("#abstractUnitsPane").query();
    final TitledPane groupPane = lookup("#groupPane").query();
    final TitledPane sessionPane = lookup("#sessionPane").query();

    moduleUnsatCore.setCourses(courseList);
    moduleUnsatCore.setModules(modules);

    Assert.assertTrue(courseUnsatCore.getUnsatCoreButtonBar().isDisabled());
    Assert.assertFalse(moduleUnsatCore.getUnsatCoreButtonBar().isDisabled());

    Assert.assertTrue(modulesPane.isVisible());
    Assert.assertFalse(abstractUnitsPane.isVisible());
    Assert.assertFalse(groupPane.isVisible());
    Assert.assertFalse(sessionPane.isVisible());

    abstractUnitUnsatCore.modulesProperty().bind(moduleUnsatCore.moduleProperty());
    abstractUnitUnsatCore.setAbstractUnits(abstractUnits);

    Assert.assertTrue(courseUnsatCore.getUnsatCoreButtonBar().isDisabled());
    Assert.assertTrue(moduleUnsatCore.getUnsatCoreButtonBar().isDisabled());
    Assert.assertFalse(abstractUnitUnsatCore.getUnsatCoreButtonBar().isDisabled());

    Assert.assertTrue(modulesPane.isVisible());
    Assert.assertTrue(abstractUnitsPane.isVisible());
    Assert.assertFalse(groupPane.isVisible());
    Assert.assertFalse(sessionPane.isVisible());

    clickOn(courseSelection.getMajorMinorCourseSelection().getMajorComboBox())
        .type(KeyCode.DOWN)
        .type(KeyCode.ENTER);

    Assert.assertTrue(courseUnsatCore.getUnsatCoreButtonBar().isDisabled());
    Assert.assertTrue(moduleUnsatCore.getUnsatCoreButtonBar().isDisabled());
    Assert.assertTrue(abstractUnitUnsatCore.getUnsatCoreButtonBar().isDisabled());
    Assert.assertTrue(groupUnsatCore.getUnsatCoreButtonBar().isDisabled());

    Assert.assertFalse(modulesPane.isVisible());
    Assert.assertFalse(abstractUnitsPane.isVisible());
    Assert.assertFalse(groupPane.isVisible());
    Assert.assertFalse(sessionPane.isVisible());

    Assert.assertTrue(moduleUnsatCore.moduleProperty().isEmpty());
    Assert.assertTrue(abstractUnitUnsatCore.abstractUnitsProperty().isEmpty());
    Assert.assertTrue(groupUnsatCore.groupProperty().isEmpty());
    Assert.assertTrue(sessionUnsatCore.sessionProperty().isEmpty());
  }

  @After
  public void cleanup() throws Exception {
    WaitForAsyncUtils.waitForFxEvents();
    setupStage(Stage::close);
  }

  @Override
  public void start(final Stage stage) throws Exception {
    final FXMLLoader subLoader = new FXMLLoader();
    subLoader.setBuilderFactory(type -> {
      if (type.equals(TaskProgressIndicator.class)) {
        return () -> new TaskProgressIndicator(new Inflater(new FXMLLoader()));
      }
      return new JavaFXBuilderFactory().getBuilder(type);
    });

    final FXMLLoader loader = new FXMLLoader();
    loader.setBuilderFactory(type -> {
      if (type.equals(MajorMinorCourseSelection.class)) {
        return () -> new MajorMinorCourseSelection(new Inflater(new FXMLLoader()));
      } else if (type.equals(CombinationOrSingleCourseSelection.class)) {
        return () -> courseSelection;
      } else if (type.equals(TaskProgressIndicator.class)) {
        return () -> new TaskProgressIndicator(new Inflater(new FXMLLoader()));
      } else if (type.equals(UnsatCoreButtonBar.class)) {
        return () -> new UnsatCoreButtonBar(new Inflater(subLoader));
      }
      return new JavaFXBuilderFactory().getBuilder(type);
    });

    final Inflater inflater = new Inflater(loader);

    courseSelection = new CombinationOrSingleCourseSelection(inflater);
    courseSelection.setCourses(courseList);

    final SolverService solverService = UiTestDataCreator.getMockedSolverService();

    final Delayed<SolverService> delayedSolverService = new Delayed<>();
    delayedSolverService.set(solverService);
    final Delayed<Store> delayedStore = new Delayed<>();
    when(store.getCourses()).thenReturn(courseList);
    delayedStore.set(store);

    final UiDataService uiDataService = new UiDataService(delayedSolverService, delayedStore,
        executorService);

    courseUnsatCore = new CourseUnsatCore(inflater, delayedStore, delayedSolverService,
        executorService, uiDataService);
    moduleUnsatCore = new ModuleUnsatCore(inflater, new Router());
    abstractUnitUnsatCore = new AbstractUnitUnsatCore(inflater, new Router());
    groupUnsatCore = new GroupUnsatCore(inflater, new Router());
    sessionUnsatCore = new SessionUnsatCore(inflater, new Router(), uiDataService);

    final FXMLLoader unsatCoreLoader = new FXMLLoader();
    unsatCoreLoader.setBuilderFactory(type -> {
      if (type.equals(CourseUnsatCore.class)) {
        return () -> courseUnsatCore;
      } else if (type.equals(ModuleUnsatCore.class)) {
        return () -> moduleUnsatCore;
      } else if (type.equals(AbstractUnitUnsatCore.class)) {
        return () -> abstractUnitUnsatCore;
      } else if (type.equals(GroupUnsatCore.class)) {
        return () -> groupUnsatCore;
      } else if (type.equals(SessionUnsatCore.class)) {
        return () -> sessionUnsatCore;
      } else if (type.equals(ControllerHeader.class)) {
        return () -> new ControllerHeader(new Inflater(new FXMLLoader()));
      }
      return new JavaFXBuilderFactory().getBuilder(type);
    });
    final Inflater unsatCoreInflater = new Inflater(unsatCoreLoader);

    final UnsatCore unsatCore = new UnsatCore(unsatCoreInflater, delayedSolverService, delayedStore,
        executorService);

    final Scene scene = new Scene(unsatCore, 600, 800);

    stage.setScene(scene);
    stage.show();
  }
}
