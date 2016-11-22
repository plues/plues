package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.prob.ReportData;
import de.hhu.stups.plues.prob.report.Pair;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.components.reports.AbstractUnitsWithoutUnits;
import de.hhu.stups.plues.ui.components.reports.ImpossibleCourseModuleAbstractUnitPairs;
import de.hhu.stups.plues.ui.components.reports.ImpossibleCourseModuleAbstractUnits;
import de.hhu.stups.plues.ui.components.reports.ImpossibleCourses;
import de.hhu.stups.plues.ui.components.reports.ImpossibleModules;
import de.hhu.stups.plues.ui.components.reports.MandatoryModules;
import de.hhu.stups.plues.ui.components.reports.ModuleAbstractUnitUnitSemesterConflicts;
import de.hhu.stups.plues.ui.components.reports.QuasiMandatoryModuleAbstractUnits;
import de.hhu.stups.plues.ui.components.reports.RedundantUnitGroups;
import de.hhu.stups.plues.ui.layout.Inflater;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

class Reports extends VBox implements Initializable {

  private final ObjectProperty<ReportData> reportData = new SimpleObjectProperty<>();
  private final Properties properties;
  private int abstractUnitAmount;
  private int groupAmount;
  private int sessionAmount;
  private int courseAmount;
  private int unitAmount;

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

  /**
   * Reports view to present several reports and information about the loaded data, statistics,
   * etc.
   */
  @Inject
  public Reports(final Inflater inflater, final Delayed<Store> delayedStore,
                 final Delayed<SolverService> delayedSolverService,
                 final ExecutorService executor,
                 final Properties properties) {

    this.properties = properties;

    delayedStore.whenAvailable(store -> {
      groupAmount = store.getGroups().size();
      sessionAmount = store.getSessions().size();
      courseAmount = store.getCourses().size();
      unitAmount = store.getUnits().size();
      abstractUnitAmount = store.getAbstractUnits().size();
    });

    delayedSolverService.whenAvailable(solverService -> {
      final SolverTask<ReportData> reportDataTask = solverService.collectReportDataTask();
      reportDataTask.setOnSucceeded(event -> setReportData(reportDataTask.getValue()));
      executor.submit(reportDataTask);
    });

    reportData.addListener((observable, oldValue, newValue) -> {
      delayedStore.whenAvailable(store -> {
        displayImpossibleModules(store, newValue);
        displayImpossibleCourses(store, newValue);
        displayMandatoryModules(store, newValue);
        displayQuasiMandatoryModuleAbstractUnits(store, newValue);
        displayRedundantUnitGroups(store, newValue);
        displayImpossibleCourseModuleAbstractUnits(store, newValue);
        displayImpossibleCourseModuleAbstractUnitPairs(store, newValue);
        displayModuleAbstractUnitUnitSemesterConflicts(store, newValue);
        displayAbstractUnitsWithoutUnits(store);

        lbImpossibleCoursesAmount.setText(String.valueOf(newValue.getImpossibleCourses().size()));
      });
    });

    inflater.inflate("Reports", this, this, "reports");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    lbCourseAmount.setText(String.valueOf(courseAmount));
    lbUnitAmount.setText(String.valueOf(unitAmount));
    lbAbstractUnitAmount.setText(String.valueOf(abstractUnitAmount));
    lbGroupAmount.setText(String.valueOf(groupAmount));
    lbSessionAmount.setText(String.valueOf(sessionAmount));
    lbModelVersion.setText(String.valueOf(properties.get("model_version")));

  }

  /**
   * @param reportData The {@link ReportData report data} object.
   */
  @SuppressWarnings("unused")
  private void setReportData(final ReportData reportData) {
    this.reportData.set(reportData);
  }

  private void displayAbstractUnitsWithoutUnits(final Store store) {
    abstractUnitsWithoutUnits.setData(store.getAbstractUnitsWithoutUnits());
  }

  private void displayModuleAbstractUnitUnitSemesterConflicts(final Store store,
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
    moduleAbstractUnitUnitSemesterConflicts.setData(conflicts);
  }

  private void displayImpossibleCourseModuleAbstractUnitPairs(final Store store,
      final ReportData reportData) {
    impossibleCourseModuleAbstractUnitPairs.setData(
        reportData.getImpossibleCourseModuleAbstractUnitPairs()
        .entrySet().stream().collect(Collectors.toMap(
          entry -> store.getCourseByKey(entry.getKey()),
          entry -> entry.getValue().entrySet().stream().collect(Collectors.toMap(
            innerEntry -> store.getModuleById(innerEntry.getKey()),
            innerEntry -> innerEntry.getValue().stream().map(
                pair -> new Pair<>(store.getAbstractUnitById(pair.getFirst()),
                  store.getAbstractUnitById(pair.getSecond()))).collect(Collectors.toSet()))))));
  }

  private void displayImpossibleCourseModuleAbstractUnits(final Store store,
      final ReportData reportData) {
    impossibleCourseModuleAbstractUnits.setData(reportData.getImpossibleCourseModuleAbstractUnits()
        .entrySet().stream().collect(Collectors.toMap(
          entry -> store.getCourseByKey(entry.getKey()),
          entry -> entry.getValue().entrySet().stream().collect(Collectors.toMap(
            innerEntry -> store.getModuleById(innerEntry.getKey()),
            innerEntry -> innerEntry.getValue().stream().map(
                store::getAbstractUnitById).collect(Collectors.toSet()))))));
  }

  private void displayRedundantUnitGroups(final Store store, final ReportData reportData) {
    redundantUnitGroups.setData(reportData.getRedundantUnitGroups().keySet().stream()
        .map(store::getUnitById).collect(Collectors.toSet()));
  }

  private void displayQuasiMandatoryModuleAbstractUnits(final Store store,
      final ReportData reportData) {
    quasiMandatoryModuleAbstractUnits.setData(reportData.getQuasiMandatoryModuleAbstractUnits()
        .entrySet().stream().collect(Collectors.toMap(
          entry -> store.getModuleById(entry.getKey()),
          entry -> entry.getValue().stream().map(
              store::getAbstractUnitById).collect(Collectors.toSet()))));
  }

  private void displayMandatoryModules(final Store store, final ReportData reportData) {
    mandatoryModules.setData(reportData.getMandatoryModules()
        .entrySet().stream().collect(Collectors.toMap(
          entry -> store.getCourseByKey(entry.getKey()),
          entry -> entry.getValue().stream().map(
              store::getModuleById).collect(Collectors.toSet()))));
  }

  private void displayImpossibleCourses(final Store store, final ReportData reportData) {
    impossibleCourses.setData(reportData.getImpossibleCourses()
          .stream().map(store::getCourseByKey).collect(Collectors.toList()),
        reportData.getImpossibleCoursesBecauseofImpossibleModules()
          .stream().map(store::getCourseByKey).collect(Collectors.toList()),
        reportData.getImpossibleCoursesBecauseOfImpossibleModuleCombinations()
          .stream().map(store::getCourseByKey).collect(Collectors.toList()));
  }

  private void displayImpossibleModules(final Store store, final ReportData reportData) {
    impossibleModules.setData(reportData.getIncompleteModules()
        .stream().map(store::getModuleById).collect(Collectors.toList()),
        reportData.getImpossibleModulesBecauseOfMissingElectiveAbstractUnits()
        .stream().map(store::getModuleById).collect(Collectors.toList()));
  }
}
