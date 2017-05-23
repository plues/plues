package de.hhu.stups.plues.studienplaene;

import static org.junit.Assert.assertEquals;

import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.ui.UiTestDataCreator;

import org.junit.Before;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestHalfSemester extends TestBase {

  private Map<String, String>[] semesters;

  /**
   * Test setup.
   */
  @Before
  public void setUp() throws URISyntaxException {
    super.setUp();

    final Map<Integer, Integer> groupChoice = new HashMap<>();
    IntStream.rangeClosed(1, 9).forEach(i -> groupChoice.put(i, i));

    final Map<Integer, Integer> semesterChoice = new HashMap<>();
    IntStream.rangeClosed(1, 9).forEach(i -> semesterChoice.put(i, 1));

    final Map<String, Set<Integer>> moduleChoice = new HashMap<>();
    final Set<Integer> integerSet = new HashSet<>();
    integerSet.add(1);
    integerSet.add(3);
    moduleChoice.put("foo", integerSet);

    final Map<Integer, Set<Integer>> abstractUnitChoice = new HashMap<>();
    abstractUnitChoice.put(1, IntStream.range(1,5).boxed().collect(Collectors.toSet()));
    abstractUnitChoice.put(3, IntStream.rangeClosed(5,9).boxed().collect(Collectors.toSet()));


    final FeasibilityResult result =
        new FeasibilityResult(moduleChoice, abstractUnitChoice, semesterChoice, groupChoice);

    final DataPreparatory data = new DataPreparatory(store, result);
    final DataStoreWrapper wrap = new DataStoreWrapper(UiTestDataCreator.getColorScheme(), data);

    semesters = wrap.getSemesters();
  }

  @Test
  public void testRhythm() {
    assertEquals("Abstract Unit: 3 (A);1", semesters[0].get("mon3"));
    assertEquals("Abstract Unit: 4 (B);1", semesters[0].get("mon4"));
    assertEquals("Abstract Unit: 5 (b);3", semesters[0].get("mon5"));
  }

  @Test
  public void testHalfSemester() {
    assertEquals("Abstract Unit: 2 (s);1", semesters[0].get("mon2"));
    assertEquals("Abstract Unit: 1 (f);1", semesters[0].get("mon1"));
  }

}
