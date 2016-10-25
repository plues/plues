package de.hhu.stups.plues.studienplaene;

import static org.junit.Assert.assertEquals;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.FeasibilityResult;

import org.junit.Before;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;


public class TestDataStoreWrapper {

  private Map<String, String>[] semesters;

  /**
   * Test setup.
   */
  @Before
  public void setUp() throws URISyntaxException {
    final MockStore store = new MockStore();
    final Course course = store.getCourseByKey("foo");
    final HashMap<Integer, Integer> groupChoice = new HashMap<>();

    groupChoice.put(1, 1);
    groupChoice.put(2, 12);
    groupChoice.put(3, 3);
    groupChoice.put(4, 4);
    groupChoice.put(5, 11);
    groupChoice.put(6, 6);
    groupChoice.put(7, 7);
    groupChoice.put(8, 10);
    groupChoice.put(9, 9);

    final HashMap<Integer, Integer> semesterChoice = new HashMap<>();
    IntStream.rangeClosed(1, 9).forEach(i -> semesterChoice.put(i, 1));

    final Map<String, Set<Integer>> moduleChoice = new HashMap<>();
    final Set<Integer> integerSet = new HashSet<>();
    integerSet.add(1);
    integerSet.add(3);
    moduleChoice.put("foo", integerSet);

    final FeasibilityResult result =
        new FeasibilityResult(moduleChoice, semesterChoice, groupChoice);

    final DataPreparatory data = new DataPreparatory(store, result, course, null);
    final DataStoreWrapper wrap = new DataStoreWrapper(ColorChoice.COLOR, data);
    semesters = wrap.getSemesters();
  }

  @Test
  public void testGetSemesterSizes() {

    // Test sizes
    assertEquals(semesters[0].size(), 9);
    assertEquals(semesters[1].size(), 0);
    assertEquals(semesters[2].size(), 0);
    assertEquals(semesters[3].size(), 0);
    assertEquals(semesters[4].size(), 0);
    assertEquals(semesters[5].size(), 0);
  }

  @Test
  public void testConflicts() {
    assertEquals("Abstract Unit: 6;3", semesters[0].get("mon6"));
    assertEquals("Abstract Unit: 1;1", semesters[0].get("mon1"));
    assertEquals("Abstract Unit: 3;1", semesters[0].get("mon3"));
  }
}
