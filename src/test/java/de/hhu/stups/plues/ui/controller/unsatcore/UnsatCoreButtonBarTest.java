package de.hhu.stups.plues.ui.controller.unsatcore;

import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.junit.Assert;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class UnsatCoreButtonBarTest extends ApplicationTest {

  private UnsatCoreButtonBar unsatCoreButtonBar;
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();

  @Test
  public void submitTaskTest() {
    Assert.assertTrue(unsatCoreButtonBar.getBtCancelTask().isDisabled());
    Assert.assertFalse(unsatCoreButtonBar.getBtSubmitTask().isDisabled());

    unsatCoreButtonBar.setOnAction((event) -> runSimpleTask(3));
    Assert.assertTrue(unsatCoreButtonBar.getBtCancelTask().isDisabled());
    Assert.assertFalse(unsatCoreButtonBar.getBtSubmitTask().isDisabled());
    Assert.assertEquals(unsatCoreButtonBar.getTask(), null);

    clickOn(unsatCoreButtonBar.getBtSubmitTask());
    Assert.assertNotEquals(unsatCoreButtonBar.getTask(), null);
    Assert.assertFalse(unsatCoreButtonBar.getBtCancelTask().isDisabled());
    Assert.assertTrue(unsatCoreButtonBar.getBtSubmitTask().isDisabled());
  }

  @Test
  public void cancelTaskTest() {
    unsatCoreButtonBar.setOnAction((event) -> runSimpleTask(10));
    Assert.assertEquals(unsatCoreButtonBar.getTask(), null);

    clickOn(unsatCoreButtonBar.getBtSubmitTask());
    Assert.assertNotEquals(unsatCoreButtonBar.getTask(), null);
    Assert.assertFalse(unsatCoreButtonBar.getBtCancelTask().isDisabled());
    Assert.assertTrue(unsatCoreButtonBar.getBtSubmitTask().isDisabled());

    clickOn(unsatCoreButtonBar.getBtCancelTask());
  }

  private void runSimpleTask(final int sleep) {
    final Task<Boolean> simpleTask = getSimpleTask(sleep);
    unsatCoreButtonBar.showTaskState(simpleTask);
    executorService.submit(simpleTask);

    simpleTask.setOnSucceeded((event -> {
      Assert.assertTrue(unsatCoreButtonBar.getBtCancelTask().isDisabled());
      Assert.assertFalse(unsatCoreButtonBar.getBtSubmitTask().isDisabled());
    }));

    simpleTask.setOnCancelled((event -> {
      Assert.assertTrue(unsatCoreButtonBar.getBtCancelTask().isDisabled());
      Assert.assertFalse(unsatCoreButtonBar.getBtSubmitTask().isDisabled());
    }));
  }

  private Task<Boolean> getSimpleTask(final int sleep) {
    return new Task<Boolean>() {
      @Override
      protected Boolean call() throws Exception {
        TimeUnit.SECONDS.sleep(sleep);
        return true;
      }
    };
  }

  @Override
  public void start(Stage stage) throws Exception {

    final Inflater inflater = new Inflater(new FXMLLoader());
    unsatCoreButtonBar = new UnsatCoreButtonBar(inflater);

    final Scene scene = new Scene(unsatCoreButtonBar, 400, 700);

    stage.setScene(scene);
    stage.show();
  }
}
