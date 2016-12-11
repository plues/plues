package de.hhu.stups.plues.prob;

import org.hibernate.annotations.common.util.impl.LoggerFactory;
import org.jboss.logging.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;


public class MockSolver implements Solver {

  private final Logger logger = LoggerFactory.logger(getClass());

  MockSolver() {
    try {
      TimeUnit.SECONDS.sleep(3);
    } catch (final InterruptedException exception) {
      logger.info("sleep interrupted", exception);
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
  public Set<Integer> unsatCore(final String... courses) throws SolverException {
    return new HashSet<>(Arrays.asList(76, 7, 50, 43));
  }

  @Override
  public Set<Integer> unsatCoreModules(final String... courses) throws SolverException {
    try {
      TimeUnit.SECONDS.sleep(2);
    } catch (final InterruptedException exception) {
      logger.error("test", exception);
      throw new RuntimeException(exception);
    }
    return new HashSet<>(Arrays.asList(1, 2, 3));
  }

  @Override
  public Set<Integer> unsatCoreAbstractUnits(final List<Integer> modules) throws SolverException {
    try {
      TimeUnit.SECONDS.sleep(2);
    } catch (final InterruptedException exception) {
      logger.error("test", exception);
      throw new RuntimeException(exception);
    }
    return new HashSet<>(Arrays.asList(1,2,5,6,11));
  }

  @Override
  public Set<Integer> unsatCoreGroups(final List<Integer> abstractUnits,
      final List<Integer> modules) throws SolverException {
    return new HashSet<>(Arrays.asList(452, 455, 459, 456, 429, 426, 1527));
  }

  @Override
  public Set<Integer> unsatCoreSessions(final List<Integer> groups) {
    try {
      TimeUnit.SECONDS.sleep(2);
    } catch (final InterruptedException exception) {
      logger.error("test", exception);
      throw new RuntimeException(exception);
    }
    return new HashSet<>(Arrays.asList(1,100,1000));
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
  public ReportData getReportingData() throws SolverException {
    final ReportData reportData = new ReportData();
    reportData.setImpossibleCourseModuleAbstractUnits(new HashMap<>());
    reportData.setImpossibleCourses(new HashSet<>());
    reportData.setImpossibleCoursesBecauseOfImpossibleModules(new HashSet<>());
    reportData.setImpossibleCourseModuleAbstractUnitPairs(new HashMap<>());
    reportData.setImpossibleAbstractUnitsInModule(new HashMap<>());
    reportData.setIncompleteModules(new HashSet<>());
    reportData.setMandatoryModules(new HashMap<>());
    reportData.setQuasiMandatoryModuleAbstractUnits(new HashMap<>());
    reportData.setRedundantUnitGroups(new HashMap<>());
    reportData.setImpossibleModulesBecauseOfMissingElectiveAbstractUnits(new HashSet<>());
    reportData.setImpossibleCoursesBecauseOfImpossibleModuleCombinations(new HashSet<>());
    reportData.setModuleAbstractUnitUnitSemesterConflicts(new HashSet<>());

    return reportData;
  }

  @Override
  public String getModelVersion() throws SolverException {
    return "";
  }
}
