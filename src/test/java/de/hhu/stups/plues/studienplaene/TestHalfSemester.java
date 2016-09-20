package de.hhu.stups.plues.studienplaene;

import static org.junit.Assert.assertEquals;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Session;

import org.junit.Before;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;


public class TestHalfSemester {

  private Map<Integer, Integer> groupChoice;
  private Map<Integer, Integer> semesterChoice;
  private Map<String, String>[] semesters;

  /**
   * Test setup.
   */
  @Before
  public void setUp() throws URISyntaxException {

    groupChoice = new HashMap<>();
    IntStream.rangeClosed(1, 9).forEach(i -> groupChoice.put(i, i));

    semesterChoice = new HashMap<>();
    IntStream.rangeClosed(1, 9).forEach(i -> semesterChoice.put(i, 1));

    final Map<String, Set<Integer>> moduleChoice = new HashMap<>();
    final Set<Integer> integerSet = new HashSet<>();
    integerSet.add(1);
    integerSet.add(3);
    moduleChoice.put("foo", integerSet);

    final MockStore store = new MockStore();
    final Course course = store.getCourseByKey("foo");

    final DataPreparatory data = new DataPreparatory(store, groupChoice, semesterChoice,
        moduleChoice, course, null);

    store.getGroups().get(0).setHalfSemester(1);
    store.getGroups().get(1).setHalfSemester(2);

    ArrayList<Session> sessions = new ArrayList<>(store.getGroups().get(2).getSessions());
    sessions.get(0).setRhythm(1);
    sessions = new ArrayList<>(store.getGroups().get(3).getSessions());
    sessions.get(0).setRhythm(2);
    sessions = new ArrayList<>(store.getGroups().get(4).getSessions());
    sessions.get(0).setRhythm(3);

    final DataStoreWrapper wrap = new DataStoreWrapper(ColorChoice.COLOR, data);

    semesters = wrap.getSemesters();
  }

  @Test
  public void testRhythm() {
    assertEquals(semesters[0].get("mon3"), "Abstract Unit: 3 (A);1");
    assertEquals(semesters[0].get("mon4"), "Abstract Unit: 4 (B);1");
    assertEquals(semesters[0].get("mon5"), "Abstract Unit: 5 (b);3");
  }

  @Test
  public void testHalfSemester() {
    assertEquals(semesters[0].get("mon2"), "Abstract Unit: 2 (s);1");
    assertEquals(semesters[0].get("mon1"), "Abstract Unit: 1 (f);1");
  }

}
