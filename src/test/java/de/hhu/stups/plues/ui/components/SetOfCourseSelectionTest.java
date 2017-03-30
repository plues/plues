package de.hhu.stups.plues.ui.components;

import static org.testfx.api.FxToolkit.setupStage;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import java.util.ArrayList;
import java.util.List;


public class SetOfCourseSelectionTest extends ApplicationTest {
  private SetOfCourseSelection courseSelection;
  private ObservableList<Course> courseList = UiTestHelper.createCourseList();

  private List<Node> masterCheckBoxes;
  private List<Node> bachelorCheckBoxes;

  @Test
  public void selectionTest() {
    masterCheckBoxes = new ArrayList<>(
        courseSelection.getTableViewMasterCourse().lookupAll(".check-box"));
    bachelorCheckBoxes = new ArrayList<>(
        courseSelection.getTableViewBachelorCourse().lookupAll(".check-box"));

    Assert.assertTrue(courseSelection.getSelectedCourses().isEmpty());

    clickOn(bachelorCheckBoxes.get(0));
    clickOn(bachelorCheckBoxes.get(1));

    clickOn(masterCheckBoxes.get(0));

    Assert.assertEquals(3, courseSelection.getSelectedCourses().size());
    Assert.assertTrue(courseSelection.getSelectedCourses().contains(courseList.get(0)));
    Assert.assertTrue(courseSelection.getSelectedCourses().contains(courseList.get(1)));
    Assert.assertTrue(courseSelection.getSelectedCourses().contains(courseList.get(6)));

    clickOn(bachelorCheckBoxes.get(1));

    Assert.assertEquals(2, courseSelection.getSelectedCourses().size());
    Assert.assertTrue(courseSelection.getSelectedCourses().contains(courseList.get(0)));
    Assert.assertTrue(courseSelection.getSelectedCourses().contains(courseList.get(6)));

    clickOn(bachelorCheckBoxes.get(0));
    clickOn(masterCheckBoxes.get(0));
    Assert.assertTrue(courseSelection.getSelectedCourses().isEmpty());

    masterCheckBoxes.forEach(item -> clickOn(item));
    bachelorCheckBoxes.forEach(item -> clickOn(item));
    Assert.assertTrue(courseSelection.getSelectedCourses().equals(courseList));
  }

  @Test
  public void testClearSelection() {
    masterCheckBoxes = new ArrayList<>(
        courseSelection.getTableViewMasterCourse().lookupAll(".check-box"));
    bachelorCheckBoxes = new ArrayList<>(
        courseSelection.getTableViewBachelorCourse().lookupAll(".check-box"));

    final Button btClearSelection = lookup("#btClearSelection").query();
    clickOn(bachelorCheckBoxes.get(0));
    clickOn(bachelorCheckBoxes.get(1));
    clickOn(masterCheckBoxes.get(0));
    Assert.assertFalse(courseSelection.getSelectedCourses().isEmpty());
    clickOn(btClearSelection);
    Assert.assertTrue(courseSelection.getSelectedCourses().isEmpty());

    clickOn(bachelorCheckBoxes.get(3));
    Assert.assertFalse(courseSelection.getSelectedCourses().isEmpty());
    clickOn(btClearSelection);
    Assert.assertTrue(courseSelection.getSelectedCourses().isEmpty());

    clickOn(btClearSelection);
    Assert.assertTrue(courseSelection.getSelectedCourses().isEmpty());
  }

  @After
  public void cleanup() throws Exception {
    WaitForAsyncUtils.waitForFxEvents();
    setupStage(Stage::close);
  }

  @Override
  public void start(final Stage stage) throws Exception {
    final Inflater inflater = new Inflater(new FXMLLoader());
    courseSelection = new SetOfCourseSelection(inflater);

    courseSelection.setCourses(courseList);

    final Scene scene = new Scene(courseSelection, 400, 700);

    stage.setScene(scene);
    stage.show();
  }
}
