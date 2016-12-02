package de.hhu.stups.plues.ui.controller;


import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.Unit;
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
import de.hhu.stups.plues.ui.components.reports.UnitsWithoutAbstractUnits;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.xmlgraphics.util.MimeConstants;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import org.jtwig.environment.EnvironmentConfiguration;
import org.jtwig.environment.EnvironmentConfigurationBuilder;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

class Reports extends VBox implements Initializable {

  private final ObjectProperty<ReportData> reportData = new SimpleObjectProperty<>();
  private final Properties properties;
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
  private Button buttonPrint;
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
  public Reports(final Inflater inflater, final Delayed<Store> delayedStore,
                 final Delayed<SolverService> delayedSolverService,
                 final ExecutorService executor,
                 final Properties properties) {

    this.properties = properties;
    resources = new HashMap<>();

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

    reportData.addListener((observable, oldValue, newValue) ->
        delayedStore.whenAvailable(store -> {
          buttonPrint.setDisable(false);
          printReportData = new PrintReportData(store, newValue, resources);

          this.mandatoryModules.setData(printReportData.getMandatoryModules());
          this.abstractUnitsWithoutUnits.setData(printReportData.getAbstractUnitsWithoutUnits());
          this.impossibleCourseModuleAbstractUnitPairs.setData(
              printReportData.getImpossibleCourseModuleAbstractUnitPairs());
          this.impossibleCourseModuleAbstractUnits.setData(
              printReportData.getImpossibleCourseModuleAbstractUnits());
          this.quasiMandatoryModuleAbstractUnits.setData(
              printReportData.getQuasiMandatoryModuleAbstractUnits());
          this.impossibleModules.setData(printReportData.getIncompleteModules(),
              printReportData.getImpossibleModulesBecauseOfMissingElectiveAbstractUnits());
          this.impossibleCourses.setData(printReportData.getImpossibleCourses(),
              printReportData.getImpossibleCoursesBecauseOfImpossibleModules(),
              printReportData.getImpossibleCoursesBecauseOfImpossibleModuleCombinations());
          this.redundantUnitGroups.setData(printReportData.getRedundantUnitGroups());
          this.unitsWithoutAbstractUnits.setData(printReportData.getUnitsWithoutAbstractUnits());
          this.moduleAbstractUnitUnitSemesterConflicts.setData(
              printReportData.getModuleAbstractUnitUnitSemesterConflicts());

          lbImpossibleCoursesAmount.setText(String.valueOf(newValue.getImpossibleCourses().size()));
        }));

    inflater.inflate("Reports", this, this, "reports", "Column");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
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
  public void print() {
    printReportData.print();
  }

  /**
   * @param reportData The {@link ReportData report data} object.
   */
  @SuppressWarnings("unused")
  private void setReportData(final ReportData reportData) {
    this.reportData.set(reportData);
  }

  private static final class PrintReportData {

    private Map<Course, Set<Module>> mandatoryModules;
    private Map<Module, Set<AbstractUnit>> quasiMandatoryModuleAbstractUnits;
    private Set<Unit> redundantUnitGroups;
    private Map<Course, Map<Module, Set<AbstractUnit>>>
        impossibleCourseModuleAbstractUnits;
    private Map<Course, Map<Module, Set<Pair<AbstractUnit>>>>
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

    private final Logger logger = Logger.getLogger(getClass().getSimpleName());
    private final String faculty;
    private final Map<String, String> resources;

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
                pair -> new Pair<>(store.getAbstractUnitById(pair.getFirst()),
                  store.getAbstractUnitById(pair.getSecond()))).collect(Collectors.toSet())))));
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
      this.impossibleCourses = reportData.getImpossibleCourses()
        .stream().map(store::getCourseByKey).collect(Collectors.toList());
      this.impossibleCoursesBecauseOfImpossibleModules =
        reportData.getImpossibleCoursesBecauseofImpossibleModules()
          .stream().map(store::getCourseByKey).collect(Collectors.toList());
      this.impossibleCoursesBecauseOfImpossibleModuleCombinations =
        reportData.getImpossibleCoursesBecauseOfImpossibleModuleCombinations()
          .stream().map(store::getCourseByKey).collect(Collectors.toList());
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
        .filter(unit -> unit.getAbstractUnits().size() == 0).collect(Collectors.toList());
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

    Set<Unit> getRedundantUnitGroups() {
      return redundantUnitGroups;
    }

    Map<Course, Map<Module, Set<Pair<AbstractUnit>>>> getImpossibleCourseModuleAbstractUnitPairs() {
      return impossibleCourseModuleAbstractUnitPairs;
    }

    void print() {
      try {
        final URL logo = getClass().getResource("/images/HHU_Logo.jpeg");
        final EnvironmentConfiguration config = EnvironmentConfigurationBuilder.configuration()
            .render().withOutputCharset(Charset.forName("utf8")).and().build();

        final JtwigModel model = JtwigModel.newModel()
            .with("date", new SimpleDateFormat("dd.MM.yyyy").format(new Date()))
            .with("faculty", faculty)
            .with("resources", resources)
            .with("incompleteModules", incompleteModules)
            .with("impossibleModulesBecauseOfMissingElectiveAbstractUnits",
                impossibleModulesBecauseOfMissingElectiveAbstractUnits)
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

        // load template
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final JtwigTemplate template =
            JtwigTemplate.classpathTemplate("/ui/controller/reportTemplate.twig", config);
        template.render(model, out);

        // write to file
        final File file = File.createTempFile("report", ".pdf");
        try (OutputStream stream = new FileOutputStream(file)) {
          final ByteArrayOutputStream pdf = toPdf(out);
          pdf.writeTo(stream);
        }
      } catch (SAXException | ParserConfigurationException | IOException exc) {
        logger.log(Level.SEVERE, "Exception while rendering reports", exc);
      }
    }

    private ByteArrayOutputStream toPdf(final ByteArrayOutputStream out)
        throws SAXException, ParserConfigurationException, IOException {
      final FopFactory fopFactory
          = FopFactory.newInstance(new File(".").toURI());
      final ByteArrayOutputStream pdf = new ByteArrayOutputStream();
      final Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, pdf);
      //
      final SAXParserFactory spf = SAXParserFactory.newInstance();
      spf.setNamespaceAware(true);
      final SAXParser saxParser = spf.newSAXParser();

      final XMLReader xmlReader = saxParser.getXMLReader();
      xmlReader.setContentHandler(fop.getDefaultHandler());
      xmlReader.parse(new InputSource(new ByteArrayInputStream(out.toByteArray())));
      //
      return pdf;
    }
  }
}
