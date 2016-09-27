package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Log;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.ui.layout.Inflater;

import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

public class ChangeLog extends VBox implements Initializable {

  @FXML
  TableView<Log> persistentTable;

  @FXML
  TableColumn<Log, Session> session_p;

  @FXML
  TableColumn<Log, String> source_p;

  @FXML
  TableColumn<Log, String> target_p;

  @FXML
  TableColumn<Log, Date> date_p;

  @FXML
  TableView<Log> tempTable;

  @FXML
  TableColumn<Log, Session> session_t;

  @FXML
  TableColumn<Log, String> source_t;

  @FXML
  TableColumn<Log, String> target_t;

  @FXML
  TableColumn<Log, Date> date_t;

  private final Delayed<Store> delayedStore;

  @Inject
  public ChangeLog(final Inflater inflater,
                   final Delayed<Store> delayedStore) {
    this.delayedStore = delayedStore;

    inflater.inflate("components/ChangeLog", this, this, "ChangeLog");
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    session_p.setCellValueFactory(new PropertyValueFactory<>("session"));
    source_p.setCellValueFactory(new PropertyValueFactory<>("src"));
    target_p.setCellValueFactory(new PropertyValueFactory<>("target"));
    date_p.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

    session_t.setCellValueFactory(new PropertyValueFactory<>("session"));
    source_t.setCellValueFactory(new PropertyValueFactory<>("src"));
    target_t.setCellValueFactory(new PropertyValueFactory<>("target"));
    date_t.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

    delayedStore.whenAvailable(store -> {
      List<Log> logs = store.getLog();
      Date compare = new Date(ManagementFactory.getRuntimeMXBean().getStartTime());
      persistentTable.getItems().addAll(logs.stream()
        .filter(log -> log.getCreatedAt().compareTo(compare) < 0)
        .collect(Collectors.toList()));
      tempTable.getItems().addAll(logs.stream()
        .filter(log -> log.getCreatedAt().compareTo(compare) >= 0)
        .collect(Collectors.toList()));
    });
  }
}
