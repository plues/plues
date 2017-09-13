package de.hhu.stups.plues.ui.components;

import static org.testfx.api.FxToolkit.setupStage;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.ui.UiTestDataCreator;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TitledPane;
import javafx.stage.Stage;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class SetOfCourseSelectionTest extends ApplicationTest {
  private SetOfCourseSelection courseSelection;
  private final ObservableList<Course> courseList = UiTestDataCreator.createCourseList();

  private List<Node> masterCheckBoxes;
  private List<Node> bachelorCheckBoxes;

  @Test
  public void selectionTest() throws InterruptedException {
    updateBachelorCheckBoxes();
    Assert.assertTrue(maxOnePaneExpanded());
    Assert.assertTrue(courseSelection.getSelectedCourses().isEmpty());

    clickOn(bachelorCheckBoxes.get(0));
    clickOn(bachelorCheckBoxes.get(1));
    selectMasterPane();
    Assert.assertTrue(maxOnePaneExpanded());
    clickOn(masterCheckBoxes.get(0));

    Assert.assertEquals(3, courseSelection.getSelectedCourses().size());
    Assert.assertTrue(courseSelection.getSelectedCourses().contains(courseList.get(0)));
    Assert.assertTrue(courseSelection.getSelectedCourses().contains(courseList.get(1)));
    Assert.assertTrue(courseSelection.getSelectedCourses().contains(courseList.get(6)));
    selectBachelorPane();
    Assert.assertTrue(maxOnePaneExpanded());
    clickOn(bachelorCheckBoxes.get(1));

    Assert.assertEquals(2, courseSelection.getSelectedCourses().size());
    Assert.assertTrue(courseSelection.getSelectedCourses().contains(courseList.get(0)));
    Assert.assertTrue(courseSelection.getSelectedCourses().contains(courseList.get(6)));

    Assert.assertTrue(maxOnePaneExpanded());
    clickOn(bachelorCheckBoxes.get(0));
    selectMasterPane();
    clickOn(masterCheckBoxes.get(0));
    Assert.assertTrue(courseSelection.getSelectedCourses().isEmpty());

    masterCheckBoxes.forEach(item -> clickOn(item));
    selectBachelorPane();
    Assert.assertTrue(maxOnePaneExpanded());
    bachelorCheckBoxes.forEach(item -> clickOn(item));
    Assert.assertTrue(courseSelection.getSelectedCourses().equals(courseList));
  }

  @Test
  public void testClearSelection() throws InterruptedException {
    updateBachelorCheckBoxes();
    Assert.assertTrue(maxOnePaneExpanded());
    final Button btClearSelection = lookup("#btClearSelection").query();
    clickOn(bachelorCheckBoxes.get(0));
    clickOn(bachelorCheckBoxes.get(1));
    selectMasterPane();
    Assert.assertTrue(maxOnePaneExpanded());
    clickOn(masterCheckBoxes.get(0));
    Assert.assertFalse(courseSelection.getSelectedCourses().isEmpty());
    clickOn(btClearSelection);
    Assert.assertTrue(courseSelection.getSelectedCourses().isEmpty());

    selectBachelorPane();
    Assert.assertTrue(maxOnePaneExpanded());
    clickOn(bachelorCheckBoxes.get(3));
    Assert.assertFalse(courseSelection.getSelectedCourses().isEmpty());
    clickOn(btClearSelection);
    Assert.assertTrue(courseSelection.getSelectedCourses().isEmpty());

    clickOn(btClearSelection);
    Assert.assertTrue(courseSelection.getSelectedCourses().isEmpty());
  }

  private void selectBachelorPane() throws InterruptedException {
    final TitledPane titledPaneBachelorCourses = lookup("#titledPaneBachelorCourse").query();
    clickOn(titledPaneBachelorCourses);
    TimeUnit.MILLISECONDS.sleep(250);
    updateBachelorCheckBoxes();
  }

  private void selectMasterPane() throws InterruptedException {
    final TitledPane titledPaneMasterCourses = lookup("#titledPaneMasterCourse").query();
    clickOn(titledPaneMasterCourses);
    TimeUnit.MILLISECONDS.sleep(250);
    updateMasterCheckBoxes();
  }

  private void updateBachelorCheckBoxes() {
    bachelorCheckBoxes = new ArrayList<>(
        courseSelection.getTableViewBachelorCourse().lookupAll(".check-box"));
  }

  private void updateMasterCheckBoxes() {
    masterCheckBoxes = new ArrayList<>(
        courseSelection.getTableViewMasterCourse().lookupAll(".check-box"));
  }

  private boolean maxOnePaneExpanded() {
    final TitledPane titledPaneMasterCourses = lookup("#titledPaneMasterCourse").query();
    final TitledPane titledPaneBachelorCourses = lookup("#titledPaneBachelorCourse").query();
    return titledPaneMasterCourses.isExpanded() != titledPaneBachelorCourses.isExpanded();
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
