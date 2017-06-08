package de.hhu.stups.plues.ui.components.conflictmatrix;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testfx.api.FxToolkit.setupStage;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.ResultState;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.ui.UiTestDataCreator;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;


public class ResultGridCellTest extends ApplicationTest {

  private final VBox box = new VBox();
  private ResultGridCell resultGridCell;
  private ResultGridCell resultGridCellSingleCourse;

  @Test
  public void succeededGridCell() {
    resultGridCell.setResultState(ResultState.SUCCEEDED);
    resultGridCellSingleCourse.setResultState(ResultState.SUCCEEDED);
    Assert.assertFalse(resultGridCell.getContextMenu().isShowing());
    Assert.assertFalse(resultGridCellSingleCourse.getContextMenu().isShowing());
    clickOn(resultGridCell);
    Assert.assertFalse(resultGridCellSingleCourse.getContextMenu().isShowing());
    Assert.assertTrue(resultGridCell.getContextMenu().isShowing());
    // combination of courses has different context menu items than a single combinable course
    Assert.assertEquals(3, resultGridCell.getContextMenu().getItems().size());
    Assert.assertEquals(1, resultGridCellSingleCourse.getContextMenu().getItems().size());
  }

  @Test
  public void failedGridCell() {
    resultGridCell.setResultState(ResultState.FAILED);
    Assert.assertFalse(resultGridCell.getContextMenu().isShowing());
    clickOn(resultGridCell);
    Assert.assertTrue(resultGridCell.getContextMenu().isShowing());
    Assert.assertEquals(2, resultGridCell.getContextMenu().getItems().size());
  }

  @Test
  public void timeoutGridCell() {
    resultGridCell.setResultState(ResultState.TIMEOUT);
    Assert.assertFalse(resultGridCell.getContextMenu().isShowing());
    clickOn(resultGridCell);
    Assert.assertTrue(resultGridCell.getContextMenu().isShowing());
    Assert.assertEquals(1, resultGridCell.getContextMenu().getItems().size());
  }

  @Test
  public void impossibleGridCell() {
    resultGridCell.setResultState(ResultState.IMPOSSIBLE);
    Assert.assertFalse(resultGridCell.getContextMenu().isShowing());
    clickOn(resultGridCell);
    Assert.assertTrue(resultGridCell.getContextMenu().isShowing());
    Assert.assertEquals(1, resultGridCell.getContextMenu().getItems().size());
  }

  @Test
  public void impossibleCombinationGridCell() {
    resultGridCell.setResultState(ResultState.IMPOSSIBLE_COMBINATION);
    Assert.assertFalse(resultGridCell.getContextMenu().isShowing());
    clickOn(resultGridCell);
    Assert.assertFalse(resultGridCell.getContextMenu().isShowing());
    Assert.assertTrue(resultGridCell.getContextMenu().getItems().isEmpty());
  }

  @After
  public void cleanup() throws Exception {
    WaitForAsyncUtils.waitForFxEvents();
    setupStage(Stage::close);
  }

  @Override
  public void start(final Stage stage) throws Exception {
    final ResultContextMenuFactory factory = mock(ResultContextMenuFactory.class);
    final Router router = mock(Router.class);
    final Course course1 = UiTestDataCreator.createCourse("shortName1", "bk", "H");
    final Course course2 = UiTestDataCreator.createCourse("shortName2", "bk", "N");

    when(factory.create(any())).thenAnswer(invocation -> {
      if (invocation.getArguments().length > 1) {
        return new ResultContextMenu(router, course1, course2);
      } else {
        return new ResultContextMenu(router, course1);
      }
    });

    resultGridCell = new ResultGridCell(factory, course1, course2);
    resultGridCell.setEnabled(true);

    resultGridCellSingleCourse = new ResultGridCell(factory, course1);
    resultGridCellSingleCourse.setEnabled(true);

    box.getChildren().addAll(resultGridCell, resultGridCellSingleCourse);

    final Scene scene = new Scene(box, 30, 50);

    stage.setScene(scene);
    stage.show();
  }
}
