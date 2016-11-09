package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.ObservableStore;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Log;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.ListBinding;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ChangeLog extends VBox implements Initializable, Observer {

  private final Delayed<ObservableStore> delayedStore;
  private SimpleObjectProperty<Date> compare;
  private ObservableStore store;

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

  /**
   * Constructor to create the change log.
   * @param inflater Inflater to handle fxml.
   * @param delayedStore Store which contains data
   */
  @Inject
  public ChangeLog(final Inflater inflater,
                   final Delayed<ObservableStore> delayedStore) {
    this.delayedStore = delayedStore;

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

    delayedStore.whenAvailable(store -> {
      this.store = store;
      this.store.addObserver(this);
      compare = new SimpleObjectProperty<>(
        new Date(ManagementFactory.getRuntimeMXBean().getStartTime()));
      updateBinding(store);
    });
  }

  @Override
  public void update(Observable observable, Object arg) {
    if (observable instanceof ObservableStore) {
      if ((Boolean) arg) {
        compare = new SimpleObjectProperty<>(new Date());
      }
      updateBinding((ObservableStore) observable);
    }
  }

  public void updateTimeStamp() {
    compare.set(new Date());
  }

  public void deleteObserver() {
    store.deleteObserver(this);
  }

  private void updateBinding(final ObservableStore store) {
    final SimpleListProperty<Log> logs =
        new SimpleListProperty<>(FXCollections.observableArrayList(store.getLogEntries()));

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

    persistentTable.itemsProperty().bind(persistentBinding);
    tempTable.itemsProperty().bind(tempBinding);
  }
}
