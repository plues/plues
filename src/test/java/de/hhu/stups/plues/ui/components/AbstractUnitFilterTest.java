package de.hhu.stups.plues.ui.components;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.stage.Stage;

import org.junit.Assert;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AbstractUnitFilterTest extends ApplicationTest {

  private final List<AbstractUnit> abstractUnits;
  private final AbstractUnit a1;
  private AbstractUnitFilter filter;

  /**
   * Default constructor.
   */
  public AbstractUnitFilterTest() {
    abstractUnits = new ArrayList<>();

    this.a1 = new AbstractUnit();
    a1.setTitle("Abstract Unit 1");
    a1.setKey("Key 1");
    final AbstractUnit a2 = new AbstractUnit();
    a2.setTitle("Abstract Unit 2");
    a2.setKey("Key 2");

    abstractUnits.addAll(Arrays.asList(a1, a2));
  }

  @Test
  public void testContent() {
    final TableView<AbstractUnitFilter.SelectableAbstractUnit> units = lookup("#units").query();
    for (final AbstractUnitFilter.SelectableAbstractUnit entry : units.getItems()) {
      boolean containsTitle = false;
      for (final AbstractUnit unit : abstractUnits) {
        if (unit.getTitle().equals(entry.getTitle())) {
          containsTitle = true;
          break;
        }
      }
      Assert.assertTrue(containsTitle);
    }
  }

  @Test
  public void testSelection() {
    TableView<AbstractUnitFilter.SelectableAbstractUnit> units = lookup("#units").query();
    final CheckBoxTableCell cb = (CheckBoxTableCell) units.lookup(".table-row-cell .table-cell");

    clickOn(cb);

    // only selected units
    clickOn((RadioButton) lookup("#selected").query());
    units = lookup("#units").query();
    Assert.assertEquals(1, units.getItems().size());
    Assert.assertEquals(abstractUnits.get(0).getTitle(), units.getItems().get(0).getTitle());

    // only not-selected units
    clickOn((RadioButton) lookup("#notSelected").query());
    units = lookup("#units").query();
    Assert.assertEquals(1, units.getItems().size());
    Assert.assertEquals(abstractUnits.get(1).getTitle(), units.getItems().get(0).getTitle());
  }

  @Test
  public void testSearchForTitle() {
    final TextField field = lookup("#query").query();
    field.setText("Abstract Unit 1");

    final TableView<AbstractUnitFilter.SelectableAbstractUnit> units = lookup("#units").query();
    Assert.assertEquals(1, units.getItems().size());
  }

  @Test
  public void testSearchForKey() {
    final TextField field = lookup("#query").query();
    field.setText("Key 1");

    final TableView<AbstractUnitFilter.SelectableAbstractUnit> units = lookup("#units").query();
    Assert.assertEquals(1, units.getItems().size());
  }

  @Test
  public void testLiveUpdate() {
    TableView<AbstractUnitFilter.SelectableAbstractUnit> units = lookup("#units").query();
    final CheckBoxTableCell cb = (CheckBoxTableCell) units.lookup(".table-row-cell .table-cell");

    clickOn(cb); // click on checkbox
    clickOn((RadioButton) lookup("#selected").query()); // filter by selected
    clickOn(cb); // click on checkbox again
    units = lookup("#units").query(); // collect units again

    Assert.assertEquals(0, units.getItems().size());

    clickOn((RadioButton) lookup("#notSelected").query());
    units = lookup("#units").query();
    Assert.assertEquals(2, units.getItems().size());
  }

  @Test
  public void testClearingSelection() {
    TableView<AbstractUnitFilter.SelectableAbstractUnit> units = lookup("#units").query();
    final CheckBoxTableCell cb = (CheckBoxTableCell) units.lookup(".table-row-cell .table-cell");
    clickOn(cb);
    clickOn((RadioButton) lookup("#selected").query());
    clickOn((Button) lookup("#clearSelection").query());
    units = lookup("#units").query();
    Assert.assertEquals(2, units.getItems().size());
    Assert.assertTrue(((RadioButton) lookup("#all").query()).isSelected());
  }


  @Test
  public void testSelectedItems() {
    Assert.assertEquals(0, filter.getSelectedAbstractUnits().size());

    final TableView<AbstractUnitFilter.SelectableAbstractUnit> units = lookup("#units").query();
    final CheckBoxTableCell cb = (CheckBoxTableCell) units.lookup(".table-row-cell .table-cell");
    clickOn(cb);

    final ObservableList<AbstractUnit> items = filter.getSelectedAbstractUnits();
    Assert.assertEquals(1, filter.getSelectedAbstractUnits().size());

    final AbstractUnit item = items.get(0);
    Assert.assertEquals(a1, item);

  }

  @Override
  @SuppressWarnings("unchecked")
  public void start(final Stage stage) throws Exception {
    final Inflater inflater = new Inflater(new FXMLLoader());
    this.filter = new AbstractUnitFilter(inflater);
    filter.setAbstractUnits(abstractUnits);

    final Scene scene = new Scene(filter, 700, 500);
    stage.setScene(scene);
    stage.show();
  }
}
