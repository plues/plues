package de.hhu.stups.plues.prob;

import com.google.common.base.Functions;
import com.google.common.base.Joiner;

import de.hhu.stups.plues.prob.report.Pair;
import de.prob.translator.types.BObject;
import de.prob.translator.types.Record;
import de.prob.translator.types.Set;
import de.prob.translator.types.Tuple;
import javafx.collections.FXCollections;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class Mappers {

  private static final String MODULE_PREFIX = "mod";
  private static final String ABSTRACT_UNIT_PREFIX = "au";
  private static final String GROUP_PREFIX = "group";
  private static final String UNIT_PREFIX = "unit";
  private static final String SEMESTER_PREFIX = "sem";
  private static final String SESSION_PREFIX = "session";

  private Mappers() {
  }

  static Map<Integer, Integer> mapSemesterChoice(final Set set) {
    return Collections.unmodifiableMap(convertToMap(set, ABSTRACT_UNIT_PREFIX, SEMESTER_PREFIX));
  }

  static Map<Integer, Integer> mapGroupChoice(final Set set) {

    return Collections.unmodifiableMap(convertToMap(set, ABSTRACT_UNIT_PREFIX, GROUP_PREFIX));
  }

  private static Map<Integer, Integer> convertToMap(final Set set,
                                                    final String keyPrefix,
                                                    final String valuePrefix) {
    return set.stream().collect(
      Collectors.toMap(
        i -> mapValue(((Tuple) i).getFirst().toString(), keyPrefix),
        i -> mapValue(((Tuple) i).getSecond().toString(), valuePrefix)));
  }

  private static Integer mapValue(final String val, final String prefix) {
    final String idVal = val.substring(prefix.length(), val.length());
    return Integer.parseInt(idVal);
  }

  static Map<String, java.util.Set<Integer>> mapModuleChoice(final Set moduleChoice) {
    return moduleChoice.stream().collect(Collectors.collectingAndThen(
      Collectors.toMap(
        bObject -> {
          final Tuple mc = (Tuple) bObject;
          return ((de.prob.translator.types.String) mc.getFirst()).getValue();
        },
        bObject -> {
          final Tuple mc = (Tuple) bObject;
          final Set modules = (Set) mc.getSecond();
          return modules.stream()
              .map(m -> mapValue(m.toString(), MODULE_PREFIX))
              .collect(
                Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
        }
      ), Collections::unmodifiableMap));
  }

  static java.util.Set<String> mapCourseSet(final Set value) {
    return value.stream().map(Object::toString).map(Mappers::mapString).collect(
        Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
  }

  static java.util.Set<Integer> mapSessions(final Set modelResult) {
    return modelResult.stream().map(
      v -> mapValue(v.toString(), SESSION_PREFIX)).collect(Collectors.collectingAndThen(
        Collectors.toSet(), Collections::unmodifiableSet));
  }

  static String mapSession(final Integer session) {
    return "session" + session;
  }

  static String mapToModuleChoice(final Map<String, List<Integer>> moduleChoice) {
    return moduleChoice.entrySet().stream()
      .map(e -> String.format("(\"%s\" |-> {%s})", e.getKey(),
        e.getValue().stream().map(i -> MODULE_PREFIX + i ).collect(Collectors.joining(","))))
      .collect(Collectors.joining(",", "{", "}"));
  }

  static List<Alternative> mapAlternatives(final Set modelResult) {
    return modelResult.stream().collect(Collectors.collectingAndThen(
      Collectors.mapping(bObject -> {
        final Record record = (Record) bObject;
        final String day = record.get("day").toString();
        return new Alternative(mapString(day), record.get("slot").toString());
      }, Collectors.toList()), Collections::unmodifiableList));
  }

  static String mapString(final String str) {
    // XXX Temporary fix, until ProB 2.0 socket encoding has been fixed.
    return new String(str.substring(1, str.length() - 1).getBytes(Charset.defaultCharset()),
        Charset.forName("utf8"));
  }

  static Map<String, Map<Integer, java.util.Set<Integer>>> mapCourseModuleAbstractUnits(
      final Set courseModuleAbstractUnits) {

    final Map<String, Map<Integer, java.util.Set<Integer>>> result = new HashMap<>();
    courseModuleAbstractUnits.forEach(bObject -> {
      final Tuple tuple = (Tuple) bObject;
      final Tuple courseModule = (Tuple) tuple.getFirst();
      final String course = mapString(courseModule.getFirst().toString());
      final Integer module = mapValue(courseModule.getSecond().toString(), MODULE_PREFIX);
      final Integer abstractUnit = mapValue(tuple.getSecond().toString(), ABSTRACT_UNIT_PREFIX);
      if (!result.containsKey(course)) {
        result.put(course, new HashMap<>());
      }

      final Map<Integer, java.util.Set<Integer>> modules = result.get(course);
      if (!modules.containsKey(module)) {
        modules.put(module, new HashSet<>());
      }
      modules.get(module).add(abstractUnit);
    });

    return Collections.unmodifiableMap(
        result.entrySet().stream().collect(Collectors.toMap(
          Map.Entry::getKey,
          e -> Collections.unmodifiableMap(
            e.getValue().entrySet().stream().collect(Collectors.toMap(
              Map.Entry::getKey,
              i -> Collections.unmodifiableSet(i.getValue())))))));
  }

  static Map<String, Map<Integer, java.util.Set<Pair<Integer>>>> mapCourseModuleAbstractUnitPairs(
      final Set courseModuleAbstractUnitPairs) {
    final Map<String, Map<Integer, java.util.Set<Pair<Integer>>>> result = new HashMap<>();

    courseModuleAbstractUnitPairs.forEach(bObject -> {
      final Tuple tuple = (Tuple) bObject;
      final Tuple cmau = (Tuple) tuple.getFirst();
      final Tuple cm = (Tuple) cmau.getFirst();
      final String course = mapString(cm.getFirst().toString());
      final Integer module = mapValue(cm.getSecond().toString(), MODULE_PREFIX);

      final Integer au1 = mapValue(cmau.getSecond().toString(), ABSTRACT_UNIT_PREFIX);
      final Integer au2 = mapValue(tuple.getSecond().toString(), ABSTRACT_UNIT_PREFIX);

      if (!result.containsKey(course)) {
        result.put(course, new HashMap<>());
      }

      final Map<Integer, java.util.Set<Pair<Integer>>> modules = result.get(course);
      if (!modules.containsKey(module)) {
        modules.put(module, new HashSet<>());
      }
      modules.get(module).add(new Pair<>(au1, au2));
    });

    return Collections.unmodifiableMap(
      result.entrySet().stream().collect(Collectors.toMap(
        Map.Entry::getKey,
        e -> Collections.unmodifiableMap(
          e.getValue().entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            i -> Collections.unmodifiableSet(i.getValue())))))));
  }

  static Map<Integer, java.util.Set<Integer>> mapModuleAbstractUnitPairs(
      final Set moduleAbstractUnits) {
    return moduleAbstractUnits.stream().collect(Collectors.collectingAndThen(
      Collectors.groupingBy(bObject -> {
        final Tuple moduleAbstractUnit = (Tuple) ((Tuple) bObject).getFirst();
        return mapValue(moduleAbstractUnit.getFirst().toString(), MODULE_PREFIX);
      }, Collectors.mapping(bObject -> {
        final Tuple moduleAbstractUnit = (Tuple) ((Tuple) bObject).getFirst();
        return mapValue(moduleAbstractUnit.getSecond().toString(), ABSTRACT_UNIT_PREFIX);
      }, Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet))),
    Collections::unmodifiableMap));
  }

  static java.util.Set<Integer> extractModules(final Set incompleteModules) {
    return incompleteModules.stream().collect(
      Collectors.collectingAndThen(
        Collectors.mapping(
          bObject -> mapValue(((Tuple) bObject).getFirst().toString(), MODULE_PREFIX),
          Collectors.toSet()),
        Collections::unmodifiableSet));
  }

  static Map<Integer, java.util.Set<Integer>> mapQuasiMandatoryModuleAbstractUnits(
      final Set quasiMandatoryModuleAbstractUnits) {

    return quasiMandatoryModuleAbstractUnits.stream().collect(Collectors.collectingAndThen(
      Collectors.groupingBy(bObject
          -> mapValue(((Tuple)bObject).getFirst().toString(), MODULE_PREFIX),
        Collectors.mapping(bObject
            -> mapValue(((Tuple)bObject).getSecond().toString(), ABSTRACT_UNIT_PREFIX),
          Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet))),
          Collections::unmodifiableMap));
  }

  static Map<Integer, java.util.Set<Pair<Integer>>> mapUnitGroups(final Set redundantUnitGroups) {
    return redundantUnitGroups.stream().collect(
      Collectors.collectingAndThen(Collectors.groupingBy(
        bObject
            -> mapValue(((Tuple)((Tuple)bObject).getFirst()).getFirst().toString(), UNIT_PREFIX),
      Collectors.mapping(bObject -> {
        final Tuple tuple = (Tuple) bObject;
        final Tuple unitGroup = (Tuple) tuple.getFirst();
        final Integer g1 = mapValue(unitGroup.getSecond().toString(), GROUP_PREFIX);
        final Integer g2 = mapValue(tuple.getSecond().toString(), GROUP_PREFIX);
        return new Pair<>(g1, g2);
      }, Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet))),
      Collections::unmodifiableMap));
  }

  static java.util.Set<ModuleAbstractUnitUnitSemesterConflict>
      mapModuleAbstractUnitUnitSemesterMismatch(final Set conflicts) {
    return conflicts.stream().map(bObject -> {
      final Tuple tuple = (Tuple) bObject;
      final Tuple maus = (Tuple) tuple.getFirst();
      final Tuple mau = (Tuple) maus.getFirst();

      final Integer module = mapValue(mau.getFirst().toString(), MODULE_PREFIX);
      final Integer abstractUnit = mapValue(mau.getSecond().toString(), ABSTRACT_UNIT_PREFIX);

      final Integer unit = mapValue(tuple.getSecond().toString(), UNIT_PREFIX);
      final java.util.Set<Integer> semesters = Collections.unmodifiableSet(
          ((Set) maus.getSecond()).stream()
            .map(sem -> mapValue(sem.toString(), SEMESTER_PREFIX))
            .collect(Collectors.toSet()));
      return new ModuleAbstractUnitUnitSemesterConflict(module, abstractUnit, semesters, unit);
    }).collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
  }

  static java.util.Set<Integer> mapModules(final Set modules) {
    return modules.stream().map(bObject
        -> mapValue(bObject.toString(), MODULE_PREFIX)).collect(
          Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
  }

  static java.util.Set<Integer> mapAbstractUnits(final Set abstractUnits) {
    return abstractUnits.stream().map(bObject
        -> mapValue(bObject.toString(), ABSTRACT_UNIT_PREFIX)).collect(
          Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
  }

  static java.util.Set<Integer> mapGroups(final Set groups) {
    return groups.stream().map(bObject ->
        mapValue(bObject.toString(), GROUP_PREFIX)).collect(
          Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));

  }

  static List<String> mapToModules(final List<Integer> modules) {
    return modules.stream().map(module
        -> String.format("%s%d", MODULE_PREFIX, module)).collect(
          Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));

  }

  static List<String> mapToAbstractUnits(final List<Integer> abstractUnits) {
    return abstractUnits.stream().map(module
        -> String.format("%s%d", ABSTRACT_UNIT_PREFIX, module)).collect(
          Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
  }

  static List<String> mapToGroups(final List<Integer> groups) {
    return groups.stream().map(module
        -> String.format("%s%d", GROUP_PREFIX, module)).collect(
          Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
  }
}
