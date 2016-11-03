package de.hhu.stups.plues.ui.components;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Group;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.ModuleAbstractUnitSemester;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.data.entities.Unit;
import de.hhu.stups.plues.data.sessions.SessionFacade;
import de.hhu.stups.plues.ui.components.timetable.DetailView;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import org.junit.Assert;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DetailViewTest extends ApplicationTest {

  private final MockStore store;
  private final Session session;
  private final SessionFacade.Slot slot;
  private final Group group;
  private final Unit unit;
  private final String semesterString;
  private final Map<Course, List<ModuleAbstractUnitSemester>> courseMap;

  /**
   * Test constructor.
   */
  public DetailViewTest() {
    store = new MockStore();
    try {
      store.init();
    } catch (Exception exception) {
      exception.printStackTrace();
    }

    slot = new SessionFacade.Slot(DayOfWeek.MONDAY, 8);
    unit = store.getUnits().get(0);
    group = store.getGroups().get(0);
    session = store.getSessions().get(0);

    final Set<Integer> semesters = new HashSet<>(Arrays.asList(1,2));
    final StringBuilder builder = new StringBuilder();
    semesters.forEach(integer -> {
      builder.append(integer);
      builder.append(",");
    });
    if (builder.length() > 0) {
      builder.setLength(builder.length() - 1);
    }
    semesterString = builder.toString();

    courseMap = new HashMap<>();
    store.getCourses().forEach(course ->
        courseMap.put(course, store.getModuleAbstractUnitSemester()));
  }

  @Test
  public void testContentSize() {
    final TableView courseTable = lookup("#courseTable").query();
    Assert.assertEquals(8, courseTable.getItems().size());
  }

  @Test
  public void testSessionInfo() {
    final HBox sessionBox = lookup("#session").query();
    Assert.assertEquals(2, sessionBox.getChildren().size());
    Assert.assertEquals(slot.toString(), ((Label) sessionBox.getChildren().get(1)).getText());

    final HBox titleBox = lookup("#title").query();
    Assert.assertEquals(2, titleBox.getChildren().size());
    Assert.assertEquals(unit.getTitle(), ((Label) titleBox.getChildren().get(1)).getText());

    final HBox groupBox = lookup("#group").query();
    Assert.assertEquals(2, groupBox.getChildren().size());
    Assert.assertEquals(group.getId(),
        Integer.parseInt(((Label) groupBox.getChildren().get(1)).getText()));

    final HBox semestersBox = lookup("#semesters").query();
    Assert.assertEquals(2, semestersBox.getChildren().size());
    Assert.assertEquals(semesterString, ((Label) semestersBox.getChildren().get(1)).getText());
  }

  @Test
  public void testTableContent() {
    final TableView courseTable = lookup("#courseTable").query();
    final HashMap<AbstractUnit, Character> expectedType = store.getExpectedType();

    courseMap.forEach((course, moduleAbstractUnitSemesters) ->
        moduleAbstractUnitSemesters.forEach(moduleAbstractUnitSemester -> {
          final AbstractUnit abstractUnit = moduleAbstractUnitSemester.getAbstractUnit();
          final Module module = moduleAbstractUnitSemester.getModule();
          final Integer semester = moduleAbstractUnitSemester.getSemester();

          boolean containsEntry = false;

          for (Object o : courseTable.getItems()) {
            final DetailView.CourseTableEntry entry = (DetailView.CourseTableEntry) o;
            if (entry.getCourseKey().equals(course.getKey())
                && entry.getAbstractUnit() == abstractUnit.getId()
                && entry.getSemesters().contains(semester.toString())
                && entry.getType().equals(expectedType.get(abstractUnit))
                && entry.getModule().equals(module.getTitle())) {
              containsEntry = true;
              break;
            }
          }

          Assert.assertTrue(containsEntry);
        }));
  }

  @Override
  public void start(Stage stage) throws Exception {
    final Inflater inflater = new Inflater(new FXMLLoader());

    final Delayed<Store> delayed = new Delayed<>();
    delayed.set(store);
    final DetailView detailView = new DetailView(inflater, delayed);
    detailView.setContent(session, slot);

    final Scene scene = new Scene(detailView, 400, 250);
    stage.setScene(scene);
    stage.show();
  }
}
