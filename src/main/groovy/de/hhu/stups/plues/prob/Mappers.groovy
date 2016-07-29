package de.hhu.stups.plues.prob

import de.prob.translator.types.BObject
import de.prob.translator.types.Set
import de.prob.translator.types.Tuple

import java.util.stream.Collectors

// TODO consider mapping to domain objects instead of plain ids
final class Mappers {
    static def mapSemesterChoice(def p) {
        convertToMap(p, "au", "sem");
    }

    static def mapGroupChoice(def p) {
        convertToMap(p, "unit", "group");
    }

    static def mapUnitChoice(def p) {
        convertToMap(p, "au", "unit");
    }

    static def mapModuleChoice(Set moduleChoice) {
        java.util.Map<java.lang.String, java.util.Set<Integer>> collectedModules = new HashMap<>();
        for(BObject o : moduleChoice) {
            Tuple mc = (Tuple) o;
            java.lang.String key = ((de.prob.translator.types.String) mc.getFirst()).getValue();
            Set modules = (Set) mc.getSecond();
            java.util.Set<Integer> s = new HashSet<>();
            collectedModules.put(key, s);
            for(BObject m : modules) {
                s.add(mapValue(m.toString(), "mod"))
            }
        }
        return collectedModules;
    }

    // convert method
    private static Map<Integer, Integer> convertToMap(Set set, String prefixKey, String prefixValue) {
        return set.stream().collect(Collectors.toMap(
                { i ->
                    String val = (((Tuple) i).getFirst()).toString();
                    mapValue(val, prefixKey);
                },
                { i ->
                    String val = (((Tuple) i).getSecond()).toString();
                    mapValue(val, prefixValue);
                }));
    }

    private static mapValue(String val, String prefix) {
        if (!val.startsWith(prefix)) {
//            throw new AnomalousMaterialsException("Unexpected key " + val + " does not have prefix " + prefix);
            throw new Exception("Unexpected key " + val + " does not have prefix " + prefix);
        }
        String idVal = val.substring(prefix.length(), val.length());
        Integer.parseInt(idVal);
    }
}
