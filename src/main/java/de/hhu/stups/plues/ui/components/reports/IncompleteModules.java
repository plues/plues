package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.ui.layout.Inflater;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;

import javafx.fxml.Initializable;
import javafx.scene.control.ListView;

public class IncompleteModules extends ListView implements Initializable {

  private final Delayed<Store> delayedStore;
  private Store store;

  @Inject
  public IncompleteModules(final Inflater inflater,
                           final Delayed<Store> delayedStore) {
    this.delayedStore = delayedStore;

    inflater.inflate("/components/reports/IncompleteModules", this, this, "reports");
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    delayedStore.whenAvailable(store -> this.store = store);
  }

  public void setData(final Set<Integer> incompleteModules) {
    incompleteModules.forEach(id -> getItems().add(store.getModuleById(id).getTitle()));
  }
}
