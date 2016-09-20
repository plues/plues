package de.hhu.stups.studienplaene;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.FeasibilityResult;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

public class TestDataPreperatory {

  private MockStore store;
  private Course course;
  private FeasibilityResult feasibilityResult;

  /**
   * Test setup.
   */
  @Before
  public void setUp() {
    store = new MockStore();
    course = store.getCourseByKey("foo");

    Map<Integer, Integer> groupChoice;
    Map<Integer, Integer> semesterChoice;
    HashMap<Integer, Integer> unitChoice;
    HashMap<String, Set<Integer>> moduleChoice;


    groupChoice = new HashMap<>();
    groupChoice.put(1, 1);
    groupChoice.put(2, 12);
    groupChoice.put(3, 3);
    groupChoice.put(4, 4);
    groupChoice.put(5, 11);
    groupChoice.put(6, 6);
    groupChoice.put(7, 7);
    groupChoice.put(8, 10);
    groupChoice.put(9, 9);

    semesterChoice = new HashMap<>();
    IntStream.rangeClosed(1, 9).forEach(i -> semesterChoice.put(i, 1));

    unitChoice = new HashMap<>();
    IntStream.rangeClosed(1, 9).forEach(i -> unitChoice.put(i, i));

    moduleChoice = new HashMap<>();
    Set<Integer> integerSet = new HashSet<Integer>();
    integerSet.add(1);
    integerSet.add(3);
    moduleChoice.put("foo", integerSet);

    feasibilityResult =
        new FeasibilityResult(moduleChoice, unitChoice, semesterChoice, groupChoice);
  }


  @Test
  public void testGetUnitGroup() {
    final DataPreparatory data = new DataPreparatory(store, feasibilityResult, course, null);

    final Map<Integer, Integer> groups = data.getPdfResult().getGroupChoice();

    // Test not null and size
    assertNotNull(groups);
    assertEquals(groups.size(), feasibilityResult.getGroupChoice().size());
;

    for (final Map.Entry<Integer, Integer> gc : feasibilityResult.getGroupChoice().entrySet()) {
      final Integer key = gc.getKey();
      final Integer value = gc.getValue();
      assertTrue(groups.containsKey(key));
      assertEquals(groups.get(key), value);
    }
  }

  @Test
  public void testUnitModuleMapping() {
    final Course course = store.getCourseByKey("foo");

    final DataPreparatory dp = new DataPreparatory(store, feasibilityResult, course, null);
    final Map<String, Set<Integer>> moduleChoice = dp.getPdfResult().getModuleChoice();

    assertTrue(moduleChoice.get(2).contains(1));
    assertTrue(moduleChoice.get(3).contains(1));
    assertTrue(moduleChoice.get(6).contains(3));
    assertTrue(moduleChoice.get(7).contains(3));
  }


}
