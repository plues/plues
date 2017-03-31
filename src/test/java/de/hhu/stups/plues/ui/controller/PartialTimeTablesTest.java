package de.hhu.stups.plues.ui.controller;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testfx.api.FxToolkit.setupStage;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.ui.UiTestHelper;
import de.hhu.stups.plues.ui.components.CheckBoxGroup;
import de.hhu.stups.plues.ui.components.CheckBoxGroupFactory;
import de.hhu.stups.plues.ui.components.MajorMinorCourseSelection;
import de.hhu.stups.plues.ui.components.TaskProgressIndicator;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import org.junit.After;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PartialTimeTablesTest extends ApplicationTest {

  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  private final ObservableList<Course> courseList = UiTestHelper.createCourseList();
  private final Store store;

  private MajorMinorCourseSelection courseSelection;
  private PartialTimeTables partialTimeTables;

  public PartialTimeTablesTest() {
    store = mock(Store.class);
  }

  @Test
  public void testScrollPaneVisibility() {
    final ScrollPane scrollPane = lookup("#scrollPane").query();
    final Button btChoose = lookup("#btChoose").query();
    assertFalse(scrollPane.isVisible());
    clickOn(btChoose);
    sleep(200, TimeUnit.MILLISECONDS);
    assertTrue(scrollPane.isVisible());
    clickOn(courseSelection.getMajorComboBox())
        .type(KeyCode.DOWN)
        .type(KeyCode.ENTER);
    assertFalse(scrollPane.isVisible());
    Platform.runLater(() -> partialTimeTables.btChoosePressed());
    sleep(200, TimeUnit.MILLISECONDS);
    assertTrue(scrollPane.isVisible());
  }

  @Test
  public void testButtonDisabledStates() {
    final Button btChoose = lookup("#btChoose").query();
    final Button btGenerate = lookup("#btGenerate").query();
    final Button btShow = lookup("#btShow").query();
    final Button btSave = lookup("#btSave").query();
    final Button btCancel = lookup("#btCancel").query();

    assertFalse(btChoose.isDisabled());
    assertTrue(btShow.isDisabled());
    assertTrue(btSave.isDisabled());
    assertTrue(btCancel.isDisabled());
    assertFalse(btGenerate.isVisible());
    assertFalse(btShow.isVisible());
    assertFalse(btSave.isVisible());
    assertFalse(btCancel.isVisible());

    clickOn(btChoose);
    sleep(200, TimeUnit.MILLISECONDS);
    assertFalse(btChoose.isDisabled());
    assertFalse(btGenerate.isDisabled());
    assertTrue(btShow.isDisabled());
    assertTrue(btSave.isDisabled());
    assertTrue(btCancel.isDisabled());
    assertTrue(btGenerate.isVisible());
    assertTrue(btShow.isVisible());
    assertTrue(btSave.isVisible());
    assertTrue(btCancel.isVisible());

    clickOn(btGenerate);
    sleep(200, TimeUnit.MILLISECONDS);
    assertTrue(btChoose.isDisabled());
    assertTrue(btGenerate.isDisabled());
    assertTrue(btShow.isDisabled());
    assertTrue(btSave.isDisabled());
    assertFalse(btCancel.isDisabled());
    assertTrue(btGenerate.isVisible());
    assertTrue(btShow.isVisible());
    assertTrue(btSave.isVisible());
    assertTrue(btCancel.isVisible());

    // wait for task to finish
    sleep(3, TimeUnit.SECONDS);
    assertFalse(btGenerate.isDisabled());
    assertFalse(btShow.isDisabled());
    assertFalse(btSave.isDisabled());
    assertTrue(btCancel.isDisabled());
    assertTrue(btGenerate.isVisible());
    assertTrue(btShow.isVisible());
    assertTrue(btSave.isVisible());
    assertTrue(btCancel.isVisible());

    clickOn(courseSelection.getMajorComboBox())
        .type(KeyCode.DOWN)
        .type(KeyCode.ENTER);
    assertFalse(btGenerate.isVisible());
    assertFalse(btShow.isVisible());
    assertFalse(btSave.isVisible());
    assertFalse(btCancel.isVisible());
  }

  @Test
  public void testCourseSelectionDisabledState() {
    final Button btChoose = lookup("#btChoose").query();
    final Button btGenerate = lookup("#btGenerate").query();
    assertFalse(courseSelection.isDisabled());
    clickOn(btChoose);
    assertFalse(courseSelection.isDisabled());
    clickOn(btGenerate);
    assertTrue(courseSelection.isDisabled());
    sleep(3, TimeUnit.SECONDS);
    assertFalse(courseSelection.isDisabled());
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
      if (type.equals(TaskProgressIndicator.class)) {
        return () -> new TaskProgressIndicator(new Inflater(new FXMLLoader()));
      } else if (type.equals(MajorMinorCourseSelection.class)) {
        return () -> courseSelection;
      }
      return new JavaFXBuilderFactory().getBuilder(type);
    });

    final Inflater inflater = new Inflater(loader);
    final SolverService solverService = UiTestHelper.getMockedSolverService();

    final Delayed<SolverService> delayedSolverService = new Delayed<>();
    delayedSolverService.set(solverService);

    final Delayed<Store> delayedStore = new Delayed<>();
    when(store.getCourses()).thenReturn(courseList);
    delayedStore.set(store);

    final UiDataService uiDataService = new UiDataService(delayedSolverService, delayedStore,
        executorService);

    final Module majorModule = new Module();
    majorModule.setTitle("Major Module");

    final AbstractUnit unit = new AbstractUnit();
    unit.setTitle("Unit");
    final AbstractUnit unit2 = new AbstractUnit();
    unit2.setTitle("Unit 2");
    final Set<AbstractUnit> majorUnits = new HashSet<>();
    majorUnits.add(unit2);
    majorUnits.add(unit);
    majorModule.setAbstractUnits(majorUnits);
    final Set<Module> modules = Collections.singleton(majorModule);

    courseSelection = new MajorMinorCourseSelection(inflater);
    Platform.runLater(() -> {
      courseSelection.setMajorCourseList(FXCollections.observableArrayList(
          Arrays.asList(UiTestHelper.getMockedMajorCourse(modules),
              UiTestHelper.getMockedMajorCourse(modules))));
      courseSelection.setMinorCourseList(FXCollections.observableArrayList(
          Arrays.asList(UiTestHelper.getMockedMinorCourse(modules),
              UiTestHelper.getMockedMajorCourse(modules))));
    });

    final CheckBoxGroupFactory checkBoxGroupFactory = mock(CheckBoxGroupFactory.class);
    when(checkBoxGroupFactory.create(any(), any()))
        .thenAnswer(invocation -> new CheckBoxGroup(inflater, courseSelection.getSelectedMajor(),
            majorModule));

    partialTimeTables = new PartialTimeTables(inflater, delayedStore, delayedSolverService,
        uiDataService, ((major, minor, solverTask) -> UiTestHelper.getWaitingPdfRenderingTask()),
        executorService, checkBoxGroupFactory);

    final Scene scene = new Scene(partialTimeTables, 400, 500);

    stage.setScene(scene);
    stage.show();
  }
}
