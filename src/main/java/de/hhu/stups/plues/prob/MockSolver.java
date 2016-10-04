package de.hhu.stups.plues.prob;

import de.hhu.stups.plues.keys.MajorMinorKey;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MockSolver implements Solver {


  MockSolver() {
    try {
      TimeUnit.SECONDS.sleep(3);
    } catch (final InterruptedException exception) {
      final Logger logger = Logger.getLogger(getClass().getSimpleName());
      logger.log(Level.INFO, "sleep interrupted", exception);
    }
  }

  @Override
  public void checkModelVersion(final String expectedVersion) throws SolverException {
    // not needed for mock solver
  }

  @Override
  public void interrupt() {
    // not needed for mock solver
  }

  @Override
  public Boolean checkFeasibility(final String... courses) {
    return false;
  }

  @Override
  public FeasibilityResult computeFeasibility(final String... courses) throws SolverException {
    final Map<String, Set<Integer>> moduleChoice = Collections.emptyMap();
    final Map<Integer, Integer> semesterChoice = Collections.emptyMap();
    final Map<Integer, Integer> groupChoice = Collections.emptyMap();
    return new FeasibilityResult(moduleChoice, semesterChoice, groupChoice);
  }

  @Override
  public FeasibilityResult computePartialFeasibility(
      final List<String> courses,
      final Map<String, List<Integer>> moduleChoice,
      final List<Integer> abstractUnitChoice) throws SolverException {

    final Map<String, Set<Integer>> mc = Collections.emptyMap();
    final Map<Integer, Integer> semesterChoice = Collections.emptyMap();
    final Map<Integer, Integer> groupChoice = Collections.emptyMap();

    return new FeasibilityResult(mc, semesterChoice, groupChoice);
  }

  @Override
  public List<Integer> unsatCore(final String... courses) throws SolverException {
    return Collections.emptyList();
  }

  @Override
  public void move(final String sessionId, final String day, final String slot) {
    // not needed for mock solver
  }

  @Override
  public Set<String> getImpossibleCourses() throws SolverException {
    return Collections.emptySet();
  }

  @Override
  public List<Alternative> getLocalAlternatives(final int session, final String... courses)
      throws SolverException {
    return Collections.emptyList();
  }

  @Override
  public String getModelVersion() throws SolverException {
    return "";
  }

  @Override
  public final ObservableMap<MajorMinorKey, Boolean> getCourseCombinationResults() {
    return FXCollections.observableHashMap();
  }

}
