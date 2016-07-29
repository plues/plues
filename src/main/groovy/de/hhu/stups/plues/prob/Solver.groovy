package de.hhu.stups.plues.prob

import de.prob.animator.domainobjects.ClassicalB
import de.prob.animator.domainobjects.FormulaExpand
import de.prob.scripting.Api
import de.prob.statespace.StateSpace
import de.prob.statespace.Trace
import de.prob.translator.Translator

// TODO: migrate to java
class Solver {

    final Api api
    StateSpace state_space
    Trace trace
    Map cache


    def Solver(final Api api, String modelPath) {
        this.api = api
        this.state_space = api.b_load(modelPath)
        this.state_space.getSubscribedFormulas().each {
            state_space.unsubscribe(state_space, it)
        }
        this.state_space.getMainComponent()
        this.trace = traceFrom state_space
        this.cache = [:]
    }

    private Trace traceFrom(state_space) {
        (state_space as Trace).execute('$setup_constants').execute('$initialise_machine')
    }

    protected def getFromTrace(Trace trace, String value) {
        def formula = new ClassicalB(value, FormulaExpand.expand)
        def result = trace.evalCurrent(formula)
        Translator.translate(result.value)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    protected def executeOperation(String op) {
        executeOperation(op, "1=1")
    }

    protected def executeOperation(String op, String predicate) {
        if (trace.canExecuteEvent(op, predicate)) {
            trace = trace.execute(op, predicate)
            return true
        }
        false
    }
    protected executeOperationWithResult(String op, String predicate) {
        if(!executeOperation(op, predicate)) {
//            throw new AnomalousMaterialsException("Could not execute operation " + op + " - " + predicate);
            throw new Exception("Could not execute operation " + op + " - " + predicate);
        }

        def trans = trace.getCurrentTransition()
        def return_values = trans.evaluate(FormulaExpand.expand).getReturnValues()
        if (return_values.size() == 0) {
            return true
        }
        if (return_values.size() == 1) {
            return Translator.translate(return_values[0])
        }
        return return_values.collect { Translator.translate(it) }
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    protected def getFromTrace(String key) {
        if (cache.containsKey(key)) {
            return cache[key]
        }
        def value = getFromTrace trace, key
        cache[key] = value
        return value
    }

    public def checkPartialFeasibility(def major, def minor) {
        throw new UnsupportedOperationException("Not implemented yet")
        def op = "check"
        def predicate = "units=${units} & modules=${modules} & major=${major}"
        if (minor) {
            predicate += "& minor=${minor}"
        }

        executeOperation(op, predicate)
    }

    private def getFeasibilityPredicate(def courses) {
        courses = courses.findAll {it != null && it != ""};
        courses.join(" , ");
        "ccss={${courses.collect{ "\"${it}\""}.join(", ")}}"
    }
    public def checkFeasibility(String... courses) {
        def op = "check"
        def predicate = getFeasibilityPredicate(courses)
        executeOperation(op, predicate) as Boolean
    }

    public List<Map<Integer, Integer>> computeFeasibility(String ... courses) {
        def op = "check"
        def predicate = getFeasibilityPredicate(courses)
        /* Check returns values in the following order:
         *  0: Semester choice - map from abstract unit to a semester
         *  1: Group choice - map from unit to the group chosen for each
         *  2: Module choice - set of modules
         *  3: Unit choice - map from abstract units to units
         */
        def modelResult = executeOperationWithResult(op, predicate)
        /*
         * TODO: *Note* An alternative would be to map the non-numeric IDs in the model to the corresponding numeric
         * values and send those to the application. Then the mappers would only need to map B-Ints and Strings to the
         * corresponding Java types instead of extracting the IDs from the non-numeric ID strings
         */
        return  [Mappers.mapSemesterChoice(modelResult[0]),
                 Mappers.mapGroupChoice(modelResult[1]),
                 Mappers.mapModuleChoice(modelResult[2]),
                 Mappers.mapUnitChoice(modelResult[3])];
    }

    boolean move(String sessionId, String day, String slot) {
        def predicate = "session=session$sessionId & dow=$day & slot=slot$slot"
        executeOperation("move", predicate)
    }

    def conflicts(String ... courses) {
        throw new UnsupportedOperationException("Not implemented yet")
        // elements in courses might be null
        def formatted_courses = courses.findAll{ c -> c != null }.collect{ c -> "\"${c}\"" }.join(", ")
        def operation = "unsat_core_global"
        def predicate = "courses={${formatted_courses}"
        // TODO map model values to app values
        executeOperationWithResult(operation, predicate)
    }

    def alternatives(String idx, String course = null) {
        throw new UnsupportedOperationException("Not implemented yet")
        def op = 'alternatives'
        def predicate = "idx=${idx}"
        if (course) {
            op = "local_alternatives"
            predicate += " & course=${course}"
        }
        // TODO map model values to app values
        executeOperationWithResult(op, predicate)
    }

    void interrupt() {
        println "Sending interrupt to state space"
        try {
            state_space.sendInterrupt();
        } catch(Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
