package de.hhu.stups.plues.studienplaene;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;

import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Group;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.ModuleAbstractUnitSemester;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.data.entities.Unit;
import de.hhu.stups.plues.prob.FeasibilityResult;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestBase {

  Store store;
  protected Course course;
  FeasibilityResult result;
  private List<AbstractUnit> abstractUnits;
  private List<ModuleAbstractUnitSemester> moduleAbstractUnitSemesters;
  private List<Group> groups;
  private final Map<Integer, Session> sessions = new HashMap<>();
  private Set<Module> modules;

  protected void setUp() throws URISyntaxException {
    setupStore();
    setupCourse();

    final Map<Integer, Integer> groupChoice = new HashMap<>();
    groupChoice.put(1, 1);
    groupChoice.put(2, 12);
    groupChoice.put(3, 3);
    groupChoice.put(4, 4);
    groupChoice.put(5, 11);
    groupChoice.put(6, 6);
    groupChoice.put(7, 7);
    groupChoice.put(8, 10);
    groupChoice.put(9, 9);

    final Map<Integer, Integer> semesterChoice = new HashMap<>();
    IntStream.rangeClosed(1, 9).forEach(i -> semesterChoice.put(i, 1));

    final Map<String, Set<Integer>> moduleChoice = new HashMap<>();
    final Set<Integer> integerSet = new HashSet<>();
    integerSet.add(1);
    integerSet.add(3);
    moduleChoice.put("foo", integerSet);

    result = new FeasibilityResult(moduleChoice, semesterChoice, groupChoice);
  }

  private void setupCourse() {
    course = mock(Course.class);
    stub(course.getKey()).toReturn("foo");
    stub(course.getModules()).toReturn(modules);
  }

  private List<Group> getGroups() {
    if (this.groups == null) {
      this.groups = IntStream.range(1, 15).mapToObj(i -> {
        final Group group = mock(Group.class);
        //
        stub(group.getId()).toReturn(i);
        stub(group.getHalfSemester()).toAnswer(invocation -> {
          switch (i) {
            case 1:
            case 2:
              return i;
            default:
              return 0;
          }
        });
        //
        final Unit unit = getUnit(i);
        stub(group.getUnit()).toReturn(unit);
        //
        stub(group.getSessions()).toAnswer(invocation
            -> IntStream.builder().add(i * 5).build().mapToObj(j
                -> getSession(i, j)).collect(Collectors.toSet()));
        return group;
      }).collect(Collectors.toList());
    }
    return this.groups;
  }

  private Session getSession(final int groupId, final int sessionId) {
    if (this.sessions.get(sessionId) == null) {
      final Session session = mock(Session.class);
      stub(session.getId()).toReturn(sessionId);
      final int rhythm;
      switch (groupId) {
        case 3:
          rhythm = 1;
          break;
        case 4:
          rhythm = 2;
          break;
        case 5:
          rhythm = 3;
          break;
        default:
          rhythm = 0;
          break;
      }
      stub(session.getRhythm()).toReturn(rhythm);
      stub(session.getDay()).toReturn("mon");
      stub(session.getTime()).toReturn(groupId);
      this.sessions.put(sessionId, session);
    }
    return this.sessions.get(sessionId);
  }

  private Unit getUnit(final int groupId) {
    final Unit unit = mock(Unit.class);

    stub(unit.getId()).toAnswer(invocation -> {
      final int id;
      switch (groupId) {
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
          id = groupId;
          break;
      }
      return id;
    });
    return unit;
  }

  private Set<Module> getModules(final List<ModuleAbstractUnitSemester> maus) {
    if (this.modules == null) {
      final Set<Module> modules = IntStream.range(1, 5).mapToObj(i -> {
        Module module = mock(Module.class);
        //
        stub(module.getId()).toReturn(i);
        stub(module.getTitle()).toReturn(String.valueOf(i));
        //
        // abstract units with IDs < 5 are linked to the module with ID 1
        // abstract units with IDs >= 5 are linked to the module with ID 3
        stub(module.getModuleAbstractUnitSemesters()).toAnswer(invocation -> {
          if (i == 1) {
            return maus.stream()
              .filter(j -> j.getAbstractUnit().getId() < 5)
              .collect(Collectors.collectingAndThen(
                Collectors.toSet(), Collections::unmodifiableSet));
          } else if (i == 3) {
            return maus.stream()
              .filter(j -> j.getAbstractUnit().getId() >= 5)
              .collect(Collectors.collectingAndThen(
                Collectors.toSet(), Collections::unmodifiableSet));
          } else {
            return new HashSet<>();
          }
        });
        return module;
      }).collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));

      maus.forEach(moduleAbstractUnitSemester
          -> stub(moduleAbstractUnitSemester.getModule()).toAnswer(invocation -> {
            if (moduleAbstractUnitSemester.getAbstractUnit().getId() < 5) {
              return modules.stream().filter(module -> module.getId() == 1).findFirst().get();
            } else {
              return modules.stream().filter(module -> module.getId() == 3).findFirst().get();
            }
          }));
      this.modules = modules;
    }
    return this.modules;
  }

  private List<ModuleAbstractUnitSemester> getModuleAbstractUnitSemesters(
      final List<AbstractUnit> abstractUnits) {
    if (this.moduleAbstractUnitSemesters == null) {
      this.moduleAbstractUnitSemesters = IntStream.range(1, 10).mapToObj(i -> {
        final ModuleAbstractUnitSemester moduleAbstractUnitSemester
            = mock(ModuleAbstractUnitSemester.class);
        stub(moduleAbstractUnitSemester.getAbstractUnit())
            .toAnswer(invocation -> abstractUnits.get(i - 1));
        stub(moduleAbstractUnitSemester.getSemester()).toReturn(1);
        return moduleAbstractUnitSemester;
      }).collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }
    return this.moduleAbstractUnitSemesters;
  }

  private List<AbstractUnit> getAbstractUnits() {
    if (this.abstractUnits == null) {
      this.abstractUnits = IntStream.range(1, 10).mapToObj(i -> {
        AbstractUnit abstractUnit = mock(AbstractUnit.class);
        stub(abstractUnit.getId()).toReturn(i);
        stub(abstractUnit.getTitle()).toReturn("Abstract Unit: " + String.valueOf(i));
        return abstractUnit;
      }).collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }
    return this.abstractUnits;
  }

  private void setupStore() {
    final List<AbstractUnit> abstractUnits = getAbstractUnits();
    final List<ModuleAbstractUnitSemester> maus = getModuleAbstractUnitSemesters(abstractUnits);
    final Set<Module> modules = getModules(maus);
    final List<Group> groups = getGroups();
    //
    store = mock(Store.class);
    //
    stub(store.getModuleById(anyInt())).toAnswer(invocation -> {
      final Integer arg = invocation.getArgumentAt(0, Integer.class);
      return modules.stream().filter(module -> module.getId() == arg).findFirst().get();
    });
    stub(store.getAbstractUnitById(anyInt())).toAnswer(invocation -> {
      final Integer arg = invocation.getArgumentAt(0, Integer.class);
      return abstractUnits.stream()
        .filter(abstractUnit -> abstractUnit.getId().equals(arg))
        .findFirst().get();
    });
    stub(store.getGroupById(anyInt())).toAnswer(invocation -> {
      final Integer arg = invocation.getArgumentAt(0, Integer.class);
      return groups.stream()
        .filter(group -> group.getId() == arg)
        .findFirst().get();
    });
  }

}
