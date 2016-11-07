package de.hhu.stups.plues.studienplaene;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.prob.FeasibilityResult;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

public class TestBase {

  protected MockStore store;
  protected Course course;
  protected FeasibilityResult result;
  protected Map<String, String>[] semesters;

  protected void setUp() throws URISyntaxException {
    store = new MockStore();
    course = store.getCourseByKey("foo");

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
    Set<Integer> integerSet = new HashSet<Integer>();
    integerSet.add(1);
    integerSet.add(3);
    moduleChoice.put("foo", integerSet);

    result = new FeasibilityResult(moduleChoice, semesterChoice, groupChoice);
  }

  protected void wrapper() {
    final DataPreparatory data = new DataPreparatory(store, result, course, null);
    final DataStoreWrapper wrap = new DataStoreWrapper(ColorChoice.COLOR, data);
    semesters = wrap.getSemesters();
  }

  protected void halfSemester() {
    final Map<Integer, Integer> groupChoice = new HashMap<>();
    IntStream.rangeClosed(1, 9).forEach(i -> groupChoice.put(i, i));

    final Map<Integer, Integer> semesterChoice = new HashMap<>();
    IntStream.rangeClosed(1, 9).forEach(i -> semesterChoice.put(i, 1));

    final Map<String, Set<Integer>> moduleChoice = new HashMap<>();
    final Set<Integer> integerSet = new HashSet<>();
    integerSet.add(1);
    integerSet.add(3);
    moduleChoice.put("foo", integerSet);

    final MockStore store = new MockStore();
    final Course course = store.getCourseByKey("foo");
    final FeasibilityResult result =
        new FeasibilityResult(moduleChoice, semesterChoice, groupChoice);

    store.getGroups().get(0).setHalfSemester(1);
    store.getGroups().get(1).setHalfSemester(2);

    ArrayList<Session> sessions = new ArrayList<>(store.getGroups().get(2).getSessions());
    sessions.get(0).setRhythm(1);
    sessions = new ArrayList<>(store.getGroups().get(3).getSessions());
    sessions.get(0).setRhythm(2);
    sessions = new ArrayList<>(store.getGroups().get(4).getSessions());
    sessions.get(0).setRhythm(3);

    final DataPreparatory data = new DataPreparatory(store, result, course, null);
    final DataStoreWrapper wrap = new DataStoreWrapper(ColorChoice.COLOR, data);

    semesters = wrap.getSemesters();
  }

}
