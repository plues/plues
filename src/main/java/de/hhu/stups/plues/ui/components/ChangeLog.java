package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Log;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ChangeLog extends VBox implements Initializable {

  @FXML
  TableView<Log> persistentTable;

  @FXML
  private TableColumn<Log, Session> sessionP;

  @FXML
  private TableColumn<Log, String> sourceP;

  @FXML
  private TableColumn<Log, String> targetP;

  @FXML
  private TableColumn<Log, Date> dateP;

  @FXML
  TableView<Log> tempTable;

  @FXML
  private TableColumn<Log, Session> sessionT;

  @FXML
  private TableColumn<Log, String> sourceT;

  @FXML
  private TableColumn<Log, String> targetT;

  @FXML
  private TableColumn<Log, Date> dateT;

  private final Delayed<Store> delayedStore;

  /**
   * Constructor to create the change log.
   * @param inflater Inflater to handle fxml.
   * @param delayedStore Store which contains data
   */
  @Inject
  public ChangeLog(final Inflater inflater,
                   final Delayed<Store> delayedStore) {
    this.delayedStore = delayedStore;

    inflater.inflate("components/ChangeLog", this, this, "ChangeLog");
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    sessionP.setCellValueFactory(new PropertyValueFactory<>("session"));
    sourceP.setCellValueFactory(new PropertyValueFactory<>("src"));
    targetP.setCellValueFactory(new PropertyValueFactory<>("target"));
    dateP.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

    sessionT.setCellValueFactory(new PropertyValueFactory<>("session"));
    sourceT.setCellValueFactory(new PropertyValueFactory<>("src"));
    targetT.setCellValueFactory(new PropertyValueFactory<>("target"));
    dateT.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

    delayedStore.whenAvailable(store -> {
      List<Log> logs = store.getLogEntries();
      Date compare = new Date(ManagementFactory.getRuntimeMXBean().getStartTime());
      persistentTable.getItems().addAll(logs.stream()
          .filter(log -> log.getCreatedAt().compareTo(compare) < 0)
          .collect(Collectors.toList()));
      tempTable.getItems().addAll(logs.stream()
          .filter(log -> log.getCreatedAt().compareTo(compare) >= 0)
          .collect(Collectors.toList()));
      // sort entries by date they were created at
      persistentTable.getItems().sorted((o1, o2) -> o1.getCreatedAt().compareTo(o2.getCreatedAt()));
      tempTable.getItems().sorted((o1, o2) -> o1.getCreatedAt().compareTo(o2.getCreatedAt()));
    });
  }
}
