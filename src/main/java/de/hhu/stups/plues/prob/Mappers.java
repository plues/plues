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
    static Map<Integer, Integer> mapSemesterChoice(Set p) {
        return convertToMap(p, "au", "sem");
    }

    static Map<Integer, Integer> mapGroupChoice(Set p) {
        return convertToMap(p, "unit", "group");
    }

    static Map<Integer, Integer> mapUnitChoice(Set p) {
        return convertToMap(p, "au", "unit");
    }

    private static Map<Integer, Integer> convertToMap(Set set, String keyPrefix, String valuePrefix) {
        return set.stream().collect(
                Collectors.toMap(
                        i -> mapValue((((Tuple) i).getFirst()).toString(), keyPrefix),
                        i -> mapValue((((Tuple) i).getSecond()).toString(), valuePrefix)));
    }

    private static Integer mapValue(String val, String prefix) {
        String idVal = val.substring(prefix.length(), val.length());
        return Integer.parseInt(idVal);
    }

    public static Map<String, java.util.Set<Integer>> mapModuleChoice(Set moduleChoice) {
        java.util.Map<java.lang.String, java.util.Set<Integer>> collectedModules = new HashMap<>();
        for (BObject o : moduleChoice) {
            Tuple mc = (Tuple) o;
            String key = ((de.prob.translator.types.String) mc.getFirst()).getValue();
            Set modules = (Set) mc.getSecond();

            collectedModules
                    .put(key, modules.stream().map(m -> mapValue(m.toString(), "mod")).collect(Collectors.toSet()));
        }
        return collectedModules;
    }

    public static java.util.Set<String> mapCourseSet(Set value) {
        return value.stream().map(i -> i.toString()).collect(Collectors.toSet());
    }

    public static List<Integer> mapSessions(Set modelResult) {
        return modelResult.stream().map(
                v -> mapValue(v.toString(), "session"))
                .collect(Collectors.toList());
    }

    public static String mapToModuleChoice(Map<String, List<Integer>> moduleChoice) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        moduleChoice.entrySet().stream().forEach(e -> {
            sb.append("(\"");
            sb.append(e.getKey());
            sb.append("\" |-> {");
            sb.append(Joiner.on(',').join(e.getValue().stream().map(i -> "mod" + i).iterator()));
            sb.append("})");
        });
        sb.append("}");

        return sb.toString();
    }
}
