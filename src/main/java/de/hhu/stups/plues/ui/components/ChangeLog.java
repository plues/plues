package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.Helpers;
import de.hhu.stups.plues.ObservableStore;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Log;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.net.URL;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;

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
  private TableColumn<Log, String> tableColumnSourceDayTemporary;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Log, String> tableColumnSourceTimeTemporary;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Log, String> tableColumnTargetDayTemporary;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Log, String> tableColumnTargetTimeTemporary;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Log, Date> tableColumnDateTemporary;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Log, Session> tableColumnSessionPersistent;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Log, String> tableColumnSourceDayPersistent;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Log, String> tableColumnSourceTimePersistent;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Log, String> tableColumnTargetDayPersistent;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Log, String> tableColumnTargetTimePersistent;
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

    inflater.inflate("components/ChangeLog", this, this, "ChangeLog", "Days");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    final Callback<TableColumn.CellDataFeatures<Log, String>, ObservableValue<String>>
        srcDayColumnCallback = param -> new ReadOnlyStringWrapper(
        resources.getString(param.getValue().getSrcDay()));
    final Callback<TableColumn.CellDataFeatures<Log, String>, ObservableValue<String>>
        srcTimeColumnCallback = param -> new ReadOnlyStringWrapper(
        Helpers.timeMap.get(param.getValue().getSrcTime()));

    final Callback<TableColumn.CellDataFeatures<Log, String>, ObservableValue<String>>
        targetDayColumnCallback = param -> new ReadOnlyStringWrapper(
        resources.getString(param.getValue().getTargetDay()));
    final Callback<TableColumn.CellDataFeatures<Log, String>, ObservableValue<String>>
        targetTimeColumnCallback = param -> new ReadOnlyStringWrapper(
        Helpers.timeMap.get(param.getValue().getTargetTime()));

    tableColumnSessionTemporary.setCellValueFactory(new PropertyValueFactory<>("session"));
    tableColumnSourceDayTemporary.setCellValueFactory(srcDayColumnCallback);
    tableColumnSourceTimeTemporary.setCellValueFactory(srcTimeColumnCallback);
    tableColumnTargetDayTemporary.setCellValueFactory(targetDayColumnCallback);
    tableColumnTargetTimeTemporary.setCellValueFactory(targetTimeColumnCallback);
    tableColumnDateTemporary.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

    tableColumnSessionPersistent.setCellValueFactory(new PropertyValueFactory<>("session"));
    tableColumnSourceDayPersistent.setCellValueFactory(srcDayColumnCallback);
    tableColumnSourceTimePersistent.setCellValueFactory(srcTimeColumnCallback);
    tableColumnTargetDayPersistent.setCellValueFactory(targetDayColumnCallback);
    tableColumnTargetTimePersistent.setCellValueFactory(targetTimeColumnCallback);
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
    tableColumnSourceDayTemporary.prefWidthProperty().bind(
        tempTable.widthProperty().multiply(0.075));
    tableColumnSourceTimeTemporary.prefWidthProperty().bind(
        tempTable.widthProperty().multiply(0.075));
    tableColumnTargetDayTemporary.prefWidthProperty().bind(
        tempTable.widthProperty().multiply(0.075));
    tableColumnTargetTimeTemporary.prefWidthProperty().bind(
        tempTable.widthProperty().multiply(0.075));
    tableColumnDateTemporary.prefWidthProperty().bind(
        tempTable.widthProperty().multiply(0.2));
    tableColumnSessionPersistent.prefWidthProperty().bind(
        tempTable.widthProperty().multiply(0.46));
    tableColumnSourceDayPersistent.prefWidthProperty().bind(
        tempTable.widthProperty().multiply(0.075));
    tableColumnSourceTimePersistent.prefWidthProperty().bind(
        tempTable.widthProperty().multiply(0.075));
    tableColumnTargetDayPersistent.prefWidthProperty().bind(
        tempTable.widthProperty().multiply(0.075));
    tableColumnTargetTimePersistent.prefWidthProperty().bind(
        tempTable.widthProperty().multiply(0.075));
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
    final SortedList<Log> sortedList
        = logs.sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()));

    final FilteredList<Log> persistentList = new FilteredList<>(sortedList);
    final FilteredList<Log> tempList = new FilteredList<>(sortedList);

    persistentList.predicateProperty().bind(Bindings.createObjectBinding(
        () -> log -> log.getCreatedAt().compareTo(compare.get()) < 0, compare));
    tempList.predicateProperty().bind(Bindings.createObjectBinding(
        () -> log -> log.getCreatedAt().compareTo(compare.get()) > 0, compare));

    getPersistentTable().itemsProperty().bind(new SimpleListProperty<>(persistentList));
    getTempTable().itemsProperty().bind(new SimpleListProperty<>(tempList));
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
