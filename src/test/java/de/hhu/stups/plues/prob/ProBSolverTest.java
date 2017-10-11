package de.hhu.stups.plues.prob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.be4.classicalb.core.parser.exceptions.BCompoundException;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.EvalElementType;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob.model.representation.AbstractModel;
import de.prob.scripting.Api;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob.translator.Translator;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ProBSolverTest {
  private ProBSolver solver;
  private Trace trace;
  private StateSpace stateSpace;
  private CommandFactory commandFactory;
  private AbstractModel model;

  @Test
  public void unsatCoreModules() throws Exception {
    final String[] modelReturnValues = new String[] {"{mod1, mod99}"};

    final String op = "unsatCoreModules";
    final String predicate = "ccss={\"foo\", \"bar\"}";
    setupOperationCanBeExecuted(modelReturnValues, op, predicate);

    final Set<Integer> uc = solver.unsatCoreModules("foo", "bar");
    assertEquals(2, uc.size());
    assertTrue(uc.contains(1));
    assertTrue(uc.contains(99));
  }

  @Test
  public void unsatCoreAbstractUnits() throws Exception {
    final String[] modelReturnValues = new String[] {"{au2, au123}"};

    final String op = "unsatCoreAbstractUnits";
    final String predicate = "uc_modules={mod2, mod123}";
    setupOperationCanBeExecuted(modelReturnValues, op, predicate);

    final Set<Integer> uc = solver.unsatCoreAbstractUnits(Arrays.asList(2, 123));

    assertEquals(2, uc.size());
    assertTrue(uc.contains(2));
    assertTrue(uc.contains(123));
  }

  @Test
  public void unsatCoreGroups() throws Exception {
    final String[] modelReturnValues = new String[] {"{group32, group2123}"};

    final String op = "unsatCoreGroups";
    final String predicate = "uc_modules={mod2, mod87} & uc_abstract_units={au1, au99}";
    setupOperationCanBeExecuted(modelReturnValues, op, predicate);

    final Set<Integer> uc = solver.unsatCoreGroups(Arrays.asList(1, 99), Arrays.asList(2, 87));
    assertEquals(2, uc.size());
    assertTrue(uc.contains(32));
    assertTrue(uc.contains(2123));
  }

  @Test
  public void unsatCoreSessions() throws Exception {
    final String[] modelReturnValues = new String[] {"{session77, session1234}"};

    final String op = "unsatCoreSessions";
    final String predicate = "uc_groups={group1, group99}";
    setupOperationCanBeExecuted(modelReturnValues, op, predicate);

    final Set<Integer> uc = solver.unsatCoreSessions(Arrays.asList(1, 99));
    assertEquals(2, uc.size());
    assertTrue(uc.contains(77));
    assertTrue(uc.contains(1234));

  }

  /**
   * Setup state for test.
   */
  @Before
  public void setUp() throws SolverException, IOException, ModelTranslationError {
    this.stateSpace = mock(StateSpace.class);
    this.trace = mock(Trace.class);
    this.model = mock(ClassicalBModel.class);


    final Api api = mock(Api.class);
    final State state = mock(State.class);

    when(api.b_load("model")).thenReturn(stateSpace);
    when(stateSpace.asType(Trace.class)).thenReturn(trace);

    when(stateSpace.getModel()).thenReturn(model);

    when(trace.getCurrentState()).thenReturn(state);
    when(state.getId()).thenReturn("TEST-STATE-ID");

    when(trace.addTransitions(anyList())).thenReturn(trace);

    when(trace.execute("$setup_constants")).thenReturn(trace);
    when(trace.execute("$initialise_machine")).thenReturn(trace);

    this.commandFactory = mock(CommandFactory.class);
    this.solver = new ProBSolver(api, commandFactory, "model");
  }

  @Test
  public void interrupt() {
    this.solver.interrupt();
    verify(this.stateSpace).sendInterrupt();
  }

  @Test
  public void checkFeasibilityFeasibleCourse() throws Exception {
    final String[] t = new String[] {};
    setupOperationCanBeExecuted(t, "check", "ccss={\"NoFoo\"}");
    setupOperationCanBeExecuted(t, "check", "ccss={\"NoBar\"}");
    setupOperationCanBeExecuted(t, "check", "ccss={\"NoFoo\", \"NoBar\"}");
    assertTrue(solver.checkFeasibility("NoFoo", "NoBar"));
    final OperationPredicateKey key
        = new OperationPredicateKey("check", "ccss={\"NoFoo\", \"NoBar\"}");
    assertTrue(solver.getOperationExecutionCache().containsKey(key));
  }

  @Test(expected = SolverException.class)
  public void checkFeasibilityInfeasibleCourse() throws Exception {
    setupOperationCanBeExecuted(new String[] {}, "check", "ccss={\"NoFoo\"}");
    setupOperationCanBeExecuted(new String[] {}, "check", "ccss={\"NoBar\"}");
    setupOperationCannotBeExecuted("check", "ccss={\"NoFoo\", \"NoBar\"}");
    //
    solver.checkFeasibility("NoFoo", "NoBar");
  }

  @Test
  public void checkFeasibilityInfeasibleCourseCache() throws Exception {
    setupOperationCanBeExecuted(new String[] {},"check", "ccss={\"NoFoo\"}");
    setupOperationCanBeExecuted(new String[] {},"check", "ccss={\"NoBar\"}");
    setupOperationCannotBeExecuted("check", "ccss={\"NoFoo\", \"NoBar\"}");
    try {
      solver.checkFeasibility("NoFoo", "NoBar");
    } catch (final SolverException ignored) {
      // ignored
    } finally {
      final OperationPredicateKey key
          = new OperationPredicateKey("check", "ccss={\"NoFoo\", \"NoBar\"}");
      assertTrue(solver.getOperationExecutionCache().containsKey(key));
    }
  }


  @Test
  public void computeFeasiblity() throws Exception {
    final String op = "check";
    final String predicate = "ccss={\"foo\", \"bar\"}";
    final String[] modelReturnValues = new String[] {"{(au1,sem2)}", "{(au3,group4)}",
      "{(mod5 |-> au1), (mod5 |-> au2)}", "{\"foo\" |-> {mod5,mod6}}"};

    setupOperationCanBeExecuted(modelReturnValues, op, predicate);

    final Map<Integer, Integer> gc = new HashMap<>();
    final Map<Integer, Integer> sc = new HashMap<>();
    final Map<Integer, Set<Integer>> ac = new HashMap<>();
    final Map<String, Set<Integer>> mc = new HashMap<>();
    final Set<Integer> modules = new HashSet<>();

    ac.put(5, new HashSet<>(Arrays.asList(1, 2)));
    gc.put(3, 4);
    sc.put(1, 2);
    modules.add(5);
    modules.add(6);
    mc.put("foo", modules);


    final FeasibilityResult result = solver.computeFeasibility("foo", "bar");

    assertEquals(result.getGroupChoice(), gc);
    assertEquals(result.getSemesterChoice(), sc);
    assertEquals(result.getModuleChoice(), mc);
    assertEquals(result.getAbstractUnitChoice(), ac);
  }

  @Test
  public void computePartialFeasiblity() throws Exception {
    final String op = "checkPartial";
    final String predicate = "ccss={\"foo\", \"bar\"} "
                           + "& partialModuleChoice={(\"foo\" |-> {mod5})}"
                           + " & partialAbstractUnitChoice={(mod5, au7)}";
    final String[] modelReturnValues = new String[] {"{(au1,sem2)}", "{(au3,group4)}",
      "{(mod5, au1), (mod5, au11)}", "{\"foo\" |-> {mod5,mod6}}"};

    setupOperationCanBeExecuted(modelReturnValues, op, predicate);

    final Map<Integer, Integer> gc = new HashMap<>();
    final Map<Integer, Integer> sc = new HashMap<>();
    final Map<String, Set<Integer>> mc = new HashMap<>();
    final Map<Integer, Set<Integer>> ac = new HashMap<>();
    final Set<Integer> modules = new HashSet<>();

    gc.put(3, 4);
    sc.put(1, 2);
    ac.put(5, new HashSet<>(Arrays.asList(1, 11)));
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

    final Map<Integer, List<Integer>> partialAuc = new HashMap<>();
    partialAuc.put(5, Collections.singletonList(7));


    final FeasibilityResult result
        = solver.computePartialFeasibility(courses, partialMc, partialAuc);

    assertEquals(result.getGroupChoice(), gc);
    assertEquals(result.getSemesterChoice(), sc);
    assertEquals(result.getModuleChoice(), mc);
    assertEquals(result.getAbstractUnitChoice(), ac);

    assertTrue(solver.getOperationExecutionCache().containsKey(
        new OperationPredicateKey(op, predicate)));
  }

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
    assertEquals(solver.unsatCore("foo", "bar"), new HashSet<>(Arrays.asList(unsatCore)));
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
    assertTrue(solver.getOperationExecutionCache().containsKey(
        new OperationPredicateKey(op, predicate)));
  }

  @Test
  public void move() throws Exception {
    final String op = "move";
    final String predicate = "session=session101 & dow=mon & slot=slot8";

    final String[] t = new String[] {};
    setupOperationCanBeExecuted(t, op, predicate);

    solver.move("101", "mon", "8");
    assertTrue(solver.getOperationExecutionCache().isEmpty());

    verify(this.commandFactory).create(
        eq(stateSpace), eq("TEST-STATE-ID"), eq(op), any(IEvalElement.class), eq(1));

    verify(stateSpace).execute(any(GetOperationByPredicateCommandDelegate.class));
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
    assertTrue(solver.getOperationExecutionCache().containsKey(
        new OperationPredicateKey(op, predicate)));
    assertTrue(r.containsAll(alternatives));
  }

  @SuppressWarnings("UnusedParameters")
  private void setupOperationCannotBeExecuted(final String op, final String pred) throws Exception {
    final GetOperationByPredicateCommandDelegate cmd
        = mock(GetOperationByPredicateCommandDelegate.class);

    final IEvalElement evalElement = mock(IEvalElement.class);
    when(this.model.parseFormula(pred)).thenReturn(evalElement);

    when(this.commandFactory.create(
      eq(stateSpace), anyString(), eq(op), eq(evalElement), eq(1))).thenReturn(cmd);

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
    when(transition.evaluate(FormulaExpand.EXPAND)).thenReturn(transition);
    when(transition.getTranslatedReturnValues()).thenReturn(
        Arrays.stream(modelReturnValues).map(s -> {
          try {
            return Translator.translate(s);
          } catch (BCompoundException exception) {
            return null;
          }
        }).collect(Collectors.toList()));

    final GetOperationByPredicateCommandDelegate cmd
        = mock(GetOperationByPredicateCommandDelegate.class);

    final IEvalElement evalElement = mock(IEvalElement.class);
    when(this.model.parseFormula(eq(predicate))).thenReturn(evalElement);

    when(this.commandFactory.create(
      eq(stateSpace), anyString(), eq(op), eq(evalElement), eq(1))).thenReturn(cmd);

    when(cmd.isCompleted()).thenReturn(true);
    when(cmd.isInterrupted()).thenReturn(false);
    when(cmd.hasErrors()).thenReturn(false);


  }
}
