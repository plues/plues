package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Unit;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Set;

public class GroupsWithConflicts extends VBox implements Initializable {

  private final ListProperty<Unit> unitsForGroupsWithConflicts =
      new SimpleListProperty<>(FXCollections.observableArrayList());

  @FXML
  @SuppressWarnings("unused")
  private ListView<Unit> listViewUnitsForGroups;

  @Inject
  public GroupsWithConflicts(final Inflater inflater) {
    inflater.inflate("/components/reports/GroupsWithConflicts",
        this, this, "reports", "Column");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    listViewUnitsForGroups.itemsProperty().bind(unitsForGroupsWithConflicts);
    listViewUnitsForGroups.setCellFactory(param -> new ListCell<Unit>() {
      @Override
      protected void updateItem(final Unit unit, final boolean empty) {
        super.updateItem(unit, empty);
        if (!empty) {
          setText(unit.getTitle());
        }
      }
    });
  }

  public void setData(final Set<Unit> unitsForGroupsWithConflicts) {
    this.unitsForGroupsWithConflicts.addAll(
        FXCollections.observableArrayList(new ArrayList<>(unitsForGroupsWithConflicts)));
  }
}
