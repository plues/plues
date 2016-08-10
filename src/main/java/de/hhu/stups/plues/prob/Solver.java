package de.hhu.stups.plues.prob;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.be4.classicalb.core.parser.exceptions.BException;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.scripting.Api;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob.translator.Translator;
import de.prob.translator.types.BObject;
import de.prob.translator.types.Record;
import de.prob.translator.types.Set;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Solver {
    private static final String CHECK = "check";
    private static final String CHECK_PARTIAL = "checkPartial";
    private static final String MOVE = "move";
    private static final String IMPOSSIBLE_COURSES = "getImpossibleCourses";
    private static final String UNSAT_CORE = "unsatCore";
    private static final String LOCAL_ALTERNATIVES = "localAlternatives";
    
    public static final String DEFAULT_PREDICATE = "1=1";

    private Trace trace;
    private final StateSpace stateSpace;

    @Inject
    Solver(final Api api, @Assisted final String modelPath)
            throws IOException, BException {
        this.stateSpace = api.b_load(modelPath);
        this.stateSpace.getSubscribedFormulas()
                .forEach(it -> stateSpace.unsubscribe(this.stateSpace, it));
        this.trace = traceFrom(stateSpace);
    }

    private Trace traceFrom(final StateSpace space) {
        return ((Trace) space.asType(Trace.class))
                .execute("$setup_constants")
                .execute("$initialise_machine");
    }

    private Boolean executeOperation(final String op, final String predicate) {
        if(this.trace.canExecuteEvent(op, predicate)) {
            this.trace = this.trace.execute(op, predicate);
            return true;
        }
        return false;
    }

    private String getFeasibilityPredicate(final String[] courses) {
        Iterator<String> i = Arrays.stream(courses)
                .filter(it -> it != null && !it.equals(""))
                .map(it -> "\"" + it + "\"").iterator();
        return "ccss={" + Joiner.on(", ").join(i) + "}";
    }

    private <T extends BObject> T executeOperationWithOneResult(
            final String op,
            final String predicate,
            final Class<T> type) throws Exception {
        final List<T> modelResult = executeOperationWithResult(
                op, predicate, type);

        assert modelResult.size() == 1;
        return modelResult.get(0);
    }

    private <T extends BObject> T executeOperationWithOneResult(
            final String op,
            final Class<T> type) throws Exception {
        return executeOperationWithOneResult(op, DEFAULT_PREDICATE, type);
    }

    private <T extends BObject> List<T> executeOperationWithResult(
            final String op,
            final String predicate,
            final Class<T> type) throws Exception {

        if(!executeOperation(op, predicate)) {
//            throw new AnomalousMaterialsException("Could not execute operation " + op + " - " + predicate);
            throw new Exception("Could not execute operation "
                    + op + " - "
                    + predicate);
        }

        final Transition trans = trace.getCurrentTransition();
        final List<String> returnValues
                = trans.evaluate(FormulaExpand.expand).getReturnValues();

        return returnValues.stream().map(i -> {
            try {
                return type.cast(Translator.translate(i));
            } catch (BException e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList());
    }

    public final void interrupt() {
        System.out.println("Sending interrupt to state space");
        this.stateSpace.sendInterrupt();
    }

    // OPERATIONS

    /**
     * Check if a combination of major and minor courses is feasible.
     * @param courses The combination of major and minor courses.
     * @return Return true if the combination is feasible otherwise false.
     */
    public final Boolean checkFeasibility(final String... courses) {
        final String predicate = getFeasibilityPredicate(courses);
        return executeOperation(CHECK, predicate);
    }

    /**
     * Compute the {@link FeasibilityResult feasibility result} for a given combination of major and minor courses.
     * @param courses The combination of major and minor courses.
     * @return Return the computed {@link FeasibilityResult FeasibilityResult}.
     * @throws Exception
     */
    // TODO: proper exception type
    public final FeasibilityResult computeFeasibility(
            final String... courses) throws Exception {

        final String predicate = getFeasibilityPredicate(courses);
        /* Check returns values in the following order:
         *  0: Semester choice - map from abstract unit to a semester
         *  1: Group choice - map from unit to the group chosen for each
         *  2: Module choice - set of modules
         *  3: Unit choice - map from abstract units to units
         */
        final List<Set> modelResult
                = executeOperationWithResult(CHECK, predicate, Set.class);
        //
        final Map<Integer, Integer> semesterChoice
                = Mappers.mapSemesterChoice(modelResult.get(0));

        final Map<Integer, Integer> groupChoice
                = Mappers.mapGroupChoice(modelResult.get(1));

        final Map<String, java.util.Set<Integer>> moduleChoice
                = Mappers.mapModuleChoice(modelResult.get(2));

        final Map<Integer, Integer> unitChoice
                = Mappers.mapUnitChoice(modelResult.get(3));
        //
        return new FeasibilityResult(moduleChoice, unitChoice,
                semesterChoice, groupChoice);
    }

    // TODO: proper exception type
    public final FeasibilityResult computePartialFeasibility(
            final List<String> courses,
            final Map<String, List<Integer>> moduleChoice,
            final List<Integer> abstractUnitChoice) throws Exception {

        final String mc = Mappers.mapToModuleChoice(moduleChoice);
        final String ac = Joiner.on(',').join(
                abstractUnitChoice.stream().map(i -> "au" + i).iterator());

        final String predicate = getFeasibilityPredicate(
                courses.toArray(new String[0]))
                + " & partialModuleChoice=" + mc
                + " & partialAbstractUnitChoice={" + ac + "}";

        /* Check returns values in the following order:
         *  0: Semester choice - map from abstract unit to a semester
         *  1: Group choice - map from unit to the group chosen for each
         *  2: Module choice - set of modules
         *  3: Unit choice - map from abstract units to units
         */
        final List<Set> modelResult
                = executeOperationWithResult(CHECK_PARTIAL,
                predicate, Set.class);
        //
        final Map<Integer, Integer> computedSemesterChoice
                = Mappers.mapSemesterChoice(modelResult.get(0));

        final Map<Integer, Integer> computedGroupChoice
                = Mappers.mapGroupChoice(modelResult.get(1));

        final Map<String, java.util.Set<Integer>> computedModuleChoice
                = Mappers.mapModuleChoice(modelResult.get(2));

        final Map<Integer, Integer> computedUnitChoice
                = Mappers.mapUnitChoice(modelResult.get(3));
        //
        return new FeasibilityResult(computedModuleChoice, computedUnitChoice,
                computedSemesterChoice, computedGroupChoice);
    }

    // TODO: proper exception
    public final List<Integer> unsatCore(
            final String... courses) throws Exception {

        final String predicate = getFeasibilityPredicate(courses);
        //
        final Set uc = executeOperationWithOneResult(UNSAT_CORE,
                predicate,
                Set.class);
        //
        return Mappers.mapSessions(uc);
    }


    public final void move(final String sessionId,
                           final String day, final String slot) {
        String predicate = "session=session" + sessionId
                + " & dow=" + day
                + " & slot=slot" + slot;
        executeOperation(MOVE, predicate);
    }

    /**
     * A course is impossible if it is statically known to be infeasible.
     * @return Return the set of all impossible courses.
     * @throws Exception
     */
    public final java.util.Set<String> getImpossibleCourses() throws Exception {
        Record result = executeOperationWithOneResult(
                IMPOSSIBLE_COURSES, Record.class);
        return Mappers.mapCourseSet((Set) result.get("courses"));
    }

    // TODO: proper exception
    public final List<Alternative> getLocalAlternatives(
            final int session, final String... courses) throws Exception {

        final String coursePredicate = getFeasibilityPredicate(courses);
        final String predicate = coursePredicate + " & session=" + Mappers
                .mapSession(session);
        final Set modelResult = executeOperationWithOneResult(
                LOCAL_ALTERNATIVES,
                predicate, Set.class);

        return Mappers.mapAlternatives(modelResult);
    }

}
