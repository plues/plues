package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

public class CheckBoxGroup extends VBox implements Initializable {

  private final Course course;
  private final Module module;
  private HashMap<CheckBox, AbstractUnit> boxToUnit;

  @FXML
  @SuppressWarnings("unused")
  private CheckBox moduleBox;

  @FXML
  @SuppressWarnings("unused")
  private VBox unitsBox;

  /**
   * Constructor for a group of checkboxes for a given course, a given module and a list of abstract
   * units.
   * @param loader FXML Loader to load fxml
   * @param course Given course. Should be a major or minor for the choosen course combination
   * @param module Given module. Should be one inside major or minor course
   * @param units A list of all abstract units of the given module
   */
  @Inject
  public CheckBoxGroup(FXMLLoader loader,
                       @Assisted Course course,
                       @Assisted Module module,
                       @Assisted Set<AbstractUnit> units) {
    this.course = course;
    this.module = module;
    boxToUnit = new LinkedHashMap<>();

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
    final ObservableList<Node> children = unitsBox.getChildren();
    BooleanBinding allSelected = Bindings.createBooleanBinding(() -> true);

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

    for (Node b : unitsBox.getChildren()) {
      unitsBox.setMargin(b, new Insets(0,0,0,20));
    }
  }

  public HashMap<CheckBox, AbstractUnit> getBoxToUnit() {
    return boxToUnit;
  }

  /**
   * Return module if selected.
   * @return Module if selected, else null
   */
  public Module getModule() {
    if (moduleBox.isSelected()) {
      return module;
    }
    return null;
  }

  /**
   * Get course of this module and units.
   * @return Course object
   */
  public Course getCourse() {
    return course;
  }
}
