package de.hhu.stups.plues.ui.components.timetable;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Group;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.ModuleAbstractUnitSemester;
import de.hhu.stups.plues.data.entities.ModuleAbstractUnitType;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.data.entities.Unit;
import de.hhu.stups.plues.data.sessions.SessionFacade;
import de.hhu.stups.plues.ui.components.timetable.DetailView.CourseTableEntry;
import de.hhu.stups.plues.ui.layout.Inflater;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.internal.stubbing.answers.ThrowsException;
import org.testfx.framework.junit.ApplicationTest;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class DetailViewTest extends ApplicationTest {

  private final Store store;
  private final Session session;
  private final SessionFacade.Slot slot;
  private final Map<Course, List<ModuleAbstractUnitSemester>> courseMap;
  private final List<Course> courses;
  private final List<Module> modules;
  private final List<AbstractUnit> abstractUnits;

  /**
   * Test constructor.
   */
  public DetailViewTest() {
    store = mock(Store.class);

    slot = new SessionFacade.Slot(DayOfWeek.MONDAY, 8);
    final Unit unit = mock(Unit.class, new ThrowsException(new RuntimeException()));
    final Group group = mock(Group.class, new ThrowsException(new RuntimeException()));
    session = mock(Session.class, new ThrowsException(new RuntimeException()));

    final AbstractUnit au1 = mock(AbstractUnit.class, new ThrowsException(new RuntimeException()));
    final AbstractUnit au2 = mock(AbstractUnit.class, new ThrowsException(new RuntimeException()));
    final ModuleAbstractUnitType maut1
        = mock(ModuleAbstractUnitType.class, new ThrowsException(new RuntimeException()));
    final ModuleAbstractUnitType maut2
        = mock(ModuleAbstractUnitType.class, new ThrowsException(new RuntimeException()));
    final Module mod1 = mock(Module.class, new ThrowsException(new RuntimeException()));
    final Module mod2 = mock(Module.class, new ThrowsException(new RuntimeException()));
    final Course course1 = mock(Course.class, new ThrowsException(new RuntimeException()));
    final Course course2 = mock(Course.class, new ThrowsException(new RuntimeException()));

    this.courses = Arrays.asList(course1, course2);
    this.modules = Arrays.asList(mod1, mod2);
    this.abstractUnits = Arrays.asList(au1, au2);

    doReturn("Course 1").when(course1).getKey();
    doReturn("Course 2").when(course2).getKey();

    Arrays.asList(mod1, mod2).forEach(module -> {
      doReturn(new HashSet<>(Arrays.asList(course1, course2))).when(module).getCourses();
      doReturn(new HashSet<>(Arrays.asList(1, 2))).when(module).getSemestersForAbstractUnit(au1);
      doReturn(new HashSet<>(Arrays.asList(3, 4))).when(module).getSemestersForAbstractUnit(au2);
    });
    doReturn("Module 1").when(mod1).getTitle();
    doReturn(1).when(mod1).getId();
    doReturn("Module 2").when(mod2).getTitle();
    doReturn(2).when(mod2).getId();

    doReturn(mod1).when(maut1).getModule();
    doReturn('m').when(maut1).getType();

    doReturn(mod2).when(maut2).getModule();
    doReturn('e').when(maut2).getType();

    doReturn(group).when(session).getGroup();
    doReturn(unit).when(group).getUnit();
    doReturn(1025).when(group).getId();

    doReturn("Unit").when(unit).getTitle();
    doReturn(new HashSet<>(Arrays.asList(1, 2))).when(unit).getSemesters();

    doReturn(1).when(au1).getId();
    doReturn("AU-1").when(au1).getKey();
    doReturn(2).when(au2).getId();
    doReturn("AU-2").when(au2).getKey();

    doReturn(new HashSet<>(Arrays.asList(au1, au2))).when(unit).getAbstractUnits();

    doReturn(new HashSet<>(Arrays.asList(maut1, maut2))).when(au1).getModuleAbstractUnitTypes();
    doReturn(new HashSet<>(Arrays.asList(maut1, maut2))).when(au2).getModuleAbstractUnitTypes();

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
    Assert.assertEquals("MONDAY: 8", ((Label) sessionBox.getChildren().get(1)).getText());

    final HBox titleBox = lookup("#title").query();
    Assert.assertEquals(2, titleBox.getChildren().size());
    Assert.assertEquals("Unit", ((Label) titleBox.getChildren().get(1)).getText());

    final HBox groupBox = lookup("#group").query();
    Assert.assertEquals(2, groupBox.getChildren().size());
    Assert.assertEquals(1025,
        Integer.parseInt(((Label) groupBox.getChildren().get(1)).getText()));

    final HBox semestersBox = lookup("#semesters").query();
    Assert.assertEquals(2, semestersBox.getChildren().size());
    Assert.assertEquals("1, 2", ((Label) semestersBox.getChildren().get(1)).getText());
  }

  @Test
  public void testTableContent() {
    final TableView<CourseTableEntry> courseTable = lookup("#courseTable").query();
    courses.forEach(course -> modules.forEach(module -> abstractUnits.forEach(abstractUnit -> {
      final char type;
      final HashSet<Integer> semesters = new HashSet<>();
      if (module.getId() == 1) {
        type = 'm';
      } else {
        type = 'e';
      }
      if (abstractUnit.getId() == 1) {
        semesters.addAll(Arrays.asList(1, 2));
      } else {
        semesters.addAll(Arrays.asList(3, 4));
      }
      final CourseTableEntry cte
          = new CourseTableEntry(course, module, abstractUnit, semesters, type);
      assertThat(courseTable.getItems(), hasItem(cte));
    })));
  }

  @Override
  public void start(final Stage stage) throws Exception {
    final Inflater inflater = new Inflater(new FXMLLoader());

    final DetailView detailView = new DetailView(inflater);
    detailView.setContent(session, slot);

    final Scene scene = new Scene(detailView, 400, 250);
    stage.setScene(scene);
    stage.show();
  }
}
