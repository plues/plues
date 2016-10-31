package de.hhu.stups.plues.ui.components;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.Unit;
import de.hhu.stups.plues.ui.components.timetable.DetailView;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import org.junit.Assert;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class DetailViewTest extends ApplicationTest {

  private final Set<AbstractUnit> abstractUnitSet;
  private final Unit unit;
  private final HashMap<Course, Set<Module>> courseModuleHashMap;

  /**
   * Test constructor.
   */
  public DetailViewTest() {
    AbstractUnit a1 = new AbstractUnit();
    a1.setId(1);
    AbstractUnit a2 = new AbstractUnit();
    a2.setId(2);
    abstractUnitSet = new HashSet<>(Arrays.asList(a1, a2));

    unit = new Unit();
    unit.setTitle("Unit");

    courseModuleHashMap = new HashMap<>();
    Course c1 = new Course();
    c1.setKey("A-B-C-D");
    c1.setPo(2016);
    Course c2 = new Course();
    c2.setKey("E-F-G-H");
    c2.setPo(2016);
    Module m1 = new Module();
    m1.setId(10);
    Module m2 = new Module();
    m2.setId(20);
    courseModuleHashMap.put(c1, new HashSet<>(Arrays.asList(m1, m2)));
    courseModuleHashMap.put(c2, new HashSet<>(Arrays.asList(m1, m2)));
  }

  @Test
  public void testContentSize() {
    TableView sessionTable = lookup("#sessionTable").query();
    TableView courseTable = lookup("#courseTable").query();

    Assert.assertEquals(2, sessionTable.getItems().size());
    Assert.assertEquals(2, courseTable.getItems().size());
  }

  @Test
  public void testContentSessionTable() {
    HashMap<String, String> expectedContent = new HashMap<>();
    expectedContent.put("Unit", unit.getTitle());

    StringBuilder b1 = new StringBuilder();
    abstractUnitSet.forEach(abstractUnit -> {
      b1.append(abstractUnit.getId());
      b1.append(",");
    });
    b1.setLength(b1.length() - 1);
    expectedContent.put("Abstract Units", b1.toString());

    TableView sessionTable = lookup("#sessionTable").query();
    sessionTable.getItems().forEach(o -> {
      DetailView.SessionTableEnry entry = (DetailView.SessionTableEnry) o;
      Assert.assertTrue(expectedContent.containsKey(entry.getKey()));
      Assert.assertEquals(expectedContent.get(entry.getKey()), entry.getValue());
    });
  }

  @Test
  public void testContentCourseTable() {
    TableView courseTable = lookup("#courseTable").query();
    courseTable.getItems().forEach(o -> {
      DetailView.CourseTableEntry entry = (DetailView.CourseTableEntry) o;
      boolean courseContained = false;
      for (Course c : courseModuleHashMap.keySet()) {
        if (c.getPo().equals(entry.getPo()) && c.getKey().equals(entry.getCourseKey())) {
          courseContained = true;
          break;
        }
      }
      Assert.assertTrue(courseContained);

      for (Set<Module> modules : courseModuleHashMap.values()) {
        StringBuilder builder = new StringBuilder();
        modules.forEach(module -> {
          builder.append(module.getId());
          builder.append(",");
        });
        builder.setLength(builder.length() - 1);
        Assert.assertEquals(builder.toString(), entry.getModules());
      }
    });
  }

  @Override
  public void start(Stage stage) throws Exception {
    final Inflater inflater = new Inflater(new FXMLLoader());
    DetailView detailView = new DetailView(inflater);
    detailView.setContent(abstractUnitSet, unit, courseModuleHashMap);

    final Scene scene = new Scene(detailView, 450, 200);
    stage.setScene(scene);
    stage.show();
  }
}
