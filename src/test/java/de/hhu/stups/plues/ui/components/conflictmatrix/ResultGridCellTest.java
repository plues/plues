package de.hhu.stups.plues.ui.components.conflictmatrix;

import static org.testfx.api.FxToolkit.setupStage;

import de.hhu.stups.plues.prob.ResultState;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.ui.UiTestHelper;

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
    final Router router = new Router();
    resultGridCell = new ResultGridCell(ResultState.UNKNOWN,
        UiTestHelper.createCourse("shortName1", "bk", "H"),
        UiTestHelper.createCourse("shortName2", "bk", "N"));
    resultGridCell.setRouter(router);
    resultGridCellSingleCourse = new ResultGridCell(ResultState.UNKNOWN,
        UiTestHelper.createCourse("shortName1", "bk", "H"));
    resultGridCellSingleCourse.setRouter(router);

    box.getChildren().addAll(resultGridCell, resultGridCellSingleCourse);

    final Scene scene = new Scene(box, 30, 50);

    stage.setScene(scene);
    stage.show();
  }
}
