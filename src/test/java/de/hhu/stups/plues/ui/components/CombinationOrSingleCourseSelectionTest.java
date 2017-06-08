package de.hhu.stups.plues.ui.components;

import static org.testfx.api.FxToolkit.setupStage;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.ui.UiTestDataCreator;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

@RunWith(JUnit4.class)
public class CombinationOrSingleCourseSelectionTest extends ApplicationTest {
  private CombinationOrSingleCourseSelection courseSelection;
  private ObservableList<Course> courseList = UiTestDataCreator.createCourseList();

  @Test
  public void enabledTest() {
    // check radio button's disable/enable logic
    clickOn(courseSelection.getRbCombination());
    Assert.assertTrue(courseSelection.getRbCombination().isSelected());
    Assert.assertFalse(courseSelection.getRbSingleSelection().isSelected());
    Assert.assertFalse(courseSelection.getMajorMinorCourseSelection().isDisabled());
    Assert.assertTrue(courseSelection.getSingleCourseSelection().isDisabled());

    clickOn(courseSelection.getRbSingleSelection());
    Assert.assertFalse(courseSelection.getRbCombination().isSelected());
    Assert.assertTrue(courseSelection.getRbSingleSelection().isSelected());
    Assert.assertTrue(courseSelection.getMajorMinorCourseSelection().isDisabled());
    Assert.assertFalse(courseSelection.getSingleCourseSelection().isDisabled());

    // minor selection disabled for integrated courses and enabled for combinable ones
    clickOn(courseSelection.getRbCombination());
    clickOn(courseSelection.getMajorMinorCourseSelection().getMajorComboBox())
        .type(KeyCode.DOWN)
        .type(KeyCode.ENTER);
    Assert.assertTrue(courseSelection.getMajorMinorCourseSelection()
        .getMinorComboBox().isDisabled());

    clickOn(courseSelection.getMajorMinorCourseSelection().getMajorComboBox())
        .type(KeyCode.DOWN)
        .type(KeyCode.ENTER);
    Assert.assertFalse(courseSelection.getMajorMinorCourseSelection()
        .getMinorComboBox().isDisabled());
  }

  @Test
  public void selectionTest() {
    clickOn(courseSelection.getRbCombination());
    clickOn(courseSelection.getMajorMinorCourseSelection().getMajorComboBox())
        .type(KeyCode.DOWN)
        .type(KeyCode.DOWN)
        .type(KeyCode.ENTER);
    clickOn(courseSelection.getMajorMinorCourseSelection().getMinorComboBox())
        .type(KeyCode.DOWN)
        .type(KeyCode.DOWN)
        .type(KeyCode.DOWN)
        .type(KeyCode.ENTER);
    Assert.assertEquals(courseList.get(4), courseSelection.getSelectedCourses().get(0));
    Assert.assertEquals(courseList.get(7), courseSelection.getSelectedCourses().get(1));

    clickOn(courseSelection.getMajorMinorCourseSelection().getMajorComboBox())
        .type(KeyCode.DOWN)
        .type(KeyCode.ENTER);
    clickOn(courseSelection.getMajorMinorCourseSelection().getMinorComboBox())
        .type(KeyCode.UP)
        .type(KeyCode.ENTER);
    Assert.assertEquals(courseList.get(8), courseSelection.getSelectedCourses().get(0));
    Assert.assertEquals(courseList.get(5), courseSelection.getSelectedCourses().get(1));

    clickOn(courseSelection.getRbSingleSelection());
    clickOn(courseSelection.getSingleCourseSelection())
        .type(KeyCode.DOWN)
        .type(KeyCode.ENTER);
    Assert.assertEquals(courseList.get(1), courseSelection.getSelectedCourses().get(0));

    clickOn(courseSelection.getSingleCourseSelection())
        .type(KeyCode.DOWN)
        .type(KeyCode.DOWN)
        .type(KeyCode.DOWN)
        .type(KeyCode.ENTER);
    Assert.assertEquals(courseList.get(4), courseSelection.getSelectedCourses().get(0));

    clickOn(courseSelection.getSingleCourseSelection())
        .type(KeyCode.UP)
        .type(KeyCode.UP)
        .type(KeyCode.ENTER);
    Assert.assertEquals(courseList.get(2), courseSelection.getSelectedCourses().get(0));
  }

  @After
  public void cleanup() throws Exception {
    WaitForAsyncUtils.waitForFxEvents();
    setupStage(Stage::close);
  }

  @Override
  public void start(final Stage stage) throws Exception {
    final FXMLLoader loader = new FXMLLoader();
    loader.setBuilderFactory(type -> {
      if (type.equals(MajorMinorCourseSelection.class)) {
        return () -> new MajorMinorCourseSelection(new Inflater(new FXMLLoader()));
      }
      return new JavaFXBuilderFactory().getBuilder(type);
    });

    final Inflater inflater = new Inflater(loader);

    courseSelection = new CombinationOrSingleCourseSelection(inflater);
    courseSelection.setCourses(courseList);

    final Scene scene = new Scene(courseSelection, 400, 300);
    stage.setScene(scene);
    stage.show();
  }
}
