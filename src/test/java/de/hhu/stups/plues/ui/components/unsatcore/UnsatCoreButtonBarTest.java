package de.hhu.stups.plues.ui.components.unsatcore;

import de.hhu.stups.plues.ui.components.TaskProgressIndicator;
import de.hhu.stups.plues.ui.components.UiTestHelper;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UnsatCoreButtonBarTest extends ApplicationTest {

  private UnsatCoreButtonBar unsatCoreButtonBar;
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();

  @Test
  public void submitTaskTest() {
    Assume.assumeFalse("true".equals(System.getenv("TRAVIS")));

    Assert.assertTrue(unsatCoreButtonBar.getCancelTask().isDisabled());
    Assert.assertFalse(unsatCoreButtonBar.getSubmitTask().isDisabled());

    unsatCoreButtonBar.setOnAction((event) -> runSimpleTask(3));
    Assert.assertTrue(unsatCoreButtonBar.getCancelTask().isDisabled());
    Assert.assertFalse(unsatCoreButtonBar.getSubmitTask().isDisabled());
    Assert.assertEquals(unsatCoreButtonBar.getTask(), null);

    clickOn(unsatCoreButtonBar.getSubmitTask());
    Assert.assertNotEquals(unsatCoreButtonBar.getTask(), null);
    Assert.assertFalse(unsatCoreButtonBar.getCancelTask().isDisabled());
    Assert.assertTrue(unsatCoreButtonBar.getSubmitTask().isDisabled());
  }

  @Test
  public void cancelTaskTest() {
    Assume.assumeFalse("true".equals(System.getenv("TRAVIS")));

    unsatCoreButtonBar.setOnAction((event) -> runSimpleTask(10));
    Assert.assertEquals(unsatCoreButtonBar.getTask(), null);

    clickOn(unsatCoreButtonBar.getSubmitTask());
    Assert.assertNotEquals(unsatCoreButtonBar.getTask(), null);
    Assert.assertFalse(unsatCoreButtonBar.getCancelTask().isDisabled());
    Assert.assertTrue(unsatCoreButtonBar.getSubmitTask().isDisabled());

    clickOn(unsatCoreButtonBar.getCancelTask());
  }

  @Test
  public void cancelWaitingTaskTest() {
    Assume.assumeFalse("true".equals(System.getenv("TRAVIS")));

    Assert.assertTrue(unsatCoreButtonBar.getCancelTask().isDisabled());
    Assert.assertFalse(unsatCoreButtonBar.getSubmitTask().isDisabled());

    executorService.submit(UiTestHelper.getSimpleTask(5));
    unsatCoreButtonBar.setOnAction((event) -> runSimpleTask(3));
    Assert.assertTrue(unsatCoreButtonBar.getCancelTask().isDisabled());
    Assert.assertFalse(unsatCoreButtonBar.getSubmitTask().isDisabled());
    Assert.assertEquals(unsatCoreButtonBar.getTask(), null);

    clickOn(unsatCoreButtonBar.getSubmitTask());
    Assert.assertNotEquals(unsatCoreButtonBar.getTask(), null);
    Assert.assertFalse(unsatCoreButtonBar.getCancelTask().isDisabled());
    Assert.assertTrue(unsatCoreButtonBar.getSubmitTask().isDisabled());

    clickOn(unsatCoreButtonBar.getCancelTask());
    Assert.assertNotEquals(unsatCoreButtonBar.getTask(), null);
    Assert.assertTrue(unsatCoreButtonBar.getCancelTask().isDisabled());
    Assert.assertFalse(unsatCoreButtonBar.getSubmitTask().isDisabled());
  }

  private void runSimpleTask(final int sleep) {
    final Task<Boolean> simpleTask = UiTestHelper.getSimpleTask(sleep);
    unsatCoreButtonBar.taskProperty().set(simpleTask);
    executorService.submit(simpleTask);

    simpleTask.setOnSucceeded((event -> {
      Assert.assertTrue(unsatCoreButtonBar.getCancelTask().isDisabled());
      Assert.assertFalse(unsatCoreButtonBar.getSubmitTask().isDisabled());
    }));

    simpleTask.setOnCancelled((event -> {
      Assert.assertTrue(unsatCoreButtonBar.getCancelTask().isDisabled());
      Assert.assertFalse(unsatCoreButtonBar.getSubmitTask().isDisabled());
    }));
  }

  @Override
  public void start(Stage stage) throws Exception {

    final FXMLLoader loader = new FXMLLoader();
    loader.setBuilderFactory(type -> {
      if (type.equals(TaskProgressIndicator.class)) {
        return () -> new TaskProgressIndicator(new Inflater(new FXMLLoader()));
      }
      return new JavaFXBuilderFactory().getBuilder(type);
    });

    final Inflater inflater = new Inflater(loader);

    unsatCoreButtonBar = new UnsatCoreButtonBar(inflater);

    final Scene scene = new Scene(unsatCoreButtonBar, 400, 700);

    stage.setScene(scene);
    stage.show();
  }
}
