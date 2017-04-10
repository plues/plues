package de.hhu.stups.plues.prob;

import de.prob.animator.domainobjects.IEvalElement;
import de.prob.statespace.StateSpace;

public class CommandDelegateFactory implements CommandFactory {
  @Override
  public GetOperationByPredicateCommandDelegate create(final StateSpace stateSpace,
                                                       final String stateId,
                                                       final String op,
                                                       final IEvalElement evalElement,
                                                       final int nrOfSolutions) {
    return new GetOperationByPredicateCommandDelegate(
        stateSpace, stateId, op, evalElement, nrOfSolutions);
  }
}
