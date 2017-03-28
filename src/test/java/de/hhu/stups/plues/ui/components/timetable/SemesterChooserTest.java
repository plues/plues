package de.hhu.stups.plues.ui.components.timetable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SemesterChooserTest extends ApplicationTest {
  private SemesterChooser semesterChooser;

  @Before
  public void setUp() throws Exception {
    semesterChooser.getButtons().forEach(toggleButton -> toggleButton.setSelected(false));
  }

  @After
  public void tearDown() throws Exception {

  }

  @Test
  public void testMultipleSelectionKeyboard() throws Exception {
    Assume.assumeFalse("true".equals(System.getenv("TRAVIS")));

    final ObservableSet<Integer> selection1 = semesterChooser.getSelectedSemesters();
    assertEquals(0, selection1.size());

    clickOn(semesterChooser.getButtons().get(0))
        .type(KeyCode.SPACE)
        .type(KeyCode.SPACE)
        .type(KeyCode.TAB)
        .type(KeyCode.TAB)
        .press(KeyCode.CONTROL)
        .type(KeyCode.SPACE)
        .release(KeyCode.CONTROL);

    final ObservableSet<Integer> selection2 = semesterChooser.getSelectedSemesters();

    assertEquals(2, selection2.size());
    assertTrue(selection2.contains(1));
    assertTrue(selection2.contains(3));
  }

  @Test
  public void testSingleSelectionKeyboard() throws Exception {
    Assume.assumeFalse("true".equals(System.getenv("TRAVIS")));

    final ObservableSet<Integer> selection1 = semesterChooser.getSelectedSemesters();
    assertEquals(0, selection1.size());

    clickOn(semesterChooser.getButtons().get(0))
        .push(KeyCode.TAB)
        .push(KeyCode.TAB)
        .push(KeyCode.TAB)
        .push(KeyCode.SPACE);

    final ObservableSet<Integer> selection2 = semesterChooser.getSelectedSemesters();

    assertTrue(selection2.contains(4));
    assertEquals(1, selection2.size());
  }

  @Test
  public void testSingleSelectionMouse() throws Exception {
    Assume.assumeFalse("true".equals(System.getenv("TRAVIS")));

    final ObservableSet<Integer> selection1 = semesterChooser.getSelectedSemesters();
    assertEquals(0, selection1.size());

    clickOn(semesterChooser.getButtons().get(2));

    final ObservableSet<Integer> selection2 = semesterChooser.getSelectedSemesters();

    assertTrue(selection2.contains(3));
    assertEquals(1, selection2.size());
  }

  @Test
  public void testMultipleSelectionMouse() throws Exception {
    // don't run this test in headless mode since it fails for unknown reasons, nevertheless, the
    // test succeeds in a headful testing environment
    Assume.assumeFalse("true".equals(System.getenv("HEADLESS")));

    final ObservableSet<Integer> selection1 = semesterChooser.getSelectedSemesters();
    assertEquals(0, selection1.size());

    clickOn(semesterChooser.getButtons().get(2))
        .press(KeyCode.CONTROL)
        .clickOn(semesterChooser.getButtons().get(4))
        .clickOn(semesterChooser.getButtons().get(0))
        .release(KeyCode.CONTROL);

    final ObservableSet<Integer> selection2 = semesterChooser.getSelectedSemesters();
    assertEquals(3, selection2.size());
    assertTrue(selection2.contains(1));
    assertTrue(selection2.contains(3));
    assertTrue(selection2.contains(5));
  }


  @Test
  public void testSetSelectedSemester() throws Exception {
    final ObservableSet<Integer> semesters = FXCollections.observableSet(4);
    this.semesterChooser.setSelectedSemesters(semesters);
    final ObservableSet<Integer> chosenSemesters = semesterChooser.getSelectedSemesters();

    // set has expected value
    assertEquals(1, chosenSemesters.size());
    assertTrue(chosenSemesters.contains(4));

    // selected value
    final List<?> selectedButtons = this.semesterChooser.getButtons().stream()
        .filter(ToggleButton::isSelected)
        .map(Node::getUserData)
        .collect(Collectors.toList());

    assertEquals(1, selectedButtons.size());
    assertTrue(selectedButtons.contains("4"));
  }

  @Test
  public void testSetConflictedSemesters() throws Exception {
    final ObservableSet<Integer> semesters = FXCollections.observableSet(4, 5);
    this.semesterChooser.setConflictedSemesters(semesters);

    final List<?> markedButtons = this.semesterChooser.getButtons().stream()
        .filter(toggleButton -> toggleButton.getStyleClass().contains("conflicted-semester"))
        .map(Node::getUserData)
        .collect(Collectors.toList());

    assertEquals(2, markedButtons.size());
    assertTrue(markedButtons.containsAll(Arrays.asList("4", "5")));

    //
    this.semesterChooser.setConflictedSemesters(FXCollections.observableSet());

    final List<?> objects = this.semesterChooser.getButtons().stream()
        .filter(toggleButton -> toggleButton.getStyleClass().contains("conflicted-semester"))
        .map(Node::getUserData)
        .collect(Collectors.toList());

    assertEquals(0, objects.size());
  }

  @Test
  public void testSelectMultipleSemesters() throws Exception {
    final ObservableSet<Integer> semesters = FXCollections.observableSet(1, 3, 5);
    this.semesterChooser.setSelectedSemesters(semesters);
    final ObservableSet<Integer> chosenSemesters = semesterChooser.getSelectedSemesters();

    // set has expected value
    assertEquals(3, chosenSemesters.size());
    assertTrue(chosenSemesters.containsAll(Arrays.asList(1, 3, 5)));

    // set has expected value
    final List<?> selectedButtons = this.semesterChooser.getButtons().stream()
        .filter(ToggleButton::isSelected)
        .map(Node::getUserData)
        .collect(Collectors.toList());
    assertEquals(3, selectedButtons.size());
    assertTrue(selectedButtons.containsAll(Arrays.asList("1", "3", "5")));

  }

  @Override
  public void start(final Stage stage) throws Exception {
    this.semesterChooser = new SemesterChooser();
    semesterChooser.setSemesters(Arrays.asList(1, 2, 3, 4, 5, 6));

    stage.setScene(new Scene(semesterChooser));
    stage.show();
  }
}
