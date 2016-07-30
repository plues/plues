package de.hhu.stups.plues.prob;

import com.google.common.base.Joiner;
import de.be4.classicalb.core.parser.exceptions.BException;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.scripting.Api;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob.translator.Translator;
import de.prob.translator.types.BObject;
import de.prob.translator.types.Set;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Solver {
    // TODO: move ops to an enum
    public static final String CHECK = "check";
    public static final String MOVE = "move";
    private final Api api;
    private Trace trace;
    private StateSpace stateSpace;

    public Solver(final Api api, String modelPath) throws IOException, BException {
        this.api = api;
        this.stateSpace = api.b_load(modelPath);
        this.stateSpace.getSubscribedFormulas().forEach(it -> this.stateSpace.unsubscribe(this.stateSpace, it));
        this.trace = traceFrom(stateSpace);
    }

    private Trace traceFrom(StateSpace stateSpace) {
        return ((Trace) stateSpace.asType(Trace.class)).execute("$setup_constants").execute("$initialise_machine");
    }

    private Boolean executeOperation(String op, String predicate) {
        if (this.trace.canExecuteEvent(op, predicate)) {
            this.trace = this.trace.execute(op, predicate);
            return true;
        }
        return false;
    }

    private String getFeasibilityPredicate(String[] courses) {
        Iterator<String> i = Arrays.asList(courses).stream()
                .filter(it -> it != null && !it.equals(""))
                .map(it -> "\"" + it + "\"").iterator();
        return "ccss={" + Joiner.on(", ").join(i) + "}";
    }

    private <T extends BObject> List<T> executeOperationWithResult(String op, String predicate, Class<T> type) throws Exception {
        if (!executeOperation(op, predicate)) {
//            throw new AnomalousMaterialsException("Could not execute operation " + op + " - " + predicate);
            throw new Exception("Could not execute operation " + op + " - " + predicate);
        }

        Transition trans = trace.getCurrentTransition();
        List<String> return_values = trans.evaluate(FormulaExpand.expand).getReturnValues();
        return return_values.stream().map(i -> {
            try {
                return type.cast(Translator.translate(i));
            } catch (BException e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList());
    }

    public void interrupt() {
        System.out.println("Sending interrupt to state space");
        this.stateSpace.sendInterrupt();
    }

    // OPERATIONS

    public Boolean checkFeasibility(String... courses) {
        String op = CHECK;
        String predicate = getFeasibilityPredicate(courses);
        return executeOperation(op, predicate);
    }

    // TODO: proper exception type
    public FeasibilityResult computeFeasibility(String... courses) throws Exception {
        String predicate = getFeasibilityPredicate(courses);
        /* Check returns values in the following order:
         *  0: Semester choice - map from abstract unit to a semester
         *  1: Group choice - map from unit to the group chosen for each
         *  2: Module choice - set of modules
         *  3: Unit choice - map from abstract units to units
         */
        List<Set> modelResult = executeOperationWithResult(CHECK, predicate, Set.class);
        Map<Integer, Integer> semesterChoice = Mappers.mapSemesterChoice(modelResult.get(0));
        Map<Integer, Integer> groupChoice = Mappers.mapGroupChoice(modelResult.get(1));
        Map<String, java.util.Set<Integer>> moduleChoice = Mappers.mapModuleChoice(modelResult.get(2));
        Map<Integer, Integer> unitChoice = Mappers.mapUnitChoice(modelResult.get(3));
        return new FeasibilityResult(moduleChoice, unitChoice, semesterChoice, groupChoice);
    }

    public void move(String sessionId, String day, String slot) {
        String predicate = "session=session" + sessionId + " & dow=" + day + " & slot=slot" + slot;
        executeOperation(MOVE, predicate);
    }
}
