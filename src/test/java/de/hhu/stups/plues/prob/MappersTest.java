package de.hhu.stups.plues.prob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import de.be4.classicalb.core.parser.exceptions.BException;
import de.prob.translator.Translator;
import de.prob.translator.types.BObject;
import de.prob.translator.types.Set;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MappersTest {

  @Test
  public void mapSemesterChoice() throws BException {
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
  public void mapGroupChoice() throws BException {
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
  public void mapModuleChoice() throws BException {
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
  public void mapToModuleChoice() {
    final Map<String, List<Integer>> inp = new HashMap<>();
    inp.put("a", Arrays.asList(1, 2, 3));
    inp.put("b", Arrays.asList(11, 12, 13));

    final String result = Mappers.mapToModuleChoice(inp);

    assertEquals("{(\"a\" |-> {mod1,mod2,mod3}),(\"b\" |-> {mod11,mod12,mod13})}", result);

  }


  @Test
  public void testMapImpossibleCoursesBecauseOfImpossibleModuleCombinations() throws BException {
    final String raw = "{(((mod39|->au58)|->{sem6})|->unit577),"
        + "(((mod37|->au5)|->{sem1})|->unit578),"
        + "(((mod560|->au88)|->{sem3})|->unit579)}";
    final Set input = (Set) Translator.translate(raw);
    final java.util.Set<ModuleAbstractUnitUnitSemesterConflict> result
        = Mappers.mapModuleAbstractUnitUnitSemesterMismatch(input);

    final Map<Integer, ModuleAbstractUnitUnitSemesterConflict> resultMap
        = result.stream().collect(
            Collectors.toMap(ModuleAbstractUnitUnitSemesterConflict::getModuleId, t -> t));

    assertEquals(3, result.size());

    final ModuleAbstractUnitUnitSemesterConflict r1 = resultMap.get(39);

    assertEquals(Integer.valueOf(58), r1.getAbstractUnitId());
    assertEquals(Integer.valueOf(577), r1.getUnitId());
    assertTrue(r1.getAbstractUnitSemesters().contains(6));
  }

  @Test
  public void testMapModules() throws  BException {
    final String raw = "{mod1, mod200}";
    final Set input = (Set) Translator.translate(raw);
    final java.util.Set<Integer> result = Mappers.mapModules(input);

    assertEquals(2, result.size());
    assertTrue(result.contains(1));
    assertTrue(result.contains(200));

  }

  @Test
  public void testMapToModules() {
    Assert.assertEquals(Arrays.asList("mod1", "mod11"), Mappers.mapToModules(Arrays.asList(1, 11)));
  }

  @Test
  public void testMapAbstractUnits() throws BException {
    Assert.assertEquals(
        new HashSet<>(Arrays.asList(1, 11)),
        new HashSet<>(Mappers.mapAbstractUnits((Set) Translator.translate("{au1, au11}"))));
  }

  @Test
  public void testMapToAbstractUnits() {
    Assert.assertEquals(Arrays.asList("au22", "au23"),
        Mappers.mapToAbstractUnits(Arrays.asList(22, 23)));
  }

  @Test
  public void testMapGroups() throws BException {
    Assert.assertEquals(
        new HashSet<>(Arrays.asList(22, 234)),
        new HashSet<>(Mappers.mapGroups((Set) Translator.translate("{group234, group22}"))));
  }

  @Test
  public void testMapToGroups() {
    Assert.assertEquals(Arrays.asList("group22", "group223"),
        Mappers.mapToGroups(Arrays.asList(22, 223)));
  }

  @Test
  public void testMapSessions() throws BException {
    Assert.assertEquals(
        new HashSet<>(Arrays.asList(222, 23423)),
        new HashSet<>(Mappers.mapSessions(
            (Set) Translator.translate("{session222, session23423}"))));
  }

  @Test
  public void testRedundantUnitGroups() throws BException {
    final String input = "{((unit254|->group269)|->group270),((unit254|->group277)|->group280),"
        + "((unit493|->group542)|->group543)}";
    final BObject translated = Translator.translate(input);
    final Map<Integer, java.util.Set<Pair<Integer>>> result
        = Mappers.mapUnitGroups((Set) translated);

    Assert.assertEquals(2, result.size());
    final java.util.Set<Pair<Integer>> u254 = result.get(254);
    Assert.assertEquals(2, u254.size());
    Assert.assertTrue(u254.contains(new Pair<>(269, 270)));
  }

  @Test
  public void testMapQuasiMandatoryModuleAbstractUnits() throws BException {
    final String input = "{(mod1|->au1),(mod1|->au2),(mod286|->au568),(mod286|->au569)}";
    final BObject translated = Translator.translate(input);
    final Map<Integer, java.util.Set<Integer>> result
        = Mappers.mapQuasiMandatoryModuleAbstractUnits((Set) translated);

    Assert.assertEquals(2, result.size());
    final java.util.Set<Integer> entry = result.get(1);
    Assert.assertEquals(2, entry.size());
    Assert.assertTrue(entry.contains(1));
    Assert.assertTrue(entry.contains(2));

  }


  @Test
  public void testMapModuleAbstractUnitPairs() throws BException {
    final String input = "{((mod235|->au329)|->{sem1,sem3}),((mod235|->au425)|->{sem1,sem3}),"
        + "((mod235|->au426)|->{sem1,sem3}),((mod236|->au429)|->{sem1,sem3}),"
        + "((mod236|->au430)|->{sem1,sem3})}";
    final BObject translated = Translator.translate(input);
    final Map<Integer, java.util.Set<Integer>> result
        = Mappers.mapModuleAbstractUnitPairs((Set) translated);
    Assert.assertEquals(2, result.size());
    final java.util.Set<Integer> entry = result.get(235);
    Assert.assertEquals(3, entry.size());
    Assert.assertTrue(entry.contains(329));
    Assert.assertTrue(entry.contains(425));
    Assert.assertTrue(entry.contains(426));
  }

  @Test
  public void testMapCourseSet() throws BException {
    final String input = "{\"BA-IWS-H-2013\",\"BA-KUL-H-2013\",\"BA-LIN-COM-H-2013\","
        + "\"BA-LIN-GRU-H-2013\",\"BA-LIN-PSY-H-2013\"}";
    final BObject translated = Translator.translate(input);
    final java.util.Set result = Mappers.mapCourseSet((Set) translated);


    Assert.assertEquals(5, result.size());
    Assert.assertTrue(result.contains("BA-KUL-H-2013"));

    Assert.assertFalse(result.contains("\"BA-IWS-H-2013\""));
  }

  @Test
  public void testMapCourseModuleAbstractUnitPairs() throws BException {
    final String input = "{(((\"BA-KUL-H-2013\"|->mod273)|->au530)|->au531)}";
    final BObject translated = Translator.translate(input);
    final Map<String, Map<Integer, java.util.Set<Pair<Integer>>>> result
        = Mappers.mapCourseModuleAbstractUnitPairs((Set) translated);

    assertEquals(1, result.size());

    final Map<Integer, java.util.Set<Pair<Integer>>> maup = result.get("BA-KUL-H-2013");
    assertEquals(1, maup.size());

    final java.util.Set<Pair<Integer>> aup = maup.get(273);
    assertEquals(1, aup.size());
    assertTrue(aup.contains(new Pair<>(530, 531)));

  }




}

