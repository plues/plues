package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
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
   * @param loader FXML Loader to load fxml
   * @param course Given course. Should be a major or minor for the choosen course combination
   * @param module Given module. Should be one inside major or minor course
   */
  @Inject
  public CheckBoxGroup(final FXMLLoader loader,
                       @Assisted final Course course,
                       @Assisted final Module module) {
    this.course = course;
    this.module = module;
    boxToUnit = new LinkedHashMap<>();

    for (final AbstractUnit abstractUnit : module.getAbstractUnits()) {
      boxToUnit.put(new CheckBox(), abstractUnit);
    }

    loader.setLocation(getClass().getResource("/fxml/components/CheckBoxGroup.fxml"));

    loader.setRoot(this);
    loader.setController(this);

    try {
      loader.load();
    } catch (final IOException exception) {
      throw new RuntimeException(exception);
    }
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

    for (final Node b : unitsBox.getChildren()) {
      setMargin(b, new Insets(0,0,0,20));
    }
  }

  /**
   * Collect an observable list all selected abstract units.
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
