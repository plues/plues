package de.hhu.stups.plues.prob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import de.prob.translator.Translator;
import de.prob.translator.types.Set;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class MappersTest {
  @Before
  public void setUp() throws Exception {

  }

  @Test
  public void mapSemesterChoice() throws Exception {
    final String sc = "{(au1|->sem1),(au2|->sem1),(au3|->sem1),(au4|->sem1),(au5|->sem1),"
        + "(au6|->sem4),(au7|->sem1),(au8|->sem1),(au9|->sem2)}";
    final Set semesterChoice = (Set) Translator.translate(sc);
    final Map<Integer, Integer> mapped = Mappers.mapSemesterChoice(semesterChoice);
    assertEquals((long) mapped.get(1), 1);
    assertEquals((long) mapped.get(2), 1);
    assertEquals((long) mapped.get(3), 1);
    assertEquals((long) mapped.get(4), 1);
    assertEquals((long) mapped.get(5), 1);
    assertEquals((long) mapped.get(6), 4);
    assertEquals((long) mapped.get(7), 1);
    assertEquals((long) mapped.get(8), 1);
    assertEquals((long) mapped.get(9), 2);
  }

  @Test
  public void mapGroupChoice() throws Exception {
    final String gc = "{(au1|->group1),(au2|->group12),(au3|->group3),(au4|->group4),"
        + "(au5|->group11),(au6|->group6),(au7|->group7),(au8|->group10),(au9|->group9)}";
    final Set groupChoice = (Set) Translator.translate(gc);
    final Map<Integer, Integer> mapped = Mappers.mapGroupChoice(groupChoice);
    assertEquals((long) mapped.get(1), 1);
    assertEquals((long) mapped.get(2), 12);
    assertEquals((long) mapped.get(3), 3);
    assertEquals((long) mapped.get(4), 4);
    assertEquals((long) mapped.get(5), 11);
    assertEquals((long) mapped.get(6), 6);
    assertEquals((long) mapped.get(7), 7);
    assertEquals((long) mapped.get(8), 10);
    assertEquals((long) mapped.get(9), 9);

  }

  @Test
  public void mapModuleChoice() throws Exception {
    final String mc = "{\"foo\" |-> {mod1, mod200}}";
    final Set moduleChoice = (Set) Translator.translate(mc);

    final HashSet<Integer> t = new HashSet<>();
    t.add(1);
    t.add(200);

    final Map<String, java.util.Set<Integer>> mapped = Mappers.mapModuleChoice(moduleChoice);

    assertTrue(mapped.containsKey("foo"));
    assertEquals(mapped.get("foo"), t);
  }

  @Test
  public void mapToModuleChoice() throws Exception {
    final Map<String, List<Integer>> inp = new HashMap<>();
    inp.put("a", Arrays.asList(1, 2, 3));
    inp.put("b", Arrays.asList(11, 12, 13));

    final String result = Mappers.mapToModuleChoice(inp);

    assertEquals("{(\"a\" |-> {mod1,mod2,mod3}),(\"b\" |-> {mod11,mod12,mod13})}", result);

  }
}
