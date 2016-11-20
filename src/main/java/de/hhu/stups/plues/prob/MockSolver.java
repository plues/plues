package de.hhu.stups.plues.prob;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Group;
import de.hhu.stups.plues.data.entities.Module;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
    return Arrays.asList(76, 7, 50, 43);
  }

  @Override
  public List<Integer> unsatCoreModules(final Course[] courses) throws SolverException {
    try {
      TimeUnit.SECONDS.sleep(2);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return Arrays.asList(1, 2, 3);
  }

  @Override
  public List<Integer> unsatCoreAbstractUnits(final List<Module> modules,
      final List<Course> courses) throws SolverException {
    try {
      TimeUnit.SECONDS.sleep(2);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return Arrays.asList(11, 12, 13);
  }

  @Override
  public List<Integer> unsatCoreGroups(final List<AbstractUnit> abstractUnits,
      final List<Module> modules, final List<Course> courses) throws SolverException {
    return Arrays.asList(1, 11, 21, 100, 203, 1527);
  }

  @Override
  public List<Integer> unsatCoreSessions(final List<Group> groups,
                                         final List<AbstractUnit> abstractUnits,
                                         final List<Module> modules,
                                         final List<Course> courses) {
    try {
      TimeUnit.SECONDS.sleep(2);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return Arrays.asList(1,100,1000);
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
