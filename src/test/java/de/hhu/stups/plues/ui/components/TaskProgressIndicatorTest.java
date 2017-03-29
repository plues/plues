package de.hhu.stups.plues.ui.components;

import static org.testfx.api.FxToolkit.setupStage;

import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.Stage;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class TaskProgressIndicatorTest extends ApplicationTest {

  private TaskProgressIndicator taskProgressIndicator;
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  private final Task simpleTask = UiTestHelper.getSimpleTask(3);

  @Test
  public void testScheduledToRunningTask() {
    Assert.assertEquals(null, taskProgressIndicator.taskProperty().get());
    taskProgressIndicator.taskProperty().set(simpleTask);
    Assert.assertEquals(simpleTask, taskProgressIndicator.taskProperty().get());
    final ProgressIndicator progressIndicator = lookup("#progressIndicator").query();
    Assert.assertFalse(progressIndicator.isVisible());
    Assert.assertTrue(taskProgressIndicator.getTaskStateIcon().isVisible());
    executorService.submit(simpleTask);
    sleep(1, TimeUnit.SECONDS);
    Assert.assertTrue(progressIndicator.isVisible());
    Assert.assertFalse(taskProgressIndicator.getTaskStateIcon().isVisible());
  }

  @Test
  public void testShowIconOnFinished() {
    Assert.assertEquals(null, taskProgressIndicator.taskProperty().get());
    taskProgressIndicator.taskProperty().set(simpleTask);
    Assert.assertEquals(simpleTask, taskProgressIndicator.taskProperty().get());
    executorService.submit(simpleTask);
    sleep(4, TimeUnit.SECONDS);
    Assert.assertTrue(taskProgressIndicator.getTaskStateIcon().isVisible());
  }

  @Test
  public void testHideIconOnFinished() {
    taskProgressIndicator.showIconOnSucceededProperty().set(false);
    Assert.assertEquals(null, taskProgressIndicator.taskProperty().get());
    taskProgressIndicator.taskProperty().set(simpleTask);
    Assert.assertEquals(simpleTask, taskProgressIndicator.taskProperty().get());
    executorService.submit(simpleTask);
    sleep(4, TimeUnit.SECONDS);
    Assert.assertFalse(taskProgressIndicator.getTaskStateIcon().isVisible());
  }

  @After
  public void cleanup() throws Exception {
    WaitForAsyncUtils.waitForFxEvents();
    setupStage(Stage::close);
  }

  @Override
  public void start(final Stage stage) throws Exception {
    final FXMLLoader loader = new FXMLLoader();
    final Inflater inflater = new Inflater(loader);

    taskProgressIndicator = new TaskProgressIndicator(inflater);

    final Scene scene = new Scene(taskProgressIndicator, 150, 100);

    stage.setScene(scene);
    stage.show();
  }
}
