package de.hhu.stups.studienplaene;

import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Group;
import de.hhu.stups.plues.data.entities.Info;
import de.hhu.stups.plues.data.entities.Level;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.ModuleAbstractUnitSemester;
import de.hhu.stups.plues.data.entities.ModuleAbstractUnitType;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.data.entities.Unit;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class MockStore extends Store {

  private List<AbstractUnit> aus;
  private List<Unit> units;
  private List<Module> modules;
  private List<ModuleAbstractUnitSemester> maus;
  private List<Group> groups;

  @Override
  public void init() {

  }

  @Override
  public void init(final String dbpath) {

  }

  @Override
  public void close() {

  }

  @Override
  public void clear() {

  }

  @Override
  public List<Info> getInfo() {
    throw new RuntimeException();
  }

  @Override
  public List<AbstractUnit> getAbstractUnits() {
    if (this.aus == null) {
      this.aus = IntStream.range(1, 10).mapToObj(i -> {
        AbstractUnit abstractUnit = new AbstractUnit();
        abstractUnit.setId(i);
        abstractUnit.setTitle("Abstract Unit: " + String.valueOf(i));
        return abstractUnit;
      }).collect(Collectors.toList());
    }
    return this.aus;
  }

  @Override
  public List<Course> getCourses() {
    throw new RuntimeException();
  }

  @Override
  public Course getCourseByKey(final String key) {
    final Course course = new Course();
    course.setKey(key);
    course.setModules(new HashSet<>(this.getModules()));
    return course;
  }

  @Override
  public List<Group> getGroups() {
    if (this.groups == null) {
      this.groups = IntStream.range(1, 15).mapToObj(i -> {
        Group group = new Group();
        group.setId(i);
        Unit unit = new Unit();
        group.setUnit(unit);
        int id;
        switch (i) {
          case 12:
            id = 2;
            break;
          case 11:
            id = 5;
            break;
          case 10:
            id = 8;
            break;
          default:
            id = i;
            break;
        }
        unit.setId(id);


        group.setSessions(IntStream.builder().add(i * 5).build().mapToObj(j -> {
          Session session = new Session();
          session.setId(j);
          session.setRhythm(0);
          session.setDay("mon");
          session.setTime(i);
          return session;
        }).collect(Collectors.toSet()));
        return group;
      }).collect(Collectors.toList());
    }
    return this.groups;
  }

  @Override
  public List<Level> getLevels() {
    throw new RuntimeException();
  }

  @Override
  public List<Module> getModules() {
    if (this.modules == null) {
      this.modules = IntStream.range(1, 5).mapToObj(i -> {
        Module module = new Module();
        module.setId(i);
        module.setName(String.valueOf(i));
        if (i == 1) {
          module.setModuleAbstractUnitSemesters(this.getModuleAbstractUnitSemester().stream()
              .filter(j -> j.getAbstractUnit().getId() < 5).collect(Collectors.toSet()));
        } else if (i == 3) {
          module.setModuleAbstractUnitSemesters(this.getModuleAbstractUnitSemester().stream()
              .filter(j -> j.getAbstractUnit().getId() >= 5).collect(Collectors.toSet()));
        } else {
          module.setModuleAbstractUnitSemesters(new HashSet<>());
        }

        return module;
      }).collect(Collectors.toList());
    }
    return this.modules;
  }

  @Override
  public List<ModuleAbstractUnitSemester> getModuleAbstractUnitSemester() {
    if (this.maus == null) {
      this.maus = IntStream.range(1, 10).mapToObj(i -> {
        ModuleAbstractUnitSemester maus = new ModuleAbstractUnitSemester();
        maus.setAbstractUnit(getAbstractUnits().get(i - 1));
        maus.setSemester(1);
        return maus;
      }).collect(Collectors.toList());
    }
    return this.maus;
  }

  @Override
  public List<ModuleAbstractUnitType> getModuleAbstractUnitType() {
    throw new RuntimeException();
  }

  @Override
  public List<Session> getSessions() {
    throw new RuntimeException();
  }

  @Override
  public List<Unit> getUnits() {
    if (this.units == null) {
      this.units = IntStream.range(1, 10).mapToObj(i -> {
        Unit unit = new Unit();
        unit.setId(i);
        unit.setTitle("Unit: " + String.valueOf(i));
        return unit;
      }).collect(Collectors.toList());
    }
    return this.units;
  }

  @Override
  @SuppressWarnings("OptionalGetWithoutIsPresent")
  public AbstractUnit getAbstractUnitById(final Integer key) {
    return this.getAbstractUnits().stream().filter(e -> e.getId().equals(key)).findFirst().get();
  }

  @Override
  @SuppressWarnings("OptionalGetWithoutIsPresent")
  public Group getGroupById(final Integer key) {
    return this.getGroups().stream().filter(e -> e.getId() == key).findFirst().get();
  }

  @Override
  public String getInfoByKey(final String key) {
    throw new RuntimeException();
  }

  @Override
  @SuppressWarnings("OptionalGetWithoutIsPresent")
  public Module getModuleById(final Integer key) {
    return this.getModules().stream().filter(e -> e.getId() == key).findFirst().get();
  }
}
