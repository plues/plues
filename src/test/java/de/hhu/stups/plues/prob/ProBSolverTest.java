package de.hhu.stups.plues.prob;

import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.scripting.Api;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class ProBSolverTest {
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

        this.solver = new ProBSolver(api, "model");
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

        setupOperation(modelReturnValues, op, predicate);

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

        setupOperation(modelReturnValues, op, predicate);

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
    @Test(expected = SolverException.class)
    public void computeFeasibilityInfeasibleCourse() throws Exception {
        String op = "check";
        String predicate = "ccss={\"foo\", \"bar\"}";
        when(trace.canExecuteEvent(op, predicate)).thenReturn(false);
        solver.computeFeasibility("foo", "bar");
    }

    @Test
    public void unsatCore() throws Exception {
        String[] modelReturnValues = new String[]{"{session1, session77}"};

        String op = "unsatCore";
        String predicate = "ccss={\"foo\", \"bar\"}";
        setupOperation(modelReturnValues, op, predicate);

        Integer[] uc = new Integer[]{1, 77};
        assertEquals(solver.unsatCore("foo", "bar"), Arrays.asList(uc));
    }

    @Test
    public void getImpossibleCourses() throws Exception {
        String[] modelReturnValues = new String[]{
                "rec(courses:{\"BK-C1-H-2013\", \"BA-C2-N-2011\"})"};

        String op = "getImpossibleCourses";
        String predicate = "1=1";

        setupOperation(modelReturnValues, op, predicate);

        String[] impossible = new String[]{"BK-C1-H-2013", "BA-C2-N-2011"};
        assertTrue(solver.getImpossibleCourses()
                .containsAll(Arrays.asList(impossible)));
    }

    @Test
    public void move() throws Exception {
        String op = "move";
        String predicate = "session=session101 & dow=mon & slot=slot8";

        solver.move("101", "mon", "8");

        verify(trace).canExecuteEvent(op, predicate);
    }

    @Test
    public void alternatives() throws Exception {
        String[] modelReturnValues = new String[]{
                "{rec(day:\"mon\", slot:slot1), rec(day:\"tue\", slot:slot2)}"};
        String op = "localAlternatives";
        String predicate = "ccss={\"foo\", \"bar\"} & session=session1";

        setupOperation(modelReturnValues, op, predicate);

        final List<Alternative> r = solver.getLocalAlternatives(1, "foo", "bar");

        List<Alternative> alternatives = new ArrayList<>();

        alternatives.add(new Alternative("mon", "slot1"));
        alternatives.add(new Alternative("tue", "slot2"));

        assertTrue(r.containsAll(alternatives));
    }

    private void setupOperation(final String[] modelReturnValues, final String op, final String predicate) {
        final Transition transition = mock(Transition.class);

        when(trace.canExecuteEvent(op, predicate)).thenReturn(true);
        when(trace.execute(op, predicate)).thenReturn(trace);
        when(trace.getCurrentTransition()).thenReturn(transition);

        when(transition.evaluate(FormulaExpand.expand))
                .thenReturn(transition);
        when(transition.getReturnValues())
                .thenReturn(Arrays.asList(modelReturnValues));
    }
}
