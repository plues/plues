package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.ObservableStore;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Log;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.ui.layout.Inflater;
import javafx.beans.binding.ListBinding;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ChangeLog extends VBox implements Initializable, Observer {

  private final Delayed<ObservableStore> delayedStore;
  private final ObservableList<Log> logs;
  private final ObjectProperty<Date> compare;

  @FXML
  @SuppressWarnings("unused")
  private TableView<Log> persistentTable;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Log, Session> tableColumnSessionTemporary;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Log, String> tableColumnSourceTemporary;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Log, String> tableColumnTargetTemporary;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Log, Date> tableColumnDateTemporary;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Log, Session> tableColumnSessionPersistent;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Log, String> tableColumnSourcePersistent;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Log, String> tableColumnTargetPersistent;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Log, Date> tableColumnDatePersistent;
  @FXML
  @SuppressWarnings("unused")
  private TableView<Log> tempTable;

  /**
   * Constructor to create the change log.
   */
  @Inject
  public ChangeLog(final Inflater inflater, final UiDataService uiDataService,
                   final Delayed<ObservableStore> delayedStore) {
    this.delayedStore = delayedStore;
    this.compare = uiDataService.lastSavedDateProperty();
    this.logs = FXCollections.observableArrayList();

    inflater.inflate("components/ChangeLog", this, this, "ChangeLog");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    tableColumnSessionTemporary.setCellValueFactory(new PropertyValueFactory<>("session"));
    tableColumnSourceTemporary.setCellValueFactory(new PropertyValueFactory<>("src"));
    tableColumnTargetTemporary.setCellValueFactory(new PropertyValueFactory<>("target"));
    tableColumnDateTemporary.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

    tableColumnSessionPersistent.setCellValueFactory(new PropertyValueFactory<>("session"));
    tableColumnSourcePersistent.setCellValueFactory(new PropertyValueFactory<>("src"));
    tableColumnTargetPersistent.setCellValueFactory(new PropertyValueFactory<>("target"));
    tableColumnDatePersistent.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

    updateBinding();

    delayedStore.whenAvailable(store -> {
      store.addObserver(this);
      logs.addAll(store.getLogEntries());
    });

    bindTableColumnsWidth();
  }

  private void bindTableColumnsWidth() {
    tableColumnSessionTemporary.prefWidthProperty().bind(
        tempTable.widthProperty().multiply(0.46));
    tableColumnSourceTemporary.prefWidthProperty().bind(
        tempTable.widthProperty().multiply(0.15));
    tableColumnTargetTemporary.prefWidthProperty().bind(
        tempTable.widthProperty().multiply(0.15));
    tableColumnDateTemporary.prefWidthProperty().bind(
        tempTable.widthProperty().multiply(0.2));
    tableColumnSessionPersistent.prefWidthProperty().bind(
        tempTable.widthProperty().multiply(0.46));
    tableColumnSourcePersistent.prefWidthProperty().bind(
        tempTable.widthProperty().multiply(0.15));
    tableColumnTargetPersistent.prefWidthProperty().bind(
        tempTable.widthProperty().multiply(0.15));
    tableColumnDatePersistent.prefWidthProperty().bind(
        tempTable.widthProperty().multiply(0.2));
  }

  @Override
  public void update(final Observable observable, final Object arg) {
    final Store store = (Store) observable;
    final Log log = store.getLastLogEntry();
    logs.add(log);
  }

  private void updateBinding() {
    final ListBinding<Log> persistentBinding = new ListBinding<Log>() {
      {
        bind(logs, compare);
      }

      @Override
      protected ObservableList<Log> computeValue() {
        return logs.stream()
          .filter(log -> log.getCreatedAt().compareTo(compare.get()) < 0)
          .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
          .collect(Collectors.toCollection(FXCollections::observableArrayList));
      }
    };

    final ListBinding<Log> tempBinding = new ListBinding<Log>() {
      {
        bind(logs, compare);
      }

      @Override
      protected ObservableList<Log> computeValue() {
        return logs.stream()
          .filter(log -> log.getCreatedAt().compareTo(compare.get()) > 0)
          .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
          .collect(Collectors.toCollection(FXCollections::observableArrayList));
      }
    };

    getPersistentTable().itemsProperty().bind(persistentBinding);
    getTempTable().itemsProperty().bind(tempBinding);
  }

  TableView<Log> getPersistentTable() {
    return persistentTable;
  }

  TableView<Log> getTempTable() {
    return tempTable;
  }

  public void dispose() {
    this.delayedStore.whenAvailable(store -> store.deleteObserver(this));
  }
}
