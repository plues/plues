package de.hhu.stups.plues.ui.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testfx.api.FxToolkit.setupStage;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.ui.UiTestHelper;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import org.junit.After;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CheckCourseFeasibilityTest extends ApplicationTest {

  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  private final ObservableList<Course> courseList = UiTestHelper.createCourseList();
  private final Store store;

  private CheckCourseFeasibility checkCourseFeasibility;
  private CombinationOrSingleCourseSelection courseSelection;
  private ListView<FeasibilityBox> feasibilityBoxWrapper;

  public CheckCourseFeasibilityTest() {
    store = mock(Store.class);
  }

  @Test
  public void testCheckFeasibilityComputation() {
    clickOn(courseSelection.getMajorMinorCourseSelection().getMajorComboBox())
        .type(KeyCode.DOWN)
        .type(KeyCode.ENTER);
    assertFalse(feasibilityBoxWrapper.isVisible());
    assertEquals(0, feasibilityBoxWrapper.getItems().size());
    checkCourseFeasibility.checkFeasibility();
    sleep(500, TimeUnit.MILLISECONDS);
    assertEquals(1, feasibilityBoxWrapper.getItems().size());
    assertTrue(feasibilityBoxWrapper.isVisible());

    clickOn(courseSelection.getMajorMinorCourseSelection().getMajorComboBox())
        .type(KeyCode.DOWN)
        .type(KeyCode.ENTER);
    checkCourseFeasibility.checkFeasibility();
    sleep(500, TimeUnit.MILLISECONDS);
    assertEquals(2, feasibilityBoxWrapper.getItems().size());
  }

  @Test
  public void testNoRedundantFeasibilityBoxes() {
    clickOn(courseSelection.getMajorMinorCourseSelection().getMajorComboBox())
        .type(KeyCode.DOWN)
        .type(KeyCode.ENTER);
    assertFalse(feasibilityBoxWrapper.isVisible());
    checkCourseFeasibility.checkFeasibility();
    sleep(500, TimeUnit.MILLISECONDS);
    assertEquals(1, feasibilityBoxWrapper.getItems().size());
    assertTrue(feasibilityBoxWrapper.isVisible());
    checkCourseFeasibility.checkFeasibility();
    sleep(500, TimeUnit.MILLISECONDS);
    assertEquals(1, feasibilityBoxWrapper.getItems().size());
    assertTrue(feasibilityBoxWrapper.isVisible());

    clickOn(courseSelection.getMajorMinorCourseSelection().getMajorComboBox())
        .type(KeyCode.UP)
        .type(KeyCode.ENTER);
    checkCourseFeasibility.checkFeasibility();
    sleep(500, TimeUnit.MILLISECONDS);
    assertEquals(2, feasibilityBoxWrapper.getItems().size());
    assertTrue(feasibilityBoxWrapper.isVisible());
  }

  @Test
  public void testDifferentBoxForCombinedAndSingleMajorCourse() {
    assertFalse(feasibilityBoxWrapper.isVisible());
    checkCourseFeasibility.checkFeasibility();
    sleep(500, TimeUnit.MILLISECONDS);
    assertEquals(1, feasibilityBoxWrapper.getItems().size());
    final FeasibilityBox combinableBox = feasibilityBoxWrapper.getItems().get(0);
    clickOn(courseSelection.getRbSingleSelection());
    checkCourseFeasibility.checkFeasibility();
    sleep(500, TimeUnit.MILLISECONDS);
    assertEquals(2, feasibilityBoxWrapper.getItems().size());
    clickOn(courseSelection.getRbCombination());
    checkCourseFeasibility.checkFeasibility();
    sleep(500, TimeUnit.MILLISECONDS);
    assertEquals(2, feasibilityBoxWrapper.getItems().size());
    assertEquals(combinableBox, feasibilityBoxWrapper.getItems().get(0));
  }

  @Test
  public void testExistingFeasibilityBoxToTop() {
    assertFalse(feasibilityBoxWrapper.isVisible());
    checkCourseFeasibility.checkFeasibility();
    clickOn(courseSelection.getMajorMinorCourseSelection().getMajorComboBox())
        .type(KeyCode.DOWN)
        .type(KeyCode.ENTER);
    checkCourseFeasibility.checkFeasibility();
    sleep(500, TimeUnit.MILLISECONDS);
    assertEquals(2, feasibilityBoxWrapper.getItems().size());
    assertTrue(feasibilityBoxWrapper.isVisible());
    final FeasibilityBox existingFeasibilityBox = feasibilityBoxWrapper.getItems().get(1);
    clickOn(courseSelection.getMajorMinorCourseSelection().getMajorComboBox())
        .type(KeyCode.UP)
        .type(KeyCode.ENTER);
    checkCourseFeasibility.checkFeasibility();
    sleep(500, TimeUnit.MILLISECONDS);
    assertEquals(existingFeasibilityBox, feasibilityBoxWrapper.getItems().get(0));
    clickOn(courseSelection.getMajorMinorCourseSelection().getMajorComboBox())
        .type(KeyCode.DOWN)
        .type(KeyCode.ENTER);
    checkCourseFeasibility.checkFeasibility();
    sleep(500, TimeUnit.MILLISECONDS);
    assertEquals(existingFeasibilityBox, feasibilityBoxWrapper.getItems().get(1));
  }

  @After
  public void cleanup() throws Exception {
    WaitForAsyncUtils.waitForFxEvents();
    setupStage(Stage::close);
  }

  @Override
  public void start(final Stage stage) throws Exception {
    final FXMLLoader loader = new FXMLLoader();
    loader.setBuilderFactory(type -> {
      if (type.equals(MajorMinorCourseSelection.class)) {
        return () -> new MajorMinorCourseSelection(new Inflater(new FXMLLoader()));
      } else if (type.equals(CombinationOrSingleCourseSelection.class)) {
        return () -> courseSelection;
      } else if (type.equals(TaskProgressIndicator.class)) {
        return () -> new TaskProgressIndicator(new Inflater(new FXMLLoader()));
      }
      return new JavaFXBuilderFactory().getBuilder(type);
    });

    final Inflater inflater = new Inflater(loader);
    final SolverService solverService = UiTestHelper.getMockedSolverService();

    final Delayed<SolverService> delayedSolverService = new Delayed<>();
    delayedSolverService.set(solverService);
    courseSelection = new CombinationOrSingleCourseSelection(inflater);
    courseSelection.setCourses(courseList);

    final Delayed<Store> delayedStore = new Delayed<>();
    when(store.getCourses()).thenReturn(courseList);
    delayedStore.set(store);

    final UiDataService uiDataService = new UiDataService(delayedSolverService, delayedStore,
        executorService);

    final Router router = new Router();

    final FeasibilityBoxFactory feasibilityBoxFactory = mock(FeasibilityBoxFactory.class);
    when(feasibilityBoxFactory.create(any(), any(), any()))
        .thenAnswer(invocation ->
            new FeasibilityBox(inflater, router, delayedSolverService, executorService,
                () -> null, uiDataService,
                courseSelection.getMajorMinorCourseSelection().getSelectedMajor(),
                courseSelection.getRbCombination().isSelected()
                    ? courseSelection.getMajorMinorCourseSelection().getSelectedMinor()
                    : null,
                feasibilityBoxWrapper));

    checkCourseFeasibility = new CheckCourseFeasibility(inflater, feasibilityBoxFactory,
        delayedSolverService, uiDataService);
    feasibilityBoxWrapper = checkCourseFeasibility.getFeasibilityBoxWrapper();

    final Scene scene = new Scene(checkCourseFeasibility, 400, 500);

    stage.setScene(scene);
    stage.show();
  }
}
