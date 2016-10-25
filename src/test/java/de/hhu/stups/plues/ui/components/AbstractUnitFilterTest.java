package de.hhu.stups.plues.ui.components;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import org.junit.Assert;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AbstractUnitFilterTest extends ApplicationTest {

  private final List<AbstractUnit> abstractUnits;

  /**
   * Default constructor.
   */
  public AbstractUnitFilterTest() {
    abstractUnits = new ArrayList<>();

    final AbstractUnit a1 = new AbstractUnit();
    a1.setTitle("Abstract Unit 1");
    a1.setKey("Key 1");
    final AbstractUnit a2 = new AbstractUnit();
    a2.setTitle("Abstract Unit 2");
    a2.setKey("Key 2");

    abstractUnits.addAll(Arrays.asList(a1, a2));
  }

  @Test
  public void testContent() {
    final TableView<AbstractUnitFilter.RowEntry> units = lookup("#units").query();
    boolean containsTitle = false;
    for (AbstractUnitFilter.RowEntry entry : units.getItems()) {
      for (AbstractUnit unit : abstractUnits) {
        if (unit.getTitle().equals(entry.getTitle())) {
          containsTitle = true;
          break;
        }
      }
    }

    Assert.assertTrue(containsTitle);
  }

  @Test
  public void testSelection() {
    TableView<AbstractUnitFilter.RowEntry> units = lookup("#units").query();
    final CheckBox cb = units.getItems().get(0).getCheckbox();
    clickOn(cb);

    // only selected units
    clickOn((RadioButton) lookup("#selected").query());
    units = lookup("#units").query();
    Assert.assertEquals(1, units.getItems().size());
    Assert.assertEquals(abstractUnits.get(0), units.getItems().get(0));

    // only not-selected units
    clickOn((RadioButton) lookup("#notSelected").query());
    units = lookup("#units").query();
    Assert.assertEquals(1, units.getItems().size());
    Assert.assertEquals(abstractUnits.get(1), units.getItems().get(0));
  }

  @Test
  public void testSearch() {
    final TextField field = lookup("#query").query();
    field.setText("1");

    final TableView<AbstractUnitFilter.RowEntry> units = lookup("#units").query();
    Assert.assertEquals(1, units.getItems().size());
  }

  @Test
  public void testClearingSelection() {
    TableView<AbstractUnitFilter.RowEntry> units = lookup("#units").query();
    final CheckBox cb = units.getItems().get(0).getCheckbox();
    clickOn(cb);
    clickOn((RadioButton) lookup("#selected").query());
    clickOn((Button) lookup("#clearSelection").query());
    units = lookup("#units").query();
    Assert.assertEquals(2, units.getItems().size());
    Assert.assertTrue(((RadioButton) lookup("#all").query()).isSelected());
  }

  @Override
  @SuppressWarnings("unchecked")
  public void start(final Stage stage) throws Exception {
    final Inflater inflater = new Inflater(new FXMLLoader());
    final AbstractUnitFilter filter = new AbstractUnitFilter(inflater);
    filter.setAbstractUnits(abstractUnits);

    final Scene scene = new Scene(filter, 700, 500);
    stage.setScene(scene);
    stage.show();
  }
}