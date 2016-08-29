package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class CheckBoxGroup extends VBox implements Initializable {

  private final Course course;
  private final Module module;
  private HashMap<CheckBox, AbstractUnit> boxToUnit;

  @FXML
  @SuppressWarnings("unused")
  private TextField courseField;

  @FXML
  @SuppressWarnings("unused")
  private CheckBox moduleBox;

  @FXML
  @SuppressWarnings("unused")
  private VBox unitsBox;

  @Inject
  public CheckBoxGroup(FXMLLoader loader,
                       @Assisted Course course,
                       @Assisted Module module,
                       @Assisted List<AbstractUnit> units) {
    this.course = course;
    this.module = module;
    boxToUnit = new HashMap<>();

    for (AbstractUnit abstractUnit : units) {
      boxToUnit.put(new CheckBox(), abstractUnit);
    }

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
    courseField.setText(course.getFullName());
    final ObservableList<Node> children = unitsBox.getChildren();
    BooleanBinding allSelected =   Bindings.createBooleanBinding(() -> true);


    for (Map.Entry<CheckBox, AbstractUnit> entry : boxToUnit.entrySet()) {
      CheckBox cb = entry.getKey();
      cb.setText(entry.getValue().getTitle());
      children.add(cb);

      allSelected = allSelected.and(cb.selectedProperty());
    }

    moduleBox.setText(module.getTitle());

    allSelected.addListener((observable, oldValue, newValue) -> moduleBox.setSelected(newValue));

    moduleBox.setOnAction(e ->
      children.forEach(box -> ((CheckBox) box).setSelected(moduleBox.isSelected())));
  }
  public HashMap<CheckBox, AbstractUnit> getBoxToUnit() {
    return boxToUnit;
  }

  public Module getModule() {
    if (moduleBox.isSelected()) {
      return module;
    }
    return null;
  }
}
