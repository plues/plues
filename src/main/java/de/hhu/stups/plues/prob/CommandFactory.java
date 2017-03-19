package de.hhu.stups.plues.prob;

import de.prob.animator.domainobjects.IEvalElement;
import de.prob.statespace.StateSpace;

@FunctionalInterface
public interface CommandFactory {
  GetOperationByPredicateCommandDelegate create(final StateSpace stateSpace,
                                                final String stateId,
                                                final String op,
                                                final IEvalElement evalElement,
                                                final int nrOfSolutions);
}
