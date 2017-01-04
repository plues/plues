package de.hhu.stups.plues.ui.components;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.ObservableStore;
import de.hhu.stups.plues.data.entities.Log;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.ui.layout.Inflater;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.testfx.framework.junit.ApplicationTest;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

@RunWith(JUnit4.class)
public class ChangeLogTest extends ApplicationTest {

  private ChangeLog changeLog;

  @Test
  public void sizeOfLists() {
    Assert.assertTrue(changeLog.getPersistentTable().getItems().size() == 2);
    Assert.assertTrue(changeLog.getTempTable().getItems().size() == 1);
  }

  @Test
  public void orderInList() {
    final Log log0 = changeLog.getPersistentTable().getItems().get(0);
    final Log log1 = changeLog.getPersistentTable().getItems().get(1);
    Assert.assertTrue(log0.getCreatedAt().compareTo(log1.getCreatedAt()) > 0);
  }

  @Override
  public void start(final Stage stage) throws Exception {
    final Inflater inflater = new Inflater(new FXMLLoader());
    final Delayed<ObservableStore> delayed = new Delayed<>();

    final Log l1 = mock(Log.class);
    final Date d1 = new Calendar.Builder().setDate(2016, 11, 1).build().getTime();
    doReturn(d1).when(l1).getCreatedAt();

    final Log l2 = mock(Log.class);
    final Date d2 = new Calendar.Builder().setDate(2016, 10, 2).build().getTime();
    doReturn(d2).when(l2).getCreatedAt();

    final Log l3 = mock(Log.class);
    final Date d3 = new Calendar.Builder().setDate(2016, 11, 4).build().getTime();
    doReturn(d3).when(l3).getCreatedAt();

    final ObservableStore store = mock(ObservableStore.class);
    doReturn(Arrays.asList(l1, l2, l3)).when(store).getLogEntries();

    final UiDataService dataService = getUiDataService();

    delayed.set(store);
    changeLog = new ChangeLog(inflater, dataService, delayed);

    final Scene scene = new Scene(changeLog, 600, 600);
    stage.setScene(scene);
    stage.show();
  }

  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
  private UiDataService getUiDataService() {
    final UiDataService dataService = mock(UiDataService.class);
    final Date lastSaveDate = new Calendar.Builder().setDate(2016, 11, 3).build().getTime();
    doReturn(new SimpleObjectProperty<>(lastSaveDate)).when(dataService).lastSavedDateProperty();
    return dataService;
  }
}
