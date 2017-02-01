package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.ui.layout.Inflater;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class CheckBoxGroup extends VBox implements Initializable {

  private final Course course;
  private final Module module;
  private final HashMap<CheckBox, AbstractUnit> boxToUnit;

  @FXML
  @SuppressWarnings("unused")
  private CheckBox moduleBox;

  @FXML
  @SuppressWarnings("unused")
  private VBox unitsBox;

  /**
   * Constructor for a group of checkboxes for a given course, a given module and a list of abstract
   * units.
   *
   * @param inflater inflater to handle fxml
   * @param course   Given course. Should be a major or minor for the chosen course combination
   * @param module   Given module. Should be one inside major or minor course
   */
  @Inject
  public CheckBoxGroup(final Inflater inflater,
                       @Assisted final Course course,
                       @Assisted final Module module) {
    this.course = course;
    this.module = module;
    boxToUnit = new LinkedHashMap<>();

    for (final AbstractUnit abstractUnit : module.getAbstractUnits()) {
      boxToUnit.put(new CheckBox(), abstractUnit);
    }

    inflater.inflate("components/CheckBoxGroup", this, this);
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    final ObservableList<Node> children = unitsBox.getChildren();
    BooleanBinding allSelected = Bindings.createBooleanBinding(() -> true);

    for (final Map.Entry<CheckBox, AbstractUnit> entry : boxToUnit.entrySet()) {
      final CheckBox cb = entry.getKey();
      cb.setText(entry.getValue().getTitle());
      children.add(cb);

      allSelected = allSelected.and(cb.selectedProperty());
    }

    moduleBox.setText(module.getTitle());

    allSelected.addListener((observable, oldValue, newValue) -> moduleBox.setSelected(newValue));

    moduleBox.setOnAction(e ->
        children.forEach(box -> ((CheckBox) box).setSelected(moduleBox.isSelected())));
  }

  /**
   * Collect an observable list all selected abstract units.
   *
   * @return Selected abstract units
   */
  public ObservableList<AbstractUnit> getSelectedAbstractUnits() {
    return FXCollections.observableList(boxToUnit.entrySet().stream()
        .filter(entry -> entry.getKey().isSelected())
        .map(Map.Entry::getValue)
        .collect(Collectors.toList()));
  }

  /**
   * Return module if selected.
   *
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
   */
  public Course getCourse() {
    return course;
  }

  /**
   * Set binding value if a checkbox is selected.
   */
  public void setOnSelectionChanged(final BooleanProperty selectionChanged) {
    boxToUnit.forEach((checkBox, abstractUnit) -> checkBox.selectedProperty().addListener(
        (observable, oldValue, newValue) -> selectionChanged.set(true)));
  }
}
