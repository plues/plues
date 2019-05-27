package de.hhu.stups.plues.ui.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testfx.api.FxToolkit.setupStage;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.services.PdfRenderingService;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.hhu.stups.plues.ui.UiTestDataCreator;
import de.hhu.stups.plues.ui.components.ColorSchemeSelection;
import de.hhu.stups.plues.ui.components.ControllerHeader;
import de.hhu.stups.plues.ui.components.MajorMinorCourseSelection;
import de.hhu.stups.plues.ui.components.PdfGenerationSettings;
import de.hhu.stups.plues.ui.components.ResultBox;
import de.hhu.stups.plues.ui.components.ResultBoxFactory;
import de.hhu.stups.plues.ui.components.TaskProgressIndicator;
import de.hhu.stups.plues.ui.components.UnitDisplayFormat;
import de.hhu.stups.plues.ui.components.UnitDisplayFormatSelection;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import org.junit.After;
import org.junit.Test;
import org.mockito.internal.stubbing.answers.ThrowsException;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MusterstudienplaeneTest extends ApplicationTest {

  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  private final ObservableList<Course> courseList = UiTestDataCreator.createCourseList();
  private final Store store;

  private MajorMinorCourseSelection courseSelection;
  private ListView<ResultBox> resultBoxWrapper;
  private Musterstudienplaene musterstudienplaene;

  public MusterstudienplaeneTest() {
    store = mock(Store.class, new ThrowsException(new RuntimeException()));
  }

  @Test
  public void testCheckFeasibilityComputation() {
    assertFalse(resultBoxWrapper.isVisible());
    assertEquals(0, resultBoxWrapper.getItems().size());
    clickOn(musterstudienplaene.getBtGenerate());
    assertTrue(resultBoxWrapper.isVisible());
    assertEquals(1, resultBoxWrapper.getItems().size());
    clickOn(courseSelection.getMajorComboBox())
        .type(KeyCode.DOWN)
        .type(KeyCode.ENTER);
    clickOn(musterstudienplaene.getBtGenerate());
    assertTrue(resultBoxWrapper.isVisible());
    assertEquals(2, resultBoxWrapper.getItems().size());
  }

  @Test
  public void testNoRedundantResultBoxes() {
    assertFalse(resultBoxWrapper.isVisible());
    assertEquals(0, resultBoxWrapper.getItems().size());
    clickOn(musterstudienplaene.getBtGenerate());
    assertTrue(resultBoxWrapper.isVisible());
    assertEquals(1, resultBoxWrapper.getItems().size());
    clickOn(musterstudienplaene.getBtGenerate());
    assertTrue(resultBoxWrapper.isVisible());
    assertEquals(1, resultBoxWrapper.getItems().size());
    clickOn(courseSelection.getMajorComboBox())
        .type(KeyCode.DOWN)
        .type(KeyCode.ENTER);
    clickOn(musterstudienplaene.getBtGenerate());
    assertTrue(resultBoxWrapper.isVisible());
    assertEquals(2, resultBoxWrapper.getItems().size());
    clickOn(musterstudienplaene.getBtGenerate());
    assertTrue(resultBoxWrapper.isVisible());
    assertEquals(2, resultBoxWrapper.getItems().size());
    clickOn(courseSelection.getMajorComboBox())
        .type(KeyCode.DOWN)
        .type(KeyCode.ENTER);
    clickOn(musterstudienplaene.getBtGenerate());
    assertTrue(resultBoxWrapper.isVisible());
    assertEquals(3, resultBoxWrapper.getItems().size());
  }

  @Test
  public void testExistingResultBoxToTop() {
    assertFalse(resultBoxWrapper.isVisible());
    clickOn(musterstudienplaene.getBtGenerate());
    clickOn(courseSelection.getMajorComboBox())
        .type(KeyCode.DOWN)
        .type(KeyCode.ENTER);
    clickOn(musterstudienplaene.getBtGenerate());
    sleep(2000, TimeUnit.MILLISECONDS);
    assertEquals(2, resultBoxWrapper.getItems().size());
    assertTrue(resultBoxWrapper.isVisible());
    final ResultBox existingResultBox = resultBoxWrapper.getItems().get(1);
    clickOn(courseSelection.getMajorComboBox())
        .type(KeyCode.UP)
        .type(KeyCode.ENTER);
    clickOn(musterstudienplaene.getBtGenerate());
    sleep(2000, TimeUnit.MILLISECONDS);
    assertEquals(existingResultBox, resultBoxWrapper.getItems().get(0));
    clickOn(courseSelection.getMajorComboBox())
        .type(KeyCode.DOWN)
        .type(KeyCode.ENTER);
    clickOn(musterstudienplaene.getBtGenerate());
    sleep(2000, TimeUnit.MILLISECONDS);
    assertEquals(existingResultBox, resultBoxWrapper.getItems().get(1));
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
      } else if (type.equals(ControllerHeader.class)) {
        return () -> new ControllerHeader(new Inflater(new FXMLLoader()));
      } else if (type.equals(ColorSchemeSelection.class)) {
        return () -> new ColorSchemeSelection(new Inflater(new FXMLLoader()));
      } else if (type.equals(UnitDisplayFormatSelection.class)) {
        return () -> new UnitDisplayFormatSelection(new Inflater(new FXMLLoader()));
      } else if (type.equals(MajorMinorCourseSelection.class)) {
        return () -> courseSelection;
      }
      return new JavaFXBuilderFactory().getBuilder(type);
    });

    final Inflater inflater = new Inflater(loader);
    final SolverService solverService = UiTestDataCreator.getMockedSolverService();

    final Delayed<SolverService> delayedSolverService = new Delayed<>();
    delayedSolverService.set(solverService);

    final Delayed<Store> delayedStore = new Delayed<>();
    doReturn(courseList).when(store).getCourses();
    doReturn(courseList.stream().filter(Course::isMajor).collect(Collectors.toList())).when(store).getMajors();
    delayedStore.set(store);

    final UiDataService uiDataService = new UiDataService(delayedSolverService, delayedStore,
        executorService);

    final Router router = new Router();

    courseSelection = new MajorMinorCourseSelection(inflater);
    courseSelection.setMajorCourseList(courseList);

    final PdfRenderingService pdfRenderingService = mock(PdfRenderingService.class);
    doAnswer(invocation ->
        executorService.submit((PdfRenderingTask) invocation.getArgument(0)))
        .when(pdfRenderingService).submit(any());
    when(pdfRenderingService.getTask(any()))
        .thenReturn(UiTestDataCreator.getWaitingPdfRenderingTask());
    when(pdfRenderingService.pdfGenerationSettingsProperty())
        .thenReturn(new SimpleObjectProperty<>());
    when(pdfRenderingService.availableProperty())
        .thenReturn(new SimpleBooleanProperty(true));

    final ResultBoxFactory resultBoxFactory = mock(ResultBoxFactory.class);
    when(resultBoxFactory.create(any(), any(), any(), any()))
        .thenAnswer(invocation ->
            new ResultBox(inflater, router, pdfRenderingService,
                courseSelection.getSelectedMajor(),
                courseSelection.getSelectedMinor(),
                resultBoxWrapper,
                new PdfGenerationSettings(UiTestDataCreator.getColorScheme(),
                    UnitDisplayFormat.TITLE)));

    musterstudienplaene = new Musterstudienplaene(inflater, delayedStore, delayedSolverService,
        uiDataService, resultBoxFactory);
    resultBoxWrapper = musterstudienplaene.getResultBoxWrapper();

    final Scene scene = new Scene(musterstudienplaene, 400, 500);

    stage.setScene(scene);
    stage.show();
  }
}
