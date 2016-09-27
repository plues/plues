package de.hhu.stups.plues.ui.components;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.ui.layout.Inflater;
import de.hhu.stups.plues.studienplaene.MockStore;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.testfx.framework.junit.ApplicationTest;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

@RunWith(JUnit4.class)
public class ChangeLogTest extends ApplicationTest{

  private ChangeLog changeLog;

  @Test
  public void sizeOfLists() {
    Assert.assertTrue(changeLog.tempTable.getItems().size() == 1);
    Assert.assertTrue(changeLog.persistentTable.getItems().size() == 1);
  }

  @Override
  public void start(Stage stage) throws Exception {
    Inflater inflater = new Inflater(new FXMLLoader());
    Delayed<Store> delayed = new Delayed<>();
    Store store = new MockStore();
    delayed.set(store);
    changeLog = new ChangeLog(inflater, delayed);

    final Scene scene = new Scene(changeLog, 200, 200);
    stage.setScene(scene);
    stage.show();
  }
}
