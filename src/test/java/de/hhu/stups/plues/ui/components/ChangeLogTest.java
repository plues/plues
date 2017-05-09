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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
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
    final LocalDateTime d1 = LocalDate.of(2016, 11, 1).atStartOfDay();
    doReturn(d1).when(l1).getCreatedAt();
    doReturn("mon").when(l1).getSrcDay();
    doReturn(1).when(l1).getSrcTime();
    doReturn("mon").when(l1).getTargetDay();
    doReturn(2).when(l1).getTargetTime();

    final Log l2 = mock(Log.class);
    final LocalDateTime d2 = LocalDate.of(2016,10,2).atStartOfDay();
    doReturn(d2).when(l2).getCreatedAt();
    doReturn("tue").when(l2).getSrcDay();
    doReturn(3).when(l2).getSrcTime();
    doReturn("tue").when(l2).getTargetDay();
    doReturn(4).when(l2).getTargetTime();

    final Log l3 = mock(Log.class);
    final LocalDateTime d3 = LocalDate.of(2016, 11, 4).atStartOfDay();
    doReturn(d3).when(l3).getCreatedAt();
    doReturn("wed").when(l3).getSrcDay();
    doReturn(5).when(l3).getSrcTime();
    doReturn("wed").when(l3).getTargetDay();
    doReturn(6).when(l3).getTargetTime();

    final ObservableStore store = mock(ObservableStore.class);
    doReturn(Arrays.asList(l1, l2, l3)).when(store).getLogEntries();

    final UiDataService dataService = getUiDataService();

    delayed.set(store);
    changeLog = new ChangeLog(inflater, dataService, delayed);

    final Scene scene = new Scene(changeLog, 600, 600);
    stage.setScene(scene);
    stage.show();
  }

  private UiDataService getUiDataService() {
    final UiDataService dataService = mock(UiDataService.class);
    final LocalDateTime lastSaveDate = LocalDate.of(2016,11,3).atStartOfDay();
    doReturn(new SimpleObjectProperty<>(lastSaveDate)).when(dataService).lastSavedDateProperty();
    return dataService;
  }
}
