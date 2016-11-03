package de.hhu.stups.plues.ui.components;

import de.hhu.stups.plues.data.IncompatibleSchemaError;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.StoreException;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Group;
import de.hhu.stups.plues.data.entities.Level;
import de.hhu.stups.plues.data.entities.Log;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.ModuleAbstractUnitSemester;
import de.hhu.stups.plues.data.entities.ModuleAbstractUnitType;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.data.entities.Unit;
import de.hhu.stups.plues.data.sessions.SessionFacade;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MockStore implements Store {

  private final Set<Integer> semesters = new HashSet<>(Arrays.asList(1,2));
  private HashMap<AbstractUnit, Character> expectedType;
  private HashMap<AbstractUnit, Set<Integer>> expectedSemesters;

  public HashMap<AbstractUnit, Character> getExpectedType() {
    return expectedType;
  }

  @Override
  public void init() throws IncompatibleSchemaError, StoreException {
    expectedType = new HashMap<>();
    expectedType.put(getAbstractUnits().get(0), 'm');
    expectedType.put(getAbstractUnits().get(1), 'e');

    expectedSemesters = new HashMap<>();
    getAbstractUnits().forEach(abstractUnit -> expectedSemesters.put(abstractUnit, semesters));
  }

  @Override
  public void init(String dbpath) throws IncompatibleSchemaError, StoreException {

  }

  @Override
  public void moveSession(SessionFacade sessionFacade, SessionFacade.Slot slot) {

  }

  @Override
  public void close() {

  }

  @Override
  public void clear() {

  }

  @Override
  public List getInfo() {
    return null;
  }

  @Override
  public List<AbstractUnit> getAbstractUnits() {
    AbstractUnit a1 = new AbstractUnit();
    a1.setId(1);
    AbstractUnit a2 = new AbstractUnit();
    a2.setId(2);

    return new ArrayList<>(Arrays.asList(a1, a2));
  }

  @Override
  public List<AbstractUnit> getAbstractUnitsWithoutUnits() {
    return null;
  }

  @Override
  public List<Course> getCourses() {
    Course c1 = new Course();
    c1.setKey("A-B-C-D");
    Course c2 = new Course();
    c2.setKey("E-F-G-H");

    return new ArrayList<>(Arrays.asList(c1, c2));
  }

  @Override
  public Course getCourseByKey(String key) {
    return null;
  }

  @Override
  public List<Group> getGroups() {
    Group group = new Group();
    group.setUnit(getUnits().get(0));
    return new ArrayList<>(Arrays.asList(group));
  }

  @Override
  public List<Level> getLevels() {
    return null;
  }

  @Override
  public List<Module> getModules() {
    Module m1 = new Module();
    m1.setTitle("Module 1");
    m1.setCourses(new HashSet<>(getCourses()));
    m1.setAbstractUnits(new HashSet<>(getAbstractUnits()));
    Module m2 = new Module();
    m2.setTitle("Module 2");
    m2.setAbstractUnits(new HashSet<>(getAbstractUnits()));
    m2.setCourses(new HashSet<>(getCourses()));

    return new ArrayList<>(Arrays.asList(m1, m2));
  }

  @Override
  public List<ModuleAbstractUnitSemester> getModuleAbstractUnitSemester() {
    List<ModuleAbstractUnitSemester> list = new ArrayList<>();
    getModules().forEach(module ->
        module.getAbstractUnits().forEach(abstractUnit -> {
          expectedSemesters.get(abstractUnit).forEach(semester -> {
            ModuleAbstractUnitSemester content = new ModuleAbstractUnitSemester();
            content.setAbstractUnit(abstractUnit);
            content.setModule(module);
            content.setSemester(semester);
            list.add(content);
          });
        }));

    return list;
  }

  @Override
  public List<ModuleAbstractUnitType> getModuleAbstractUnitType() {
    List<ModuleAbstractUnitType> list = new ArrayList<>();
    for (Map.Entry<AbstractUnit, Character> entry : expectedType.entrySet()) {
      ModuleAbstractUnitType type = new ModuleAbstractUnitType();
      type.setAbstractUnit(entry.getKey());
      type.setType(entry.getValue());
      list.add(type);
    }

    return list;
  }

  @Override
  public List<Session> getSessions() {
    Session session = new Session();
    session.setGroup(getGroups().get(0));
    session.setDay("mon");
    session.setTime(8);
    return new ArrayList<>(Arrays.asList(session));
  }

  @Override
  public List<Unit> getUnits() {
    Unit unit = new Unit();
    unit.setTitle("Unit");
    unit.setSemesters(semesters);
    return new ArrayList<>(Arrays.asList(unit));
  }

  @Override
  public Unit getUnitById(Integer uid) {
    return null;
  }

  @Override
  public AbstractUnit getAbstractUnitById(Integer key) {
    return null;
  }

  @Override
  public Group getGroupById(Integer integer) {
    return null;
  }

  @Override
  public String getInfoByKey(String key) {
    return null;
  }

  @Override
  public Module getModuleById(Integer mid) {
    return null;
  }

  @Override
  public List<Log> getLogEntries() {
    return null;
  }

  @Override
  public Session getSessionById(int id) {
    return null;
  }
}
