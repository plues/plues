package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Module;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

public class CheckBoxGroup extends VBox implements Initializable {

  private final Module module;
  private final List<AbstractUnit> units;

  @FXML
  @SuppressWarnings("unused")
  private CheckBox moduleBox;

  @FXML
  @SuppressWarnings("unused")
  private VBox unitsBox;

  @Inject
  public CheckBoxGroup(FXMLLoader loader,
                       @Assisted Module module,
                       @Assisted List<AbstractUnit> units) {
    this.module = module;
    this.units = units;

    loader.setLocation(getClass().getResource("/fxml/components/CheckBoxGroup.fxml"));

    loader.setRoot(this);
    loader.setController(this);

    try {
      loader.load();
    } catch (IOException exception) {
      throw new RuntimeException(exception);
    }
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    CheckBox[] list = new CheckBox[units.size()];
    for (int i=0; i<units.size(); i++) {
      CheckBox cb = new CheckBox();
      cb.setText(units.get(i).getTitle());
      cb.setSelected(moduleBox.isSelected());
      unitsBox.getChildren().add(cb);
      list[i] = cb;
    }

    moduleBox.setText(module.getTitle());
    BooleanBinding allSelected = Bindings.createBooleanBinding(() ->
      Stream.of(list).allMatch(CheckBox::isSelected),
      Stream.of(list).map(CheckBox::selectedProperty).toArray(Observable[]::new));

    allSelected.addListener((obs, wereAllSelected, areAllNowSelected) ->
      moduleBox.setSelected(areAllNowSelected));

    moduleBox.setOnAction(e ->
      Stream.of(list).forEach(box -> box.setSelected(moduleBox.isSelected())));
  }
}
