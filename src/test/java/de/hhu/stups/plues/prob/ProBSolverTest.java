package de.hhu.stups.plues.prob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.prob.animator.command.GetOperationByPredicateCommand;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.EvalElementType;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob.scripting.Api;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(PowerMockRunner.class)
@PrepareForTest( {ProBSolver.class, GetOperationByPredicateCommand.class})
public class ProBSolverTest {
  private ProBSolver solver;
  private Trace trace;
  private StateSpace stateSpace;

  /**
   * Setup state for test.
   */
  @Before
  public void setUp() throws Exception {
    this.stateSpace = mock(StateSpace.class);
    this.trace = mock(Trace.class);

    final Api api = mock(Api.class);
    final ClassicalBModel model = mock(ClassicalBModel.class);
    final ClassicalB evalElement = mock(ClassicalB.class);
    final State state = mock(State.class);

    when(evalElement.getKind()).thenReturn(String.valueOf(EvalElementType.PREDICATE));


    when(api.b_load("model")).thenReturn(stateSpace);
    when(stateSpace.asType(Trace.class)).thenReturn(trace);

    when(stateSpace.getModel()).thenReturn(model);
    when(model.parseFormula(anyString())).thenReturn(evalElement);

    when(trace.getCurrentState()).thenReturn(state);
    when(state.getId()).thenReturn("TEST-STATE-ID");

    when(trace.addTransitions(anyListOf(Transition.class))).thenReturn(trace);

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
    assertTrue(
        solver.getOperationExecutionCache().containsKey("check" + "ccss={\"foo\", \"bar\"}"));
    assertEquals(true,
                 solver.getOperationExecutionCache().get("check" + "ccss={\"foo\", \"bar\"}"));
  }

  @Test
  public void checkFeasibilityInfeasibleCourse() throws Exception {
    setupOperationCannotBeExecuted("check", "ccss={\"NoFoo\", \"NoBar\"}");
    assertFalse(solver.checkFeasibility("NoFoo", "NoBar"));
    assertTrue(
        solver.getOperationExecutionCache().containsKey("check" + "ccss={\"NoFoo\", \"NoBar\"}"));
    assertEquals(false,
                 solver.getOperationExecutionCache().get("check" + "ccss={\"NoFoo\", \"NoBar\"}"));
  }


  @Test
  public void computeFeasiblity() throws Exception {
    final String op = "check";
    final String predicate = "ccss={\"foo\", \"bar\"}";
    final String[] modelReturnValues = new String[] {"{(au1,sem2)}", "{(au3,group4)}", "{\"foo\" |-> {mod5,mod6}}"};

    setupOperationCanBeExecuted(modelReturnValues, op, predicate);

    final Map<Integer, Integer> gc = new HashMap<>();
    final Map<Integer, Integer> sc = new HashMap<>();
    final Map<String, Set<Integer>> mc = new HashMap<>();
    final Set<Integer> modules = new HashSet<>();

    gc.put(3, 4);
    sc.put(1, 2);
    modules.add(5);
    modules.add(6);
    mc.put("foo", modules);


    final FeasibilityResult result = solver.computeFeasibility("foo", "bar");

    assertEquals(result.getGroupChoice(), gc);
    assertEquals(result.getSemesterChoice(), sc);
    assertEquals(result.getModuleChoice(), mc);
  }

  @Test
  public void computePartialFeasiblity() throws Exception {
    final String op = "checkPartial";
    final String predicate = "ccss={\"foo\", \"bar\"} & "
        + "partialModuleChoice={(\"foo\" |-> {mod5})} & "
        + "partialAbstractUnitChoice={au7}";
    final String[] modelReturnValues = new String[] {"{(au1,sem2)}", "{(au3,group4)}",
        "{\"foo\" |-> {mod5,mod6}}"};

    setupOperationCanBeExecuted(modelReturnValues, op, predicate);

    final Map<Integer, Integer> gc = new HashMap<>();
    final Map<Integer, Integer> sc = new HashMap<>();
    final Map<String, Set<Integer>> mc = new HashMap<>();
    final Set<Integer> modules = new HashSet<>();

    gc.put(3, 4);
    sc.put(1, 2);
    modules.add(5);
    modules.add(6);
    mc.put("foo", modules);


    final List<String> courses = new ArrayList<>();
    courses.add("foo");
    courses.add("bar");

    final Map<String, List<Integer>> partialMc = new HashMap<>();
    final List<Integer> partialModules = new ArrayList<>();
    partialModules.add(5);
    partialMc.put("foo", partialModules);

    final List<Integer> partialAuc = new ArrayList<>();
    partialAuc.add(7);


    final FeasibilityResult result = solver.computePartialFeasibility(courses, partialMc, partialAuc);

    assertEquals(result.getGroupChoice(), gc);
    assertEquals(result.getSemesterChoice(), sc);
    assertEquals(result.getModuleChoice(), mc);

    assertTrue(solver.getSolverResultCache().containsKey(op + predicate));
  }

  // TODO: Proper exception
  @Test(expected = SolverException.class)
  public void computeFeasibilityInfeasibleCourse() throws Exception {
    final String op = "check";
    final String predicate = "ccss={\"foo\", \"bar\"}";
    setupOperationCannotBeExecuted(op, predicate);
    solver.computeFeasibility("foo", "bar");
  }

  @Test
  public void unsatCore() throws Exception {
    final String[] modelReturnValues = new String[] {"{session1, session77}"};

    final String op = "unsatCore";
    final String predicate = "ccss={\"foo\", \"bar\"}";
    setupOperationCanBeExecuted(modelReturnValues, op, predicate);


    final Integer[] unsatCore = new Integer[] {1, 77};
    assertEquals(solver.unsatCore("foo", "bar"), Arrays.asList(unsatCore));
  }

  @Test
  public void getImpossibleCourses() throws Exception {
    final String[] modelReturnValues = new String[] {
      "rec(courses:{\"BK-C1-H-2013\", \"BA-C2-N-2011\"})"};

    final String op = "getImpossibleCourses";
    final String predicate = "1=1";

    setupOperationCanBeExecuted(modelReturnValues, op, predicate);

    final String[] impossible = new String[] {"BK-C1-H-2013", "BA-C2-N-2011"};
    assertTrue(solver.getImpossibleCourses().containsAll(Arrays.asList(impossible)));
    assertTrue(solver.getSolverResultCache().containsKey(op + predicate));
  }

  @Test
  public void move() throws Exception {
    final String op = "move";
    final String predicate = "session=session101 & dow=mon & slot=slot8";

    final GetOperationByPredicateCommand f
        = PowerMockito.mock(GetOperationByPredicateCommand.class);
    PowerMockito.whenNew(GetOperationByPredicateCommand.class).withAnyArguments().thenReturn(f);
    when(f.isCompleted()).thenReturn(true);
    when(f.isInterrupted()).thenReturn(false);
    when(f.hasErrors()).thenReturn(false);

    solver.move("101", "mon", "8");
    assertTrue(solver.getSolverResultCache().isEmpty());
    assertTrue(solver.getOperationExecutionCache().isEmpty());
    PowerMockito.verifyNew(GetOperationByPredicateCommand.class).withArguments(eq(stateSpace),
        eq("TEST-STATE-ID"), eq(op), anyObject(), eq(1));

    verify(stateSpace).execute(any(GetOperationByPredicateCommand.class));
  }

  @Test
  public void alternatives() throws Exception {
    final String[] modelReturnValues = new String[] {
      "{rec(day:\"mon\", slot:slot1), rec(day:\"tue\", slot:slot2)}"};
    final String op = "localAlternatives";
    final String predicate = "ccss={\"foo\", \"bar\"} & session=session1";

    setupOperationCanBeExecuted(modelReturnValues, op, predicate);

    final List<Alternative> r = solver.getLocalAlternatives(1, "foo", "bar");

    final List<Alternative> alternatives = new ArrayList<>();

    alternatives.add(new Alternative("mon", "slot1"));
    alternatives.add(new Alternative("tue", "slot2"));

    assertTrue(r.containsAll(alternatives));
    assertTrue(solver.getSolverResultCache().containsKey(op + predicate));
    assertTrue(r.containsAll(alternatives));
  }

  @SuppressWarnings("UnusedParameters")
  private void setupOperationCannotBeExecuted(final String op, final String pred) throws Exception {
    final GetOperationByPredicateCommand cmd
        = PowerMockito.mock(GetOperationByPredicateCommand.class);

    PowerMockito.whenNew(GetOperationByPredicateCommand.class)
      .withArguments(eq(stateSpace), anyString(), eq(op), any(ClassicalB.class), eq(1))
      .thenReturn(cmd);

    when(cmd.hasErrors()).thenReturn(true);
    when(cmd.getErrors()).thenReturn(new ArrayList<>());

    when(cmd.isCompleted()).thenReturn(true);
    when(cmd.isInterrupted()).thenReturn(false);
    when(cmd.hasErrors()).thenReturn(true);
  }

  @SuppressWarnings("UnusedParameters")
  private void setupOperationCanBeExecuted(final String[] modelReturnValues, final String op,
                                           final String predicate) throws Exception {

    final Transition transition = mock(Transition.class);
    when(trace.getCurrentTransition()).thenReturn(transition);
    when(transition.evaluate(FormulaExpand.expand)).thenReturn(transition);
    when(transition.getReturnValues()).thenReturn(Arrays.asList(modelReturnValues));

    final GetOperationByPredicateCommand command
        = PowerMockito.mock(GetOperationByPredicateCommand.class);
    PowerMockito.whenNew(GetOperationByPredicateCommand.class)
      .withArguments(eq(stateSpace), anyString(), eq(op), any(ClassicalB.class), eq(1))
      .thenReturn(command);

    when(command.isCompleted()).thenReturn(true);
    when(command.isInterrupted()).thenReturn(false);
    when(command.hasErrors()).thenReturn(false);


  }
}
