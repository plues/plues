package de.hhu.stups.plues.ui.components;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit.ApplicationTest;

import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

@RunWith(JUnit4.class)
public class CheckBoxGroupTest extends ApplicationTest {
  private Course major;

  public CheckBoxGroupTest() {
    major = new Course();
    major.setLongName("Major Course");
    major.setDegree("MA");
    major.setKzfa("H");
    major.setPo(2016);
  }

  @Test
  public void everythingUnchecked() {
    FxAssert.verifyThat("#moduleBox", input -> !((CheckBox) input).isSelected());
    VBox units = lookup("#unitsBox").query();
    units.getChildren().forEach(node -> Assert.assertFalse(((CheckBox) node).isSelected()));
  }

  @Test
  public void clickOnModule() {
    clickOn("#moduleBox");
    VBox units = lookup("#unitsBox").query();
    units.getChildren().forEach(node -> Assert.assertTrue(((CheckBox) node).isSelected()));
  }

  @Test
  public void clickOnUnits() {
    CheckBox module = lookup("#moduleBox").query();
    VBox units = lookup("#unitsBox").query();
    units.getChildren().forEach(node -> clickOn(node));
    Assert.assertTrue(module.isSelected());
  }

  @Override
  public void start(final Stage stage) throws Exception {
    Module majorModule = new Module();
    majorModule.setTitle("Major Module");
    AbstractUnit unit = new AbstractUnit();
    unit.setTitle("Unit");
    AbstractUnit unit2 = new AbstractUnit();
    unit2.setTitle("Unit 2");
    List<AbstractUnit> majorUnits = new ArrayList<>();
    majorUnits.add(unit2);
    majorUnits.add(unit);

    final CheckBoxGroup checkBoxGroup = new CheckBoxGroup(new FXMLLoader(),
      major, majorModule, majorUnits);

    final Scene scene = new Scene(checkBoxGroup, 200, 200);
    stage.setScene(scene);
    stage.show();
  }
}
