package de.hhu.stups.plues.prob;

import de.prob.animator.command.AbstractCommand;
import de.prob.animator.command.GetOperationByPredicateCommand;
import de.prob.animator.command.IStateSpaceModifier;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.parser.ISimplifiedROMap;
import de.prob.prolog.output.IPrologTermOutput;
import de.prob.prolog.term.PrologTerm;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Transition;

import java.util.List;

class GetOperationByPredicateCommandDelegate extends AbstractCommand
    implements IStateSpaceModifier {

  public List<String> getErrors() {
    return command.getErrors();
  }

  public boolean hasErrors() {
    return command.hasErrors();
  }

  private final GetOperationByPredicateCommand command;

  public GetOperationByPredicateCommandDelegate(final StateSpace stateSpace,
                                                final String stateId,
                                                final String op,
                                                final IEvalElement evalElement,
                                                final int nrOfSolutions) {

    this.command
        = new GetOperationByPredicateCommand(stateSpace, stateId, op, evalElement, nrOfSolutions);
  }

  @Override
  public void writeCommand(final IPrologTermOutput pto) {
    this.command.writeCommand(pto);
  }

  @Override
  public void processResult(final ISimplifiedROMap<String, PrologTerm> bindings) {
    this.command.processResult(bindings);

  }

  @Override
  public List<Transition> getNewTransitions() {
    return this.command.getNewTransitions();
  }
}
