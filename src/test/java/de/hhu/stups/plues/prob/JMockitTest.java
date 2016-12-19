package de.hhu.stups.plues.prob;

import de.be4.classicalb.core.parser.exceptions.BException;
import de.prob.animator.command.GetOperationByPredicateCommand;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob.translator.Translator;

import javafx.stage.Stage;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testfx.framework.junit.ApplicationTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;


@RunWith(JMockit.class)
public class JMockitTest extends ApplicationTest {

  @Mocked @SuppressWarnings("unused") private ProBSolver solver;
  @Mocked @SuppressWarnings("unused") private Trace trace;
  @Mocked @SuppressWarnings("unused") private StateSpace stateSpace;

  @Test
  public void checkFeasibilityFeasibleCourse(@Mocked final GetOperationByPredicateCommand command,
                                             @Mocked final Transition transition)
      throws IOException, BException, SolverException {
    setUpCommandCanBeExecuted(command, transition, new String[]{});

    new Expectations() {
      {
        solver.checkFeasibility("foo", "bar");
        times = 1;
        result = true;
        solver.getOperationExecutionCache().containsKey(any);
        times = 1;
        result = true;
      }
    };

    Assert.assertTrue(solver.checkFeasibility("foo", "bar"));
    final OperationPredicateKey key = new OperationPredicateKey("check", "ccss={\"foo\", \"bar\"}");
    Assert.assertTrue(solver.getOperationExecutionCache().containsKey(key));
  }

  private Expectations setUpOperationCannotBeExecuted(
      final GetOperationByPredicateCommand command) {
    return new Expectations() {
      {
        command.hasErrors();
        result = true;
        command.getErrors();
        result = new ArrayList<>();
        command.isCompleted();
        result = true;
        command.isInterrupted();
        result = false;
        command.hasErrors();
        result = true;
      }
    };
  }

  private Expectations setUpCommandCanBeExecuted(final GetOperationByPredicateCommand command,
                                                 final Transition transition,
                                                 final String[] modelReturnValues)
      throws BException {
    return new Expectations() {
      {
        trace.getCurrentTransition();
        result = transition;
        transition.evaluate(FormulaExpand.expand);
        result = transition;
        transition.getTranslatedReturnValues();
        result = Arrays.stream(modelReturnValues).map(s -> {
          try {
            return Translator.translate(s);
          } catch (BException exception) {
            return null;
          }
        }).collect(Collectors.toList());
        command.isCompleted();
        result = true;
        command.isInterrupted();
        result = false;
        command.hasErrors();
        result = false;
      }
    };
  }

  @Override
  public void start(Stage stage) throws Exception {
    // empty
  }
}
