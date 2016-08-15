package de.hhu.stups.plues.ui.components;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.prob.FeasibilityResult;

import javafx.concurrent.Worker;

public interface ResultBoxFactory {
  ResultBox create(Worker<FeasibilityResult> task, Delayed<Store> delayedStore);
}
