package de.hhu.stups.plues.ui.components.timetable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import java.util.stream.IntStream;

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
    final ObservableSet<Integer> selection1 = semesterChooser.getSelectedSemesters();
    assertEquals(0, selection1.size());


    clickOn(semesterChooser.getButtons().get(0))
      .type(KeyCode.SPACE)
      .type(KeyCode.TAB)
      .type(KeyCode.TAB)
      .type(KeyCode.CONTROL, KeyCode.SPACE);

    final ObservableSet<Integer> selection2 = semesterChooser.getSelectedSemesters();

    assertEquals(2, selection2.size());
    assertTrue(selection2.contains(1));
    assertTrue(selection2.contains(3));
  }

  @Test
  public void testSingleSelectionKeyboard() throws Exception {
    final ObservableSet<Integer> selection1 = semesterChooser.getSelectedSemesters();
    assertEquals(0, selection1.size());

    clickOn(semesterChooser)
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
    final ObservableSet<Integer> selection1 = semesterChooser.getSelectedSemesters();
    assertEquals(0, selection1.size());

    clickOn(semesterChooser.getButtons().get(2));

    final ObservableSet<Integer> selection2 = semesterChooser.getSelectedSemesters();

    assertTrue(selection2.contains(3));
    assertEquals(1, selection2.size());
  }

  @Test
  public void testMultipleSelectionMouse() throws Exception {
    final ObservableSet<Integer> selection1 = semesterChooser.getSelectedSemesters();
    assertEquals(0, selection1.size());

    clickOn(semesterChooser.getButtons().get(2))
        .type(KeyCode.CONTROL)
        .clickOn(semesterChooser.getButtons().get(4))
        .type(KeyCode.CONTROL)
        .clickOn(semesterChooser.getButtons().get(0));

    final ObservableSet<Integer> selection2 = semesterChooser.getSelectedSemesters();
    assertEquals(3, selection2.size());
    assertTrue(selection2.contains(1));
    assertTrue(selection2.contains(3));
    assertTrue(selection2.contains(5));
  }

  @Override
  public void start(final Stage stage) throws Exception {
    final ObservableList<ToggleButton> buttons = FXCollections.observableArrayList();
    IntStream.rangeClosed(1, 6).forEachOrdered(value -> {
      final String text = String.valueOf(value);
      final ToggleButton button = new ToggleButton(text);
      button.setUserData(text);
      buttons.add(button);
    });
    this.semesterChooser
        = new SemesterChooser(buttons);

    stage.setScene(new Scene(semesterChooser));
    stage.show();
  }
}
