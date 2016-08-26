package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.Unit;

import org.controlsfx.control.CheckTreeView;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

public class CheckBoxGroup extends VBox implements Initializable {

  private final Module module;
  private final List<Unit> units;

  @FXML
  @SuppressWarnings("unused")
  private CheckBox moduleBox;

  @FXML
  @SuppressWarnings("unused")
  private VBox unitsBox;

  @Inject
  public CheckBoxGroup(FXMLLoader loader,
                       @Assisted Module module,
                       @Assisted List<Unit> units) {
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
    List<BooleanProperty> bindings = new ArrayList<>();
    for (Unit u : units) {
      CheckBox cb = new CheckBox(u.getTitle());
      bindings.add(cb.selectedProperty());
      cb.selectedProperty().bind(moduleBox.selectedProperty());
      unitsBox.getChildren().add(cb);
    }

    CheckTreeView<CheckBox> view = new CheckTreeView<>();
    view.

    moduleBox.setText(module.getTitle());
    moduleBox.selectedProperty().bind(bindings.forEach(booleanProperty -> booleanProperty.get()));
  }
}
