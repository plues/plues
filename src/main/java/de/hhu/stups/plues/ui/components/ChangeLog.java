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
  private TableView<Log> persistentTable;

  @FXML
  private TableColumn<Log, Session> sessionP;

  @FXML
  private TableColumn<Log, String> sourceP;

  @FXML
  private TableColumn<Log, String> targetP;

  @FXML
  private TableColumn<Log, Date> dateP;

  @FXML
  private TableView<Log> tempTable;

  @FXML
  private TableColumn<Log, Session> sessionT;

  @FXML
  private TableColumn<Log, String> sourceT;

  @FXML
  private TableColumn<Log, String> targetT;

  @FXML
  private TableColumn<Log, Date> dateT;

  /**
   * Constructor to create the change log.
   * @param inflater Inflater to handle fxml.
   * @param delayedStore Store which contains data
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
    sessionP.setCellValueFactory(new PropertyValueFactory<>("session"));
    sourceP.setCellValueFactory(new PropertyValueFactory<>("src"));
    targetP.setCellValueFactory(new PropertyValueFactory<>("target"));
    dateP.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

    sessionT.setCellValueFactory(new PropertyValueFactory<>("session"));
    sourceT.setCellValueFactory(new PropertyValueFactory<>("src"));
    targetT.setCellValueFactory(new PropertyValueFactory<>("target"));
    dateT.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

    updateBinding();

    delayedStore.whenAvailable(store -> {
      store.addObserver(this);
      logs.addAll(store.getLogEntries());
    });
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
}
