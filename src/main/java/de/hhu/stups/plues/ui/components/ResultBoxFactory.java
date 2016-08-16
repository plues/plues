package de.hhu.stups.plues.ui.components;

import de.hhu.stups.plues.prob.FeasibilityResult;

import javafx.concurrent.Task;

public interface ResultBoxFactory {
  ResultBox create(Task<FeasibilityResult> task);
}
