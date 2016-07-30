package de.hhu.stups.plues.prob;

import com.google.common.base.Joiner;
import de.be4.classicalb.core.parser.exceptions.BException;
import de.prob.scripting.Api;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

public class Solver {
    public static final String CHECK = "check";
    private Trace trace;
    private final Api api;
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

    public Boolean checkFeasibility(String... courses) {
        String op = CHECK;
        String predicate = getFeasibilityPredicate(courses);
        return executeOperation(op, predicate);
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

    public void interrupt() {
        System.out.println("Sending interrupt to state space");
        this.stateSpace.sendInterrupt();
    }
}
