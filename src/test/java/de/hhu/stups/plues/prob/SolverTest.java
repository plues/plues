package de.hhu.stups.plues.prob;

import de.prob.scripting.Api;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;


public class SolverTest {
    private Solver solver;
    private Trace trace;
    private StateSpace stateSpace;

    @Before
    public void setUp() throws Exception {
        Api api = mock(Api.class);

        this.stateSpace = mock(StateSpace.class);
        this.trace = mock(Trace.class);

        when(api.b_load("model")).thenReturn(stateSpace);
        when(stateSpace.asType(Trace.class)).thenReturn(trace);

        when(trace.execute("$setup_constants")).thenReturn(trace);
        when(trace.execute("$initialise_machine")).thenReturn(trace);

        this.solver = new Solver(api, "model");
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void interrupt() throws Exception {
        this.solver.interrupt();
        verify(this.stateSpace).sendInterrupt();
    }

    @Test
    public void checkFeasibilityFeasibleCourse() throws Exception {
        when(trace.canExecuteEvent("check", "ccss={\"foo\", \"bar\"}")).thenReturn(true);
        assertTrue(solver.checkFeasibility("foo", "bar"));
    }

    @Test
    public void checkFeasibilityInfeasibleCourse() throws Exception {
        when(trace.canExecuteEvent(eq("check"), anyString())).thenReturn(false);
        assertFalse(solver.checkFeasibility("NoFoo", "NoBar"));
    }

    @Test
    public void move() throws Exception {
        String op = "move";
        String predicate = "session=session101 & dow=mon & slot=slot8";

        solver.move("101", "mon", "8");

        verify(trace).canExecuteEvent(op, predicate);
    }

}