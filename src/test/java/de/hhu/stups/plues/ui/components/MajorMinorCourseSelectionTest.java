package de.hhu.stups.plues.ui.components;

import static org.testfx.api.FxToolkit.setupStage;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.ui.UiTestDataCreator;
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

import java.util.Set;
import java.util.stream.Collectors;

@RunWith(JUnit4.class)
public class MajorMinorCourseSelectionTest extends ApplicationTest {
  private MajorMinorCourseSelection courseSelection;
  private ObservableList<Course> courseList = UiTestDataCreator.createCourseList();

  @Test
  public void initialisationTest() {
    Assert.assertFalse(courseSelection.getMinorComboBox().isDisabled());
    Assert.assertEquals(courseList.get(0), courseSelection.getSelectedMajor());
    Assert.assertEquals(5, courseSelection.getMajorComboBox().getItems().size());

    Assert.assertTrue(courseSelection.getSelectedMajor().isCombinable());
    Assert.assertEquals(5, courseSelection.getMinorComboBox().getItems().size());

    final Set<Course> minorCourses = courseList.get(0).getMinorCourses();
    Assert.assertEquals(courseList.stream().filter(Course::isMajor).collect(Collectors.toList()),
        courseSelection.getMajorComboBox().getItems());
    Assert.assertTrue(courseSelection.getMinorComboBox().getItems()
        .containsAll(courseList.filtered(minorCourses::contains)));
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
    Assert.assertEquals(courseList.get(4), major);
    final Course minor = courseSelection.getSelectedMinor();
    Assert.assertEquals(courseList.get(2), minor);

    Assert.assertEquals(5, courseSelection.getMajorComboBox().getItems().size());
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

    Assert.assertFalse(courseSelection.getMajorComboBox().isDisabled());
    Assert.assertTrue(courseSelection.getMinorComboBox().isDisabled());
    Assert.assertNull(courseSelection.getSelectedMinor());

    Assert.assertEquals(5, courseSelection.getMajorComboBox().getItems().size());
    Assert.assertEquals(5, courseSelection.getMinorComboBox().getItems().size());
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

    courseSelection.setMajorCourseList(courseList);

    final Scene scene = new Scene(this.courseSelection, 100, 100);

    stage.setScene(scene);
    stage.show();
  }
}
