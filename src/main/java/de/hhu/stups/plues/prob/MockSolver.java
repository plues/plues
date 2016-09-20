package de.hhu.stups.plues.prob;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MockSolver implements Solver {
  MockSolver() {
    try {
      TimeUnit.SECONDS.sleep(3);
    } catch (final InterruptedException exception) {
      exception.printStackTrace();
    }
  }

  @Override
  public void checkModelVersion(final String expectedVersion) throws SolverException {
  }

  @Override
  public void interrupt() {
  }

  @Override
  public Boolean checkFeasibility(final String... courses) {
    return false;
  }

  @Override
  public FeasibilityResult computeFeasibility(final String... courses) throws SolverException {
    final Map<String, Set<Integer>> moduleChoice = new HashMap<>();
    final Map<Integer, Integer> semesterChoice = new HashMap<>();
    final Map<Integer, Integer> groupChoice = new HashMap<>();
    return new FeasibilityResult(moduleChoice, semesterChoice, groupChoice);
  }

  @Override
  public FeasibilityResult computePartialFeasibility(
      final List<String> courses,
      final Map<String, List<Integer>> moduleChoice,
      final List<Integer> abstractUnitChoice) throws SolverException {

    final Map<String, Set<Integer>> mc = new HashMap<>();
    final Map<Integer, Integer> semesterChoice = new HashMap<>();
    final Map<Integer, Integer> groupChoice = new HashMap<>();

    return new FeasibilityResult(mc, semesterChoice, groupChoice);
  }

  @Override
  public List<Integer> unsatCore(final String... courses) throws SolverException {
    return new ArrayList<>();
  }

  @Override
  public void move(final String sessionId, final String day, final String slot) {

  }

  @Override
  public Set<String> getImpossibleCourses() throws SolverException {
    return new HashSet<>();
  }

  @Override
  public List<Alternative> getLocalAlternatives(final int session, final String... courses)
      throws SolverException {
    return new ArrayList<>();
  }

  @Override
  public String getModelVersion() throws SolverException {
    return "6.0.0-dev";
  }
}
