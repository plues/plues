package de.hhu.stups.plues.ui.components;

import static org.testfx.api.FxToolkit.setupStage;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.ui.UiTestHelper;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
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
public class MajorMinorCourseSelectionTest extends ApplicationTest {
  private MajorMinorCourseSelection courseSelection;
  private ObservableList<Course> courseList = UiTestHelper.createCourseList();

  @Test
  public void initialisationTest() {

    Assert.assertFalse(courseSelection.getMinorComboBox().isDisabled());
    Assert.assertEquals(courseList.get(0), courseSelection.getSelectedMajor());
    Assert.assertEquals(10, courseSelection.getMajorComboBox().getItems().size());

    Assert.assertTrue(courseSelection.getSelectedMajor().isCombinable());
    Assert.assertEquals(5, courseSelection.getMinorComboBox().getItems().size());

    Assert.assertEquals(courseList, courseSelection.getMajorComboBox().getItems());
    Assert.assertEquals(courseList.filtered(
            course -> course.isCombinableWith(courseList.get(0))),
        courseSelection.getMinorComboBox().getItems());
  }

  @Test
  public void selectionTestCombinableCourse() {
    // select combinable course
    clickOn(courseSelection.getMajorComboBox())
        .type(KeyCode.DOWN)
        .type(KeyCode.DOWN)
        .type(KeyCode.ENTER);

    Assert.assertFalse(courseSelection.getMinorComboBox().isDisabled());

    final Course major = courseSelection.getSelectedMajor();
    Assert.assertEquals(major, courseList.get(2));
    final Course minor = courseSelection.getSelectedMinor();
    Assert.assertEquals(minor, courseList.get(0));

    Assert.assertEquals(10, courseSelection.getMajorComboBox().getItems().size());
    Assert.assertEquals(5, courseSelection.getMinorComboBox().getItems().size());

    final ObservableList<Course> courses = courseSelection.getSelectedCourses();
    Assert.assertEquals(2, courses.size());
    Assert.assertTrue(courses.contains(major));
    Assert.assertTrue(courses.contains(minor));

  }

  @Test
  public void selectionTestNonCombinableCourse() {
    // select not combinable course
    clickOn(courseSelection.getMajorComboBox())
        .type(KeyCode.DOWN)
        .type(KeyCode.ENTER);

    Assert.assertEquals(courseList.get(1), courseSelection.getSelectedMajor());

    Assert.assertTrue(courseSelection.getMinorComboBox().isDisabled());
    Assert.assertNull(courseSelection.getSelectedMinor());

    Assert.assertEquals(10, courseSelection.getMajorComboBox().getItems().size());
    Assert.assertEquals(0, courseSelection.getMinorComboBox().getItems().size());
  }

  @Test
  public void selectionStateWhenDisabled() {
    final Course major = courseSelection.getSelectedMajor();
    final Course minor = courseSelection.getSelectedMinor();
    final ObservableList<Course> courses = courseSelection.getSelectedCourses();

    Assert.assertNotNull(major);
    Assert.assertNotNull(minor);
    Assert.assertEquals(2, courses.size());

    courseSelection.setDisable(true);

    Assert.assertEquals(major, courseSelection.getSelectedMajor());
    Assert.assertEquals(minor, courseSelection.getSelectedMinor());
    Assert.assertEquals(courses, courseSelection.getSelectedCourses());

  }

  @After
  public void cleanup() throws Exception {
    WaitForAsyncUtils.waitForFxEvents();
    setupStage(Stage::close);
  }

  @Override
  public void start(final Stage stage) throws Exception {
    courseSelection = new MajorMinorCourseSelection(new Inflater(new FXMLLoader()));

    courseSelection.setMinorCourseList(courseList);
    courseSelection.setMajorCourseList(courseList);

    final Scene scene = new Scene(this.courseSelection, 100, 100);

    stage.setScene(scene);
    stage.show();
  }
}
