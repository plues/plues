package de.hhu.stups.plues.prob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import de.prob.translator.Translator;
import de.prob.translator.types.Set;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
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
    final String gc = "{(unit1|->group1),(unit2|->group12),(unit3|->group3),(unit4|->group4),"
        + "(unit5|->group11),(unit6|->group6),(unit7|->group7),(unit8|->group10),(unit9|->group9)}";
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
  public void mapUnitChoice() throws Exception {
    final String uc = "{(au1|->unit1),(au2|->unit2),(au3|->unit99),(au4|->unit4),(au5|->unit5),"
        + "(au6|->unit6),(au7|->unit7),(au8|->unit8),(au9|->unit9)}";
    final Set unitChoice = (Set) Translator.translate(uc);
    final Map<Integer, Integer> mapped = Mappers.mapUnitChoice(unitChoice);
    assertEquals((long) mapped.get(1), 1);
    assertEquals((long) mapped.get(2), 2);
    assertEquals((long) mapped.get(3), 99);
    assertEquals((long) mapped.get(4), 4);
    assertEquals((long) mapped.get(5), 5);
    assertEquals((long) mapped.get(6), 6);
    assertEquals((long) mapped.get(7), 7);
    assertEquals((long) mapped.get(8), 8);
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
}
