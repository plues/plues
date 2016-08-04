package de.hhu.stups.plues.prob;

import com.google.common.base.Joiner;
import de.prob.translator.types.BObject;
import de.prob.translator.types.Set;
import de.prob.translator.types.Tuple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class Mappers {

    private Mappers() {
    }

    static Map<Integer, Integer> mapSemesterChoice(final Set p) {
        return convertToMap(p, "au", "sem");
    }

    static Map<Integer, Integer> mapGroupChoice(final Set p) {
        return convertToMap(p, "unit", "group");
    }

    static Map<Integer, Integer> mapUnitChoice(final Set p) {
        return convertToMap(p, "au", "unit");
    }

    private static Map<Integer, Integer> convertToMap(final Set set,
                                                      final String keyPrefix,
                                                      final String valuePrefix) {
        return set.stream().collect(
                Collectors.toMap(
                        i -> mapValue((((Tuple) i).getFirst()).toString(),
                                keyPrefix),
                        i -> mapValue((((Tuple) i).getSecond()).toString(),
                                valuePrefix)));
    }

    private static Integer mapValue(final String val, final String prefix) {
        final String idVal = val.substring(prefix.length(), val.length());
        return Integer.parseInt(idVal);
    }

    static Map<String, java.util.Set<Integer>> mapModuleChoice(
            final Set moduleChoice) {

        final java.util.Map<java.lang.String, java.util.Set<Integer>>
                collectedModules = new HashMap<>();

        for(final BObject o : moduleChoice) {

            final Tuple mc = (Tuple) o;
            final Set modules = (Set) mc.getSecond();

            final String key =
                    ((de.prob.translator.types.String) mc.getFirst())
                            .getValue();

            collectedModules
                    .put(key, modules.stream()
                            .map(m -> mapValue(m.toString(), "mod"))
                            .collect(Collectors.toSet()));
        }
        return collectedModules;
    }

    static java.util.Set<String> mapCourseSet(final Set value) {
        return value.stream().map(Object::toString).collect(Collectors.toSet());
    }

    static List<Integer> mapSessions(final Set modelResult) {
        return modelResult.stream().map(
                v -> mapValue(v.toString(), "session"))
                .collect(Collectors.toList());
    }

    static String mapToModuleChoice(
            final Map<String, List<Integer>> moduleChoice) {

        final StringBuilder sb = new StringBuilder();

        sb.append("{");
        moduleChoice.entrySet().forEach(e -> {
            sb.append("(\"");
            sb.append(e.getKey());
            sb.append("\" |-> {");

            sb.append(Joiner.on(',')
                    .join(e.getValue().stream()
                            .map(i -> "mod" + i).iterator()));

            sb.append("})");
        });
        sb.append("}");

        return sb.toString();
    }
}
