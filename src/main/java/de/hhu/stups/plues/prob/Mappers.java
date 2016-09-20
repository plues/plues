package de.hhu.stups.plues.prob;

import com.google.common.base.Joiner;

import de.prob.translator.types.BObject;
import de.prob.translator.types.Record;
import de.prob.translator.types.Set;
import de.prob.translator.types.Tuple;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class Mappers {

  private Mappers() {
  }

  static Map<Integer, Integer> mapSemesterChoice(final Set set) {
    return Collections.unmodifiableMap(convertToMap(set, "au", "sem"));
  }

  static Map<Integer, Integer> mapGroupChoice(final Set set) {

    return Collections.unmodifiableMap(convertToMap(set, "au", "group"));
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
    final java.util.Map<java.lang.String, java.util.Set<Integer>> collectedModules
        = new HashMap<>();

    for (final BObject o : moduleChoice) {

      final Tuple mc = (Tuple) o;
      final Set modules = (Set) mc.getSecond();

      final String key = ((de.prob.translator.types.String) mc.getFirst()).getValue();

      collectedModules.put(key, modules.stream()
          .map(m -> mapValue(m.toString(), "mod"))
          .collect(Collectors.toSet()));
    }
    return Collections.unmodifiableMap(collectedModules);
  }

  static java.util.Set<String> mapCourseSet(final Set value) {
    return Collections.unmodifiableSet( value.stream().map(Object::toString)
        .map(Mappers::mapString).collect(Collectors.toSet()));
  }

  static List<Integer> mapSessions(final Set modelResult) {
    return Collections.unmodifiableList(modelResult.stream().map(
        v -> mapValue(v.toString(), "session")).collect(Collectors.toList()));
  }

  static String mapSession(final Integer session) {
    return "session" + session;
  }

  static String mapToModuleChoice(final Map<String, List<Integer>> moduleChoice) {

    final String result = Joiner.on(',').join(moduleChoice.entrySet().stream().map(e -> "(\""
        + e.getKey()
        + "\" |-> {"
        + Joiner.on(',').join(e.getValue().stream().map(i -> "mod" + i).iterator())
        + "})").iterator());

    return "{" + result + "}";
  }

  static List<Alternative> mapAlternatives(final Set modelResult) {
    return Collections.unmodifiableList(modelResult.stream().map(
      o -> {
        Record record = (Record) o;
        String day = record.get("day").toString();
        return new Alternative(mapString(day),
          record.get("slot").toString());
      }).collect(Collectors.toList()));
  }

  static String mapString(final String str) {
    return str.substring(1, str.length() - 1);
  }
}
