package de.hhu.stups.plues.ui.components.unsatcore;

import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.ui.components.CombinationOrSingleCourseSelection;
import de.hhu.stups.plues.ui.components.MajorMinorCourseSelection;
import de.hhu.stups.plues.ui.components.TaskProgressIndicator;
import de.hhu.stups.plues.ui.components.UiTestHelper;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;

import org.junit.Assert;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CourseUnsatCoreTest extends ApplicationTest {

  private CombinationOrSingleCourseSelection courseSelection;
  private List<Course> courseList;

  private UnsatCoreButtonBar unsatCoreButtonBar;
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();

  private CourseUnsatCore courseUnsatCore;
  private final Store store;

  public CourseUnsatCoreTest() {
    store = mock(Store.class);
  }

  @Test
  public void testDisableSelectionTaskRunning() {
    Assert.assertFalse(courseSelection.isDisabled());
    Assert.assertFalse(courseUnsatCore.courseIsInfeasibleProperty().get());
    Assert.assertFalse(courseUnsatCore.taskRunningProperty().get());
    clickOn(((UnsatCoreButtonBar) lookup("#unsatCoreButtonBar").query()).getBtSubmitTask(),
        MouseButton.PRIMARY);
    Assert.assertTrue(courseUnsatCore.taskRunningProperty().get());
    Assert.assertTrue(courseSelection.isDisabled());
    sleep(2, TimeUnit.SECONDS);
    Assert.assertFalse(courseUnsatCore.taskRunningProperty().get());
  }

  /**
   * When the selected course is infeasible the button bar switches to unsat core computation. If
   * the selection changes the check feasibility button is restored and unsat core search disabled.
   */
  @Test
  public void testCheckFeasibilityBeforeUnsatCore() {
    final String btCheckFeasibilityText = unsatCoreButtonBar.getBtSubmitTask().getText();
    Assert.assertFalse(courseUnsatCore.courseIsInfeasibleProperty().get());
    courseUnsatCore.courseIsInfeasibleProperty().set(true);
    sleep(1, TimeUnit.SECONDS);
    Assert.assertNotEquals(btCheckFeasibilityText, unsatCoreButtonBar.getBtSubmitTask().getText());
    clickOn(courseSelection.lookup("#rbSingleSelection"), MouseButton.PRIMARY);
    Assert.assertEquals(btCheckFeasibilityText, unsatCoreButtonBar.getBtSubmitTask().getText());
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

  @Override
  public void start(final Stage stage) throws Exception {
    courseList = new ArrayList<>();
    courseList.add(UiTestHelper.createCourse("shortName1", "bk", "H"));
    courseList.add(UiTestHelper.createCourse("shortName2", "ba", "H"));
    courseList.add(UiTestHelper.createCourse("shortName3", "bk", "N"));
    courseList.add(UiTestHelper.createCourse("shortName4", "bk", "N"));
    courseList.add(UiTestHelper.createCourse("shortName5", "bk", "H"));
    courseList.add(UiTestHelper.createCourse("shortName6", "bk", "N"));
    courseList.add(UiTestHelper.createCourse("shortName7", "ma", "N"));
    courseList.add(UiTestHelper.createCourse("shortName8", "ma", "N"));
    courseList.add(UiTestHelper.createCourse("shortName9", "bk", "H"));
    courseList.add(UiTestHelper.createCourse("shortName10", "ma", "H"));

    final FXMLLoader loader = new FXMLLoader();
    loader.setBuilderFactory(type -> {
      if (type.equals(MajorMinorCourseSelection.class)) {
        return () -> new MajorMinorCourseSelection(new Inflater(new FXMLLoader()));
      } else if (type.equals(CombinationOrSingleCourseSelection.class)) {
        return () -> courseSelection;
      } else if (type.equals(TaskProgressIndicator.class)) {
        return () -> new TaskProgressIndicator(new Inflater(new FXMLLoader()));
      } else if (type.equals(UnsatCoreButtonBar.class)) {
        return () -> unsatCoreButtonBar;
      }
      return new JavaFXBuilderFactory().getBuilder(type);
    });

    final Inflater inflater = new Inflater(loader);

    courseSelection = new CombinationOrSingleCourseSelection(inflater);
    courseSelection.setCourses(courseList);

    unsatCoreButtonBar = new UnsatCoreButtonBar(inflater);

    final SolverService solverService = mock(SolverService.class);
    when(solverService.computeFeasibilityTask(anyVararg()))
        .thenReturn(UiTestHelper.getSimpleComputeFeasibilityTask());
    when(solverService.checkFeasibilityTask(anyVararg()))
        .thenReturn(UiTestHelper.getSimpleCheckFeasibilityTask());
    when(solverService.checkFeasibilityTask(anyVararg(), anyVararg()))
        .thenReturn(UiTestHelper.getSimpleCheckFeasibilityTask());
    when(solverService.impossibleCoursesTask())
        .thenReturn(UiTestHelper.getSimpleImpossibleCoursesTask());

    final Delayed<SolverService> delayedSolverService = new Delayed<>();
    delayedSolverService.set(solverService);
    final Delayed<Store> delayedStore = new Delayed<>();
    when(store.getCourses()).thenReturn(courseList);
    delayedStore.set(store);

    final UiDataService uiDataService = new UiDataService(delayedSolverService, delayedStore,
        executorService);

    courseUnsatCore = new CourseUnsatCore(inflater, delayedStore, delayedSolverService,
        Executors.newSingleThreadExecutor(), uiDataService);

    final Scene scene = new Scene(courseUnsatCore, 400, 700);

    stage.setScene(scene);
    stage.show();
  }
}
