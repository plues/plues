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

  private RealReportData realReportData;
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

          MultipleContent<List<Module>> modules = displayImpossibleModules(store, newValue);
          MultipleContent<List<Course>> courses = displayImpossibleCourses(store, newValue);
          Map<Course, Set<Module>> mandatoryModules = displayMandatoryModules(store, newValue);
          Map<Module, Set<AbstractUnit>> quasiMandatoryModuleAbstractUnits =
              displayQuasiMandatoryModuleAbstractUnits(store, newValue);
          Set<Unit> redundantUnitGroups = displayRedundantUnitGroups(store, newValue);
          Map<Course, Map<Module, Set<AbstractUnit>>> impossibleCourseModuleAbstractUnits =
              displayImpossibleCourseModuleAbstractUnits(store, newValue);
          Map<Course, Map<Module, Set<Pair<AbstractUnit>>>> impossibleCourseModuleAbstractUnitPairs
              = displayImpossibleCourseModuleAbstractUnitPairs(store, newValue);
          HashMap<Module, List<ModuleAbstractUnitUnitSemesterConflicts.Conflict>>
              moduleAbstractUnitUnitSemesterConflicts =
              displayModuleAbstractUnitUnitSemesterConflicts(store, newValue);
          List<Unit> unitsWithoutAbstractUnits = displayUnitsWithoutAbstractUnits(store);
          List<AbstractUnit> abstractUnitsWithoutUnits = displayAbstractUnitsWithoutUnits(store);

          realReportData = new RealReportData(modules.getFirst(), modules.getSecond(),
              courses.getFirst(), courses.getSecond(), courses.getThird(),
              mandatoryModules, quasiMandatoryModuleAbstractUnits,
              redundantUnitGroups, impossibleCourseModuleAbstractUnits,
              impossibleCourseModuleAbstractUnitPairs, moduleAbstractUnitUnitSemesterConflicts,
              unitsWithoutAbstractUnits, abstractUnitsWithoutUnits, store.getInfoByKey("name"),
              resources);

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
  public void print() {
    realReportData.print();
  }

  /**
   * @param reportData The {@link ReportData report data} object.
   */
  @SuppressWarnings("unused")
  private void setReportData(final ReportData reportData) {
    this.reportData.set(reportData);
  }

  private List<AbstractUnit> displayAbstractUnitsWithoutUnits(final Store store) {
    List<AbstractUnit> abstractUnitsWithoutUnits = store.getAbstractUnitsWithoutUnits();
    this.abstractUnitsWithoutUnits.setData(abstractUnitsWithoutUnits);

    return abstractUnitsWithoutUnits;
  }

  private HashMap<Module, List<ModuleAbstractUnitUnitSemesterConflicts.Conflict>>
      displayModuleAbstractUnitUnitSemesterConflicts(final Store store,
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

    return conflicts;
  }

  private Map<Course, Map<Module, Set<Pair<AbstractUnit>>>>
      displayImpossibleCourseModuleAbstractUnitPairs(final Store store,
                                                     final ReportData reportData) {
    Map<Course, Map<Module, Set<Pair<AbstractUnit>>>> impossibleCourseModuleAbstractUnitPairs =
        reportData.getImpossibleCourseModuleAbstractUnitPairs()
        .entrySet().stream().collect(Collectors.toMap(
          entry -> store.getCourseByKey(entry.getKey()),
          entry -> entry.getValue().entrySet().stream().collect(Collectors.toMap(
            innerEntry -> store.getModuleById(innerEntry.getKey()),
            innerEntry -> innerEntry.getValue().stream().map(
              pair -> new Pair<>(store.getAbstractUnitById(pair.getFirst()),
              store.getAbstractUnitById(pair.getSecond()))).collect(Collectors.toSet())))));
    this.impossibleCourseModuleAbstractUnitPairs.setData(
        impossibleCourseModuleAbstractUnitPairs);

    return impossibleCourseModuleAbstractUnitPairs;
  }

  private Map<Course, Map<Module, Set<AbstractUnit>>>
      displayImpossibleCourseModuleAbstractUnits(final Store store, final ReportData reportData) {
    Map<Course, Map<Module, Set<AbstractUnit>>> impossibleCourseModuleAbstractUnits =
        reportData.getImpossibleCourseModuleAbstractUnits()
        .entrySet().stream().collect(Collectors.toMap(
          entry -> store.getCourseByKey(entry.getKey()),
          entry -> entry.getValue().entrySet().stream().collect(Collectors.toMap(
            innerEntry -> store.getModuleById(innerEntry.getKey()),
            innerEntry -> innerEntry.getValue().stream().map(
            store::getAbstractUnitById).collect(Collectors.toSet())))));
    this.impossibleCourseModuleAbstractUnits.setData(impossibleCourseModuleAbstractUnits);

    return impossibleCourseModuleAbstractUnits;
  }

  private Set<Unit> displayRedundantUnitGroups(final Store store, final ReportData reportData) {
    Set<Unit> redundantUnitGroups = reportData.getRedundantUnitGroups().keySet().stream()
        .map(store::getUnitById).collect(Collectors.toSet());
    this.redundantUnitGroups.setData(redundantUnitGroups);

    return redundantUnitGroups;
  }

  private Map<Module, Set<AbstractUnit>>
      displayQuasiMandatoryModuleAbstractUnits(final Store store, final ReportData reportData) {
    Map<Module, Set<AbstractUnit>> quasiMandatoryModuleAbstractUnits =
        reportData.getQuasiMandatoryModuleAbstractUnits()
        .entrySet().stream().collect(Collectors.toMap(
          entry -> store.getModuleById(entry.getKey()),
          entry -> entry.getValue().stream().map(
          store::getAbstractUnitById).collect(Collectors.toSet())));
    this.quasiMandatoryModuleAbstractUnits.setData(quasiMandatoryModuleAbstractUnits);

    return quasiMandatoryModuleAbstractUnits;
  }

  private Map<Course, Set<Module>> displayMandatoryModules(final Store store,
                                                           final ReportData reportData) {
    Map<Course, Set<Module>> mandatoryModules = reportData.getMandatoryModules()
        .entrySet().stream().collect(Collectors.toMap(
          entry -> store.getCourseByKey(entry.getKey()),
          entry -> entry.getValue().stream().map(
          store::getModuleById).collect(Collectors.toSet())));
    this.mandatoryModules.setData(mandatoryModules);

    return mandatoryModules;
  }

  private MultipleContent<List<Course>> displayImpossibleCourses(final Store store,
                                                                 final ReportData reportData) {
    List<Course> impossibleCourses = reportData.getImpossibleCourses()
        .stream().map(store::getCourseByKey).collect(Collectors.toList());
    List<Course> impossibleCoursesBecauseOfImpossibleModules =
        reportData.getImpossibleCoursesBecauseofImpossibleModules()
          .stream().map(store::getCourseByKey).collect(Collectors.toList());
    List<Course> impossibleCoursesBecauseOfImpossibleModuleCombinations =
        reportData.getImpossibleCoursesBecauseOfImpossibleModuleCombinations()
          .stream().map(store::getCourseByKey).collect(Collectors.toList());
    this.impossibleCourses.setData(impossibleCourses,
        impossibleCoursesBecauseOfImpossibleModules,
        impossibleCoursesBecauseOfImpossibleModuleCombinations);

    return new MultipleContent<>(impossibleCourses, impossibleCoursesBecauseOfImpossibleModules,
        impossibleCoursesBecauseOfImpossibleModuleCombinations);
  }

  private MultipleContent<List<Module>> displayImpossibleModules(final Store store,
                                                                 final ReportData reportData) {
    List<Module> incompleteModules = reportData.getIncompleteModules()
        .stream().map(store::getModuleById).collect(Collectors.toList());
    List<Module> impossibleModulesBecauseOfMissingElectiveAbstractUnits =
        reportData.getImpossibleModulesBecauseOfMissingElectiveAbstractUnits()
        .stream().map(store::getModuleById).collect(Collectors.toList());
    impossibleModules.setData(incompleteModules,
        impossibleModulesBecauseOfMissingElectiveAbstractUnits);

    return new MultipleContent<>(incompleteModules,
      impossibleModulesBecauseOfMissingElectiveAbstractUnits);
  }

  private List<Unit> displayUnitsWithoutAbstractUnits(final Store store) {
    List<Unit> unitsWithoutAbstractUnits = store.getUnits().stream()
        .filter(unit -> unit.getAbstractUnits().size() == 0).collect(Collectors.toList());
    this.unitsWithoutAbstractUnits.setData(unitsWithoutAbstractUnits);

    return unitsWithoutAbstractUnits;
  }

  private static final class MultipleContent<T> {
    private final T first;
    private final T second;
    private final T third;

    public MultipleContent(final T first, final T second) {
      this.first = first;
      this.second = second;
      this.third = null;
    }

    public MultipleContent(final T first, final T second, final T third) {
      this.first = first;
      this.second = second;
      this.third = third;
    }

    public T getFirst() {
      return first;
    }

    public T getSecond() {
      return second;
    }

    public T getThird() {
      return third;
    }
  }

  private static final class RealReportData {

    private final Map<Course, Set<Module>> mandatoryModules;
    private final Map<Module, Set<AbstractUnit>> quasiMandatoryModuleAbstractUnits;
    private final Set<Unit> redundantUnitGroups;
    private final Map<Course, Map<Module, Set<AbstractUnit>>>
        impossibleCourseModuleAbstractUnits;
    private final Map<Course, Map<Module, Set<Pair<AbstractUnit>>>>
        impossibleCourseModuleAbstractUnitPairs;
    private final HashMap<Module, List<ModuleAbstractUnitUnitSemesterConflicts.Conflict>>
        moduleAbstractUnitUnitSemesterConflicts;
    private final List<Unit> unitsWithoutAbstractUnits;
    private final List<AbstractUnit> abstractUnitsWithoutUnits;

    private final Logger logger = Logger.getLogger(getClass().getSimpleName());
    private final String faculty;
    private final Map<String, String> resources;
    private final List<Module> incompleteModules;
    private final List<Module> impossibleModulesBecauseOfMissingElectiveAbstractUnits;
    private final List<Course> impossibleCourses;
    private final List<Course> impossibleCoursesBecauseOfImpossibleModules;
    private final List<Course> impossibleCoursesBecauseOfImpossibleModuleCombinations;

    RealReportData(final List<Module> incompleteModules,
                   final List<Module> impossibleModulesBecauseOfMissingElectiveAbstractUnits,
                   final List<Course> impossibleCourses,
                   final List<Course> impossibleCoursesBecauseOfImpossibleModules,
                   final List<Course> impossibleCoursesBecauseOfImpossibleModuleCombinations,
                   final Map<Course, Set<Module>> mandatoryModules,
                   final Map<Module, Set<AbstractUnit>> quasiMandatoryModuleAbstractUnits,
                   final Set<Unit> redundantUnitGroups,
                   final Map<Course, Map<Module, Set<AbstractUnit>>>
                     impossibleCourseModuleAbstractUnits,
                   final Map<Course, Map<Module, Set<Pair<AbstractUnit>>>>
                     impossibleCourseModuleAbstractUnitPairs,
                   final HashMap<Module,
                     List<ModuleAbstractUnitUnitSemesterConflicts.Conflict>>
                     moduleAbstractUnitUnitSemesterConflicts,
                   final List<Unit> unitsWithoutAbstractUnits,
                   final List<AbstractUnit> abstractUnitsWithoutUnits, String faculty,
                   final Map<String, String> resources) {
      this.incompleteModules = incompleteModules;
      this.impossibleModulesBecauseOfMissingElectiveAbstractUnits =
        impossibleModulesBecauseOfMissingElectiveAbstractUnits;
      this.impossibleCourses = impossibleCourses;
      this.impossibleCoursesBecauseOfImpossibleModules =
        impossibleCoursesBecauseOfImpossibleModules;
      this.impossibleCoursesBecauseOfImpossibleModuleCombinations =
        impossibleCoursesBecauseOfImpossibleModuleCombinations;
      this.mandatoryModules = mandatoryModules;
      this.quasiMandatoryModuleAbstractUnits = quasiMandatoryModuleAbstractUnits;
      this.redundantUnitGroups = redundantUnitGroups;
      this.impossibleCourseModuleAbstractUnits = impossibleCourseModuleAbstractUnits;
      this.impossibleCourseModuleAbstractUnitPairs = impossibleCourseModuleAbstractUnitPairs;
      this.moduleAbstractUnitUnitSemesterConflicts = moduleAbstractUnitUnitSemesterConflicts;
      this.unitsWithoutAbstractUnits = unitsWithoutAbstractUnits;
      this.abstractUnitsWithoutUnits = abstractUnitsWithoutUnits;

      this.faculty = faculty;
      this.resources = resources;
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
