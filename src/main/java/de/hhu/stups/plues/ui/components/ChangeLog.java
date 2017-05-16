package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.Helpers;
import de.hhu.stups.plues.ObservableStore;
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
import org.reactfx.Subscription;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class ChangeLog extends VBox implements Initializable {

  private final Delayed<ObservableStore> delayedStore;
  private final ObservableList<Log> logs;
  private final ObjectProperty<LocalDateTime> compare;

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
  private TableColumn<Log, LocalDateTime> tableColumnDateTemporary;
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
  private TableColumn<Log, LocalDateTime> tableColumnDatePersistent;
  @FXML
  @SuppressWarnings("unused")
  private TableView<Log> tempTable;
  private Subscription subscriptions;

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
        srcColumnCallback = param -> new ReadOnlyStringWrapper(
        String.format("%s, %s", resources.getString(param.getValue().getSrcDay()),
            Helpers.timeMap.get(param.getValue().getSrcTime())));

    final Callback<TableColumn.CellDataFeatures<Log, String>, ObservableValue<String>>
        targetColumnCallback = param -> new ReadOnlyStringWrapper(
        String.format("%s, %s", resources.getString(param.getValue().getTargetDay()),
            Helpers.timeMap.get(param.getValue().getTargetTime())));

    tableColumnSessionTemporary.setCellValueFactory(new PropertyValueFactory<>("session"));
    tableColumnSourceTemporary.setCellValueFactory(srcColumnCallback);
    tableColumnTargetTemporary.setCellValueFactory(targetColumnCallback);
    tableColumnDateTemporary.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

    tableColumnSessionPersistent.setCellValueFactory(new PropertyValueFactory<>("session"));
    tableColumnSourcePersistent.setCellValueFactory(srcColumnCallback);
    tableColumnTargetPersistent.setCellValueFactory(targetColumnCallback);
    tableColumnDatePersistent.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

    updateBinding();

    delayedStore.whenAvailable(store -> {
      logs.addAll(store.getLogEntries());

      final Subscription removed = store.getChanges()
          .filter("removed"::equals)
          .subscribe(value -> logs.remove(logs.size() - 1));
      final Subscription added = store.getChanges()
          .filter(""::equals)
          .subscribe(value -> logs.add(store.getLastLogEntry()));
      subscriptions = added.and(removed);
    });
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

  /**
   * Free resources before closing the window containing this element.
   */
  public void dispose() {
    if (this.subscriptions != null) {
      this.subscriptions.unsubscribe();
    }
  }
}
