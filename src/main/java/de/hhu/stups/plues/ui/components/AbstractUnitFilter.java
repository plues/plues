package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.ui.layout.Inflater;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;

public class AbstractUnitFilter extends GridPane implements Initializable {

  private final Delayed<Store> delayedStore;
  private final ToggleGroup group = new ToggleGroup();
  private Store store;

  @FXML
  @SuppressWarnings("unused")
  private RadioButton name;

  @FXML
  @SuppressWarnings("unused")
  private RadioButton semester;

  @FXML
  @SuppressWarnings("unused")
  private ListView<AbstractUnit> units;

  @FXML
  @SuppressWarnings("unused")
  private ListView<AbstractUnit> selectedItems;

  @Inject
  public AbstractUnitFilter(final Inflater inflater,
                            final Delayed<Store> store) {
    this.delayedStore = store;

    inflater.inflate("components/AbstractUnitFilter", this, this, "main");
  }

  @FXML
  public void sortByName() {
    System.out.println("Name");
    units.getItems().sort((o1, o2) -> o1.getTitle().compareTo(o2.getTitle()));
  }

  @FXML
  public void sortBySemester() {
    System.out.println("Semester");
  }

  @FXML
  public void resetSelection() {
    selectedItems.getItems().clear();
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    name.setToggleGroup(group);
    semester.setToggleGroup(group);

    delayedStore.whenAvailable(store -> {
      this.store = store;

      Callback<ListView<AbstractUnit>, ListCell<AbstractUnit>> callback = new Callback<ListView<AbstractUnit>, ListCell<AbstractUnit>>() {
        @Override
        public ListCell<AbstractUnit> call(ListView<AbstractUnit> param) {
          return new ListCell<AbstractUnit>() {
            @Override
            protected void updateItem(AbstractUnit unit, boolean bool) {
              super.updateItem(unit, bool);
              if (unit != null) {
                setText(unit.getTitle());
              }
            }
          };
        }
      };

      units.getItems().addAll(store.getAbstractUnits());
      units.setCellFactory(callback);
      units.setOnMouseClicked(event -> {
        if (!selectedItems.getItems().contains(units.getSelectionModel().getSelectedItem())) {
          selectedItems.getItems().add(units.getSelectionModel().property.getSelectedItem());
        }
      });

      selectedItems.setCellFactory(callback);
    });

  }
}
