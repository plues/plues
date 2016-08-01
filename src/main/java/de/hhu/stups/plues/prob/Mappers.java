package de.hhu.stups.plues.prob;

import de.prob.translator.types.BObject;
import de.prob.translator.types.Set;
import de.prob.translator.types.Tuple;

import java.util.HashMap;
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
}
