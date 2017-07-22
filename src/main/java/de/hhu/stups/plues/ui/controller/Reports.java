package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.ObservableStore;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.Unit;
import de.hhu.stups.plues.prob.ReportData;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.TooltipAllocator;
import de.hhu.stups.plues.ui.components.reports.AbstractUnitPair;
import de.hhu.stups.plues.ui.components.reports.AbstractUnitsWithoutUnits;
import de.hhu.stups.plues.ui.components.reports.ImpossibleCourseModuleAbstractUnitPairs;
import de.hhu.stups.plues.ui.components.reports.ImpossibleCourseModuleAbstractUnits;
import de.hhu.stups.plues.ui.components.reports.ImpossibleCourses;
import de.hhu.stups.plues.ui.components.reports.ImpossibleModules;
import de.hhu.stups.plues.ui.components.reports.MandatoryModules;
import de.hhu.stups.plues.ui.components.reports.ModuleAbstractUnitUnitSemesterConflicts;
import de.hhu.stups.plues.ui.components.reports.QuasiMandatoryModuleAbstractUnits;
import de.hhu.stups.plues.ui.components.reports.RedundantUnitGroups;
import de.hhu.stups.plues.ui.components.reports.UnitsWithoutAbstractUnits;
import de.hhu.stups.plues.ui.layout.Inflater;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import org.jtwig.JtwigModel;
import org.reactfx.Subscription;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public class Reports extends VBox implements Initializable {

  private final ObjectProperty<ReportData> reportData = new SimpleObjectProperty<>();
  private final BooleanProperty dataOutOfSync = new SimpleBooleanProperty(false);
  private final Properties properties;
  private final ExecutorService executorService;
  private Subscription storeChanges;
  private SolverService solverService;
  private int abstractUnitAmount;
  private int groupAmount;
  private int sessionAmount;
  private int courseAmount;
  private int unitAmount;
  private Map<String, String> resources;

  private PrintReportData printReportData;
  @FXML
  @SuppressWarnings("unused")
  private Label lbCourseAmount;
  @FXML
  @SuppressWarnings("unused")
  private Label lbUnitAmount;
  @FXML
  @SuppressWarnings("unused")
  private Label lbImpossibleCoursesAmount;
  @FXML
  @SuppressWarnings("unused")
  private Label lbAbstractUnitAmount;
  @FXML
  @SuppressWarnings("unused")
  private Label lbGroupAmount;
  @FXML
  @SuppressWarnings("unused")
  private Label lbSessionAmount;
  @FXML
  @SuppressWarnings("unused")
  private Label lbModelVersion;
  @FXML
  @SuppressWarnings("unused")
  private Label lbOutOfSyncInfo;
  @FXML
  @SuppressWarnings("unused")
  private Tooltip outOfSyncHint;
  @FXML
  @SuppressWarnings("unused")
  private Button btPrint;
  @FXML
  @SuppressWarnings("unused")
  private Button btRecomputeData;
  @FXML
  @SuppressWarnings("unused")
  private ImpossibleModules impossibleModules;
  @FXML
  @SuppressWarnings("unused")
  private ImpossibleCourses impossibleCourses;
  @FXML
  @SuppressWarnings("unused")
  private MandatoryModules mandatoryModules;
  @FXML
  @SuppressWarnings("unused")
  private QuasiMandatoryModuleAbstractUnits quasiMandatoryModuleAbstractUnits;
  @FXML
  @SuppressWarnings("unused")
  private RedundantUnitGroups redundantUnitGroups;
  @FXML
  @SuppressWarnings("unused")
  private ImpossibleCourseModuleAbstractUnits impossibleCourseModuleAbstractUnits;
  @FXML
  @SuppressWarnings("unused")
  private ImpossibleCourseModuleAbstractUnitPairs impossibleCourseModuleAbstractUnitPairs;
  @FXML
  @SuppressWarnings("unused")
  private ModuleAbstractUnitUnitSemesterConflicts moduleAbstractUnitUnitSemesterConflicts;
  @FXML
  @SuppressWarnings("unused")
  private AbstractUnitsWithoutUnits abstractUnitsWithoutUnits;
  @FXML
  @SuppressWarnings("unused")
  private UnitsWithoutAbstractUnits unitsWithoutAbstractUnits;

  /**
   * Reports view to present several reports and information about the loaded data, statistics,
   * etc.
   */
  @Inject
  public Reports(final Inflater inflater,
                 final Delayed<ObservableStore> delayedStore,
                 final Delayed<SolverService> delayedSolverService,
                 final ExecutorService executorService,
                 final Properties properties) {
    this.executorService = executorService;
    this.properties = properties;
    resources = new HashMap<>();

    delayedStore.whenAvailable(store -> {
      groupAmount = store.getGroups().size();
      sessionAmount = store.getSessions().size();
      courseAmount = store.getCourses().size();
      unitAmount = store.getUnits().size();
      abstractUnitAmount = store.getAbstractUnits().size();
      storeChanges = store.getChanges().subscribe(s -> dataOutOfSync.setValue(true));
    });

    delayedSolverService.whenAvailable(solverService1 -> {
      this.solverService = solverService1;
      recomputeData();
    });

    reportData.addListener((observable, oldValue, newValue) ->
        delayedStore.whenAvailable(store -> {
          btPrint.setDisable(false);
          printReportData = new PrintReportData(store, newValue, resources);
          setSpecificData();
          lbImpossibleCoursesAmount.setText(String.valueOf(newValue.getImpossibleCourses().size()));
        }));

    inflater.inflate("Reports", this, this, "reports", "Column");
  }

  private void setSpecificData() {
    mandatoryModules.setData(printReportData.getMandatoryModules());
    abstractUnitsWithoutUnits.setData(printReportData.getAbstractUnitsWithoutUnits());
    impossibleCourseModuleAbstractUnitPairs.setData(
        printReportData.getImpossibleCourseModuleAbstractUnitPairs());
    impossibleCourseModuleAbstractUnits.setData(
        printReportData.getImpossibleCourseModuleAbstractUnits());
    quasiMandatoryModuleAbstractUnits.setData(
        printReportData.getQuasiMandatoryModuleAbstractUnits());
    impossibleModules.setData(printReportData.getIncompleteModules(),
        printReportData.getImpossibleModulesBecauseOfMissingElectiveAbstractUnits(),
        printReportData.getImpossibleModulesBecauseOfIncompleteQuasiMandatoryAbstractUnits());
    impossibleCourses.setData(printReportData.getImpossibleCourses(),
        printReportData.getImpossibleCoursesBecauseOfImpossibleModules(),
        printReportData.getImpossibleCoursesBecauseOfImpossibleModuleCombinations());
    redundantUnitGroups.setData(printReportData.getRedundantUnitGroups());
    unitsWithoutAbstractUnits.setData(printReportData.getUnitsWithoutAbstractUnits());
    moduleAbstractUnitUnitSemesterConflicts.setData(
        printReportData.getModuleAbstractUnitUnitSemesterConflicts());
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    lbOutOfSyncInfo.graphicProperty().bind(Bindings.createObjectBinding(() ->
        FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.INFO_CIRCLE, "12")));
    TooltipAllocator.showTooltipOnEnter(
        lbOutOfSyncInfo, outOfSyncHint, new SimpleBooleanProperty(false));

    btRecomputeData.disableProperty().bind(dataOutOfSync.not());
    btRecomputeData.visibleProperty().bind(dataOutOfSync);
    lbOutOfSyncInfo.visibleProperty().bind(dataOutOfSync);

    lbCourseAmount.setText(String.valueOf(courseAmount));
    lbUnitAmount.setText(String.valueOf(unitAmount));
    lbAbstractUnitAmount.setText(String.valueOf(abstractUnitAmount));
    lbGroupAmount.setText(String.valueOf(groupAmount));
    lbSessionAmount.setText(String.valueOf(sessionAmount));
    lbModelVersion.setText(String.valueOf(properties.get("model_version")));

    this.resources = resources.keySet().stream()
        .filter(s -> s.startsWith("title.") || s.startsWith("column")).collect(Collectors.toList())
        .stream().collect(Collectors.toMap(o -> o, resources::getString));
  }

  @FXML
  @SuppressWarnings("unused")
  public void printReport() {
    printReportData.print();
  }

  /**
   * Compute and update the report data.
   */
  @FXML
  @SuppressWarnings( {"unused", "WeakerAccess"})
  public void recomputeData() {
    final SolverTask<ReportData> reportDataTask = solverService.collectReportDataTask();
    reportDataTask.setOnSucceeded(event -> setReportData(reportDataTask.getValue()));
    reportDataTask.setOnCancelled(event -> dataOutOfSync.setValue(true));
    reportDataTask.setOnFailed(event -> dataOutOfSync.setValue(true));
    dataOutOfSync.setValue(false);
    executorService.submit(reportDataTask);
  }

  /**
   * @param reportData The {@link ReportData report data} object.
   */
  @SuppressWarnings("unused")
  private void setReportData(final ReportData reportData) {
    this.reportData.set(reportData);
  }

  /**
   * Free resources held by this component before it is closed.
   */
  void dispose() {
    if (storeChanges != null) {
      storeChanges.unsubscribe();
    }
  }

  private static final class PrintReportData {

    private final String faculty;
    private final Map<String, String> resources;
    private Map<Course, Set<Module>> mandatoryModules;
    private Map<Module, Set<AbstractUnit>> quasiMandatoryModuleAbstractUnits;
    private Set<Unit> redundantUnitGroups;
    private Map<Course, Map<Module, Set<AbstractUnit>>>
        impossibleCourseModuleAbstractUnits;
    private Map<Course, Map<Module, Set<AbstractUnitPair>>>
        impossibleCourseModuleAbstractUnitPairs;
    private Map<Module, List<ModuleAbstractUnitUnitSemesterConflicts.Conflict>>
        moduleAbstractUnitUnitSemesterConflicts;
    private List<Unit> unitsWithoutAbstractUnits;
    private List<Module> incompleteModules;
    private List<Module> impossibleModulesBecauseOfMissingElectiveAbstractUnits;
    private List<Course> impossibleCourses;
    private List<Course> impossibleCoursesBecauseOfImpossibleModules;
    private List<Course> impossibleCoursesBecauseOfImpossibleModuleCombinations;
    private List<AbstractUnit> abstractUnitsWithoutUnits;
    private Map<Module, Set<AbstractUnit>>
        impossibleModulesBecauseOfIncompleteQuasiMandatoryAbstractUnits;

    PrintReportData(final Store store, final ReportData reportData,
                    final Map<String, String> resources) {
      calculateAbstractUnitsWithoutUnits(store);
      calculateImpossibleCourseModuleAbstractUnitPairs(store, reportData);
      calculateImpossibleCourseModuleAbstractUnits(store, reportData);
      calculateImpossibleCourses(store, reportData);
      calculateImpossibleModules(store, reportData);
      calculateModuleAbstractUnitUnitSemesterConflicts(store, reportData);
      calculateMandatoryModules(store, reportData);
      calculateAbstractUnitsWithoutUnits(store);
      calculateRedundantUnitGroups(store, reportData);
      calculateUnitsWithoutAbstractUnits(store);
      calculateQuasiMandatoryModuleAbstractUnits(store, reportData);
      calculateImpossibleModulesBecauseOfIncompleteQuasiMandatoryAbstractUnits(store, reportData);

      this.faculty = store.getInfoByKey("name");
      this.resources = resources;
    }

    private void calculateAbstractUnitsWithoutUnits(final Store store) {
      this.abstractUnitsWithoutUnits = store.getAbstractUnitsWithoutUnits();
    }

    private void calculateModuleAbstractUnitUnitSemesterConflicts(final Store store,
                                                                  final ReportData reportData) {
      final HashMap<Module, List<ModuleAbstractUnitUnitSemesterConflicts.Conflict>> conflicts
          = new HashMap<>();
      reportData.getModuleAbstractUnitUnitSemesterConflicts().forEach(conflict -> {
        final Module module = store.getModuleById(conflict.getModuleId());
        if (conflicts.containsKey(module)) {
          conflicts.get(module).add(new ModuleAbstractUnitUnitSemesterConflicts.Conflict(
              store.getAbstractUnitById(conflict.getAbstractUnitId()),
              store.getUnitById(conflict.getUnitId()),
              conflict.getAbstractUnitSemesters()));
        } else {
          conflicts.put(module,
              new ArrayList<>(Collections.singletonList(
              new ModuleAbstractUnitUnitSemesterConflicts.Conflict(
                store.getAbstractUnitById(conflict.getAbstractUnitId()),
                store.getUnitById(conflict.getUnitId()),
                conflict.getAbstractUnitSemesters()))));
        }
      });
      this.moduleAbstractUnitUnitSemesterConflicts = conflicts;
    }

    private void calculateImpossibleCourseModuleAbstractUnitPairs(final Store store,
                                                                  final ReportData reportData) {
      this.impossibleCourseModuleAbstractUnitPairs =
        reportData.getImpossibleCourseModuleAbstractUnitPairs()
          .entrySet().stream().collect(Collectors.toMap(
            entry -> store.getCourseByKey(entry.getKey()),
            entry -> entry.getValue().entrySet().stream().collect(Collectors.toMap(
              innerEntry -> store.getModuleById(innerEntry.getKey()),
              innerEntry -> innerEntry.getValue().stream().map(
                pair -> new AbstractUnitPair(store.getAbstractUnitById(pair.getFirst()),
                store.getAbstractUnitById(pair.getSecond())))
              .collect(Collectors.toSet())))));
    }

    private void calculateImpossibleCourseModuleAbstractUnits(final Store store,
                                                              final ReportData reportData) {
      this.impossibleCourseModuleAbstractUnits =
        reportData.getImpossibleCourseModuleAbstractUnits()
          .entrySet().stream().collect(Collectors.toMap(
            entry -> store.getCourseByKey(entry.getKey()),
            entry -> entry.getValue().entrySet().stream().collect(Collectors.toMap(
              innerEntry -> store.getModuleById(innerEntry.getKey()),
              innerEntry -> innerEntry.getValue().stream().map(
              store::getAbstractUnitById).collect(Collectors.toSet())))));
    }

    private void calculateRedundantUnitGroups(final Store store,
                                              final ReportData reportData) {
      this.redundantUnitGroups = reportData.getRedundantUnitGroups().keySet().stream()
        .map(store::getUnitById).collect(Collectors.toSet());
    }

    private void calculateImpossibleModulesBecauseOfIncompleteQuasiMandatoryAbstractUnits(
        final Store store,
        final ReportData reportData) {
      this.impossibleModulesBecauseOfIncompleteQuasiMandatoryAbstractUnits =
        reportData.getImpossibleModulesBecauseOfIncompleteQuasiMandatoryAbstractUnits()
          .entrySet().stream().collect(Collectors.toMap(
            entry -> store.getModuleById(entry.getKey()),
            entry -> entry.getValue().stream().map(
            store::getAbstractUnitById).collect(Collectors.toSet())));
    }

    private void calculateQuasiMandatoryModuleAbstractUnits(final Store store,
                                                            final ReportData reportData) {
      this.quasiMandatoryModuleAbstractUnits =
        reportData.getQuasiMandatoryModuleAbstractUnits()
          .entrySet().stream().collect(Collectors.toMap(
            entry -> store.getModuleById(entry.getKey()),
            entry -> entry.getValue().stream().map(
            store::getAbstractUnitById).collect(Collectors.toSet())));
    }

    private void calculateMandatoryModules(final Store store,
                                           final ReportData reportData) {
      this.mandatoryModules = reportData.getMandatoryModules()
        .entrySet().stream().collect(Collectors.toMap(
          entry -> store.getCourseByKey(entry.getKey()),
          entry -> entry.getValue().stream().map(
            store::getModuleById).collect(Collectors.toSet())));
    }

    private void calculateImpossibleCourses(final Store store,
                                            final ReportData reportData) {
      this.impossibleCourses
        = getCoursesByKeys(store, reportData.getImpossibleCourses());
      this.impossibleCoursesBecauseOfImpossibleModules
        = getCoursesByKeys(store, reportData.getImpossibleCoursesBecauseofImpossibleModules());
      this.impossibleCoursesBecauseOfImpossibleModuleCombinations
        = getCoursesByKeys(store,
        reportData.getImpossibleCoursesBecauseOfImpossibleModuleCombinations());
    }

    private List<Course> getCoursesByKeys(final Store store, final Set<String> courseKeys) {
      return courseKeys.stream().map(store::getCourseByKey).collect(Collectors.toList());
    }

    private void calculateImpossibleModules(final Store store,
                                            final ReportData reportData) {
      this.incompleteModules = reportData.getIncompleteModules()
        .stream().map(store::getModuleById).collect(Collectors.toList());
      this.impossibleModulesBecauseOfMissingElectiveAbstractUnits =
        reportData.getImpossibleModulesBecauseOfMissingElectiveAbstractUnits()
          .stream().map(store::getModuleById).collect(Collectors.toList());
    }

    private void calculateUnitsWithoutAbstractUnits(final Store store) {
      this.unitsWithoutAbstractUnits = store.getUnits().stream()
        .filter(unit -> unit.getAbstractUnits().isEmpty()).collect(Collectors.toList());
    }

    Map<Course, Set<Module>> getMandatoryModules() {
      return mandatoryModules;
    }

    Map<Module, Set<AbstractUnit>> getQuasiMandatoryModuleAbstractUnits() {
      return quasiMandatoryModuleAbstractUnits;
    }

    Map<Module, List<ModuleAbstractUnitUnitSemesterConflicts.Conflict>>
        getModuleAbstractUnitUnitSemesterConflicts() {
      return moduleAbstractUnitUnitSemesterConflicts;
    }

    List<AbstractUnit> getAbstractUnitsWithoutUnits() {
      return abstractUnitsWithoutUnits;
    }

    List<Course> getImpossibleCourses() {
      return impossibleCourses;
    }

    List<Course> getImpossibleCoursesBecauseOfImpossibleModuleCombinations() {
      return impossibleCoursesBecauseOfImpossibleModuleCombinations;
    }

    List<Course> getImpossibleCoursesBecauseOfImpossibleModules() {
      return impossibleCoursesBecauseOfImpossibleModules;
    }

    List<Module> getImpossibleModulesBecauseOfMissingElectiveAbstractUnits() {
      return impossibleModulesBecauseOfMissingElectiveAbstractUnits;
    }

    List<Module> getIncompleteModules() {
      return incompleteModules;
    }

    List<Unit> getUnitsWithoutAbstractUnits() {
      return unitsWithoutAbstractUnits;
    }

    Map<Course, Map<Module, Set<AbstractUnit>>> getImpossibleCourseModuleAbstractUnits() {
      return impossibleCourseModuleAbstractUnits;
    }

    Map<Module, Set<AbstractUnit>>
        getImpossibleModulesBecauseOfIncompleteQuasiMandatoryAbstractUnits() {
      return impossibleModulesBecauseOfIncompleteQuasiMandatoryAbstractUnits;
    }

    Set<Unit> getRedundantUnitGroups() {
      return redundantUnitGroups;
    }

    Map<Course, Map<Module, Set<AbstractUnitPair>>> getImpossibleCourseModuleAbstractUnitPairs() {
      return impossibleCourseModuleAbstractUnitPairs;
    }

    void print() {
      PdfRenderingHelper.writeJtwigTemplateToPdfFile(getJtwigModel(),
          "/reports/templates/reportTemplate.twig", "report");
    }

    private JtwigModel getJtwigModel() {
      final URL logo = getClass().getResource("/images/HHU_Logo.jpeg");
      final LocalDate date = LocalDate.now();
      final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
      final String formattedDate = date.format(formatter);

      return JtwigModel.newModel()
        .with("date", formattedDate)
        .with("faculty", faculty)
        .with("resources", resources)
        .with("incompleteModules", incompleteModules)
        .with("impossibleModulesBecauseOfMissingElectiveAbstractUnits",
          impossibleModulesBecauseOfMissingElectiveAbstractUnits)
        .with("impossibleModulesBecauseOfIncompleteQuasiMandatoryAbstractUnits",
          impossibleModulesBecauseOfIncompleteQuasiMandatoryAbstractUnits)
        .with("impossibleCourses", impossibleCourses)
        .with("impossibleCoursesBecauseOfImpossibleModules",
          impossibleCoursesBecauseOfImpossibleModules)
        .with("impossibleCoursesBecauseOfImpossibleModuleCombinations",
          impossibleCoursesBecauseOfImpossibleModuleCombinations)
        .with("abstractUnitsWithoutUnits", abstractUnitsWithoutUnits)
        .with("unitsWithoutAbstractUnits", unitsWithoutAbstractUnits)
        .with("moduleAbstractUnitUnitSemesterConflicts",
          moduleAbstractUnitUnitSemesterConflicts)
        .with("mandatoryModules", mandatoryModules)
        .with("quasiMandatoryModuleAbstractUnits", quasiMandatoryModuleAbstractUnits)
        .with("redundantUnitGroups", redundantUnitGroups)
        .with("impossibleCourseModuleAbstractUnitPairs",
          impossibleCourseModuleAbstractUnitPairs)
        .with("impossibleCourseModuleAbstractUnits", impossibleCourseModuleAbstractUnits)
        .with("logo", logo);
    }
  }
}
