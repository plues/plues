package de.hhu.stups.plues.prob;

import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.scripting.Api;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;
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
    public void computeFeasiblity() throws Exception {
        String op = "check";
        String predicate = "ccss={\"foo\", \"bar\"}";
        String[] modelReturnValues = new String[]{"{(au1,sem2)}", "{(unit3,group4)}", "{\"foo\" |-> {mod5,mod6}}", "{(au7,unit8)}"};

        Transition transition = mock(Transition.class);

        when(trace.canExecuteEvent(op, predicate)).thenReturn(true);
        when(trace.execute(op, predicate)).thenReturn(trace);
        when(trace.getCurrentTransition()).thenReturn(transition);
        when(transition.evaluate(FormulaExpand.expand)).thenReturn(transition);
        when(transition.getReturnValues()).thenReturn(Arrays.asList(modelReturnValues));

        Map<Integer, Integer> gc = new HashMap<>();
        Map<Integer, Integer> sc = new HashMap<>();
        Map<Integer, Integer> uc = new HashMap<>();
        Map<String, Set<Integer>> mc = new HashMap<>();
        Set<Integer> modules = new HashSet<>();

        gc.put(3, 4);
        sc.put(1, 2);
        uc.put(7, 8);
        modules.add(5);
        modules.add(6);
        mc.put("foo", modules);


        FeasibilityResult r = solver.computeFeasibility("foo", "bar");

        assertEquals(r.getGroupChoice(), gc);
        assertEquals(r.getSemesterChoice(), sc);
        assertEquals(r.getUnitChoice(), uc);
        assertEquals(r.getModuleChoice(), mc);
    }

    @Test
    public void computePartialFeasiblity() throws Exception {
        String op = "checkPartial";
        String predicate = "ccss={\"foo\", \"bar\"} & partialModuleChoice={(\"foo\" |-> {mod5})} & partialAbstractUnitChoice={au7}";
        String[] modelReturnValues = new String[]{"{(au1,sem2)}", "{(unit3,group4)}", "{\"foo\" |-> {mod5,mod6}}", "{(au7,unit8)}"};

        Transition transition = mock(Transition.class);

        when(trace.canExecuteEvent(op, predicate)).thenReturn(true);
        when(trace.execute(op, predicate)).thenReturn(trace);
        when(trace.getCurrentTransition()).thenReturn(transition);
        when(transition.evaluate(FormulaExpand.expand)).thenReturn(transition);
        when(transition.getReturnValues()).thenReturn(Arrays.asList(modelReturnValues));

        Map<Integer, Integer> gc = new HashMap<>();
        Map<Integer, Integer> sc = new HashMap<>();
        Map<Integer, Integer> uc = new HashMap<>();
        Map<String, Set<Integer>> mc = new HashMap<>();
        Set<Integer> modules = new HashSet<>();

        gc.put(3, 4);
        sc.put(1, 2);
        uc.put(7, 8);
        modules.add(5);
        modules.add(6);
        mc.put("foo", modules);


        List<String> courses = new ArrayList<>();
        courses.add("foo");
        courses.add("bar");

        Map<String, List<Integer>> partialMC = new HashMap<>();
        List<Integer> partialModules = new ArrayList<>();
        partialModules.add(5);
        partialMC.put("foo", partialModules);

        List<Integer> partialAUC = new ArrayList<>();
        partialAUC.add(7);


        FeasibilityResult r = solver.computePartialFeasibility(courses, partialMC, partialAUC);

        assertEquals(r.getGroupChoice(), gc);
        assertEquals(r.getSemesterChoice(), sc);
        assertEquals(r.getUnitChoice(), uc);
        assertEquals(r.getModuleChoice(), mc);

    }

    // TODO: Proper exception
    @Test(expected = Exception.class)
    public void computeFeasibilityInfeasibleCourse() throws Exception {
        String op = "check";
        String predicate = "ccss={\"foo\", \"bar\"}";
        when(trace.canExecuteEvent(op, predicate)).thenReturn(false);
        solver.computeFeasibility("foo", "bar");
    }

    @Test
    public void unsatCore() throws Exception {
        Transition transition = mock(Transition.class);
        String[] modelReturnValues = new String[]{"{session1, session77}"};

        String op = "unsatCore";
        String predicate = "ccss={\"foo\", \"bar\"}";
        when(trace.canExecuteEvent(op, predicate)).thenReturn(true);


        when(trace.execute(op, predicate)).thenReturn(trace);
        when(trace.getCurrentTransition()).thenReturn(transition);
        when(transition.evaluate(FormulaExpand.expand)).thenReturn(transition);
        when(transition.getReturnValues()).thenReturn(Arrays.asList(modelReturnValues));

        Integer[] uc = new Integer[]{1, 77};
        assertEquals(solver.unsatCore("foo", "bar"), Arrays.asList(uc));
    }

    @Test
    public void move() throws Exception {
        String op = "move";
        String predicate = "session=session101 & dow=mon & slot=slot8";

        solver.move("101", "mon", "8");

        verify(trace).canExecuteEvent(op, predicate);
    }

}