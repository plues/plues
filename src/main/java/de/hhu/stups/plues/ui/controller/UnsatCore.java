package de.hhu.stups.plues.ui.controller;

import com.google.common.base.Joiner;
import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Group;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.ModuleAbstractUnitSemester;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.components.CombinationOrSingleCourseSelection;
import de.hhu.stups.plues.ui.layout.Inflater;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UnsatCore extends VBox implements Initializable {
  private final ObjectProperty<Store> store;
  private final ObjectProperty<SolverService> solverService;

  private final ListProperty<Session> sessions;
  private final ListProperty<Group> groups;
  private final ListProperty<AbstractUnit> abstractUnits;
  private final ListProperty<Module> modules;
  private final ListProperty<Course> courses;
  private final UiDataService uiDataService;
  private final ExecutorService executorService;

  @FXML
  private CombinationOrSingleCourseSelection courseSelection;
  @FXML
  private Button unsatCoreModulesButton;
  @FXML
  private Button unsatCoreAbstractUnitsButton;
  @FXML
  private Button unsatCoreGroupsButton;
  @FXML
  private Button unsatCoreSessionButton;

  @FXML
  private Label modulesTaskStateIcon;
  @FXML
  private Label modulesTaskStateLabel;
  @FXML
  private Label abstractUnitsTaskStateIcon;
  @FXML
  private Label abstractUnitsTaskStateLabel;
  @FXML
  private Label groupsTaskStateIcon;
  @FXML
  private Label groupsTaskStateLabel;
  @FXML
  private Label sessionsTaskStateIcon;
  @FXML
  private Label sessionsTaskStateLabel;

  @FXML
  private TableView<Module> modulesTable;
  @FXML
  private TableView<AbstractUnit> abstractUnitsTable;
  @FXML
  private TableView<Group> groupsTable;
  @FXML
  private TableView<Session> sessionsTable;
  @FXML
  private TableColumn<Module, String> modulePordnrColumn;
  @FXML
  private TableColumn<Module, String> moduleNameColumn;
  @FXML
  private TableColumn<AbstractUnit, String> abstractUnitKeyColumn;
  @FXML
  private TableColumn<AbstractUnit, String> abstractUnitTitleColumn;
  @FXML
  private TableColumn<AbstractUnit, Map<Module, List<Integer>>> abstractUnitModuleSemester;
  @FXML
  private TableColumn<Group, String> groupUnitKeyColumn;
  @FXML
  private TableColumn<Group, String> groupUnitTitleColumn;
  @FXML
  private TableColumn<Group, String> groupUnitSemestersColumn;
  @FXML
  private TableColumn<Group, Set<Session>> groupSessionsColumn;
  @FXML
  private TableColumn<Group, Set<AbstractUnit>> groupAbstractUnits;
  @FXML
  private TableColumn<Session, String> sessionDayColumn;
  @FXML
  private TableColumn<Session, Integer> sessionTimeColumn;
  @FXML
  private TableColumn<Session, String> sessionUnitKeyColumn;
  @FXML
  private TableColumn<Session, String> sessionUnitTitleColumn;
  @FXML
  private Pane abstractUnitsPane;
  @FXML
  private Pane modulesPane;
  @FXML
  private Pane groupsPane;
  @FXML
  private Pane sessionsPane;
  private ResourceBundle resources;

  /**
   * Constructor.
   *
   * @param inflater             Inflater to load FXML
   * @param delayedStore         Delayed Store
   * @param delayedSolverService Delayed Solver Service
   * @param uiDataService        UiDataService
   */
  @Inject
  public UnsatCore(final Inflater inflater, final Delayed<Store> delayedStore,
                   final Delayed<SolverService> delayedSolverService,
                   final ExecutorService executorService,
                   final UiDataService uiDataService) {

    this.uiDataService = uiDataService;
    this.executorService = executorService;
    this.solverService = new SimpleObjectProperty<>();
    this.store = new SimpleObjectProperty<>();

    this.sessions = new SimpleListProperty<>(FXCollections.emptyObservableList());
    this.groups = new SimpleListProperty<>(FXCollections.emptyObservableList());
    this.abstractUnits = new SimpleListProperty<>(FXCollections.emptyObservableList());
    this.modules = new SimpleListProperty<>(FXCollections.emptyObservableList());
    this.courses = new SimpleListProperty<>(FXCollections.emptyObservableList());


    delayedStore.whenAvailable(this.store::set);
    delayedSolverService.whenAvailable(this.solverService::set);

    inflater.inflate("UnsatCore", this, this, "unsatCore", "Column");
  }

  /**
   * Event handler for button to trigger UNSAT_CORE_MODULES computation.
   */
  @FXML
  @SuppressWarnings("unused")
  private void computeUnsatCoreModules() {
    final Course[] courses = getCoursesAsArray();
    final SolverTask<Set<Integer>> task = getSolverService().unsatCoreModules(courses);

    task.setOnSucceeded(event -> {
      final Set<Integer> moduleIds = task.getValue();
      this.modules.set(moduleIds.stream().map(getStore()::getModuleById).collect(Collectors
          .collectingAndThen(Collectors.toList(), FXCollections::observableArrayList)));

    });
    showTaskState(modulesTaskStateIcon, modulesTaskStateLabel, task);
    executorService.submit(task);
  }

  private void showTaskState(final Label icon, final Label message, final Task<?> task) {
    icon.graphicProperty().unbind();
    icon.styleProperty().unbind();
    message.textProperty().unbind();

    icon.graphicProperty().bind(PdfRenderingHelper.getIconBinding("25", task));
    icon.styleProperty().bind(PdfRenderingHelper.getStyleBinding(task));
    message.textProperty().bind(Bindings.createStringBinding(() -> {
      final String msg;
      switch (task.getState()) {
        case SUCCEEDED:
          msg = "";
          break;
        case CANCELLED:
          msg = this.resources.getString("task.Cancelled");
          break;
        case FAILED:
          msg = this.resources.getString("task.Failed");
          break;
        case READY:
        case SCHEDULED:
        case RUNNING:
        default:
          msg = this.resources.getString("task.Running");
          break;
      }
      return msg;
    }, task.stateProperty()));
  }

  /**
   * Event handler for button to trigger UNSAT_CORE_ABSTRACT_UNITS.
   */
  @FXML
  @SuppressWarnings("unused")
  private void computeUnsatCoreAbstractUnits() {
    final ObservableList<Module> mods = getModules();
    final SolverTask<Set<Integer>> task = getSolverService().unsatCoreAbstractUnits(mods);

    task.setOnSucceeded(event -> {
      final Set<Integer> abstractUnitIds = task.getValue();
      this.abstractUnits.set(abstractUnitIds.stream()
          .map(getStore()::getAbstractUnitById).collect(Collectors
            .collectingAndThen(Collectors.toList(), FXCollections::observableArrayList)));

    });
    showTaskState(abstractUnitsTaskStateIcon, abstractUnitsTaskStateLabel, task);
    executorService.submit(task);
  }

  /**
   * Event handler for button to trigger UNSAT_CORE_GROUPS.
   */
  @FXML
  @SuppressWarnings("unused")
  private void computeUnsatCoreGroups() {
    final ObservableList<Module> mods = getModules();
    final ObservableList<AbstractUnit> aUnits = getAbstractUnits();

    final SolverTask<Set<Integer>> task
        = getSolverService().unsatCoreGroups(aUnits, mods);

    task.setOnSucceeded(event -> {
      final Set<Integer> groupIds = task.getValue();
      this.groups.set(groupIds.stream().map(getStore()::getGroupById).collect(Collectors
          .collectingAndThen(Collectors.toList(), FXCollections::observableArrayList)));

    });
    showTaskState(groupsTaskStateIcon, groupsTaskStateLabel, task);
    executorService.submit(task);
  }

  /**
   * Event handler for button to trigger UNSAT_CORE_SESSIONS.
   */
  @FXML
  @SuppressWarnings("unused")
  private void computeUnsatCoreSessions() {
    final ObservableList<Group> groups = getGroups();

    final SolverTask<Set<Integer>> task
        = getSolverService().unsatCoreSessions(groups);

    task.setOnSucceeded(event -> {
      final Set<Integer> sessionIds = task.getValue();
      this.sessions.set(sessionIds.stream().map(getStore()::getSessionById).collect(
          Collectors.collectingAndThen(Collectors.toList(), FXCollections::observableArrayList)));

    });
    showTaskState(sessionsTaskStateIcon, sessionsTaskStateLabel, task);
    executorService.submit(task);
  }

  private Course[] getCoursesAsArray() {
    final ObservableList<Course> selectedCourses = this.getCourses();
    final Course[] courses = new Course[selectedCourses.size()];
    return selectedCourses.toArray(courses);
  }

  private SolverService getSolverService() {
    return solverService.get();
  }

  private Store getStore() {
    return store.get();
  }

  private ObservableList<Course> getCourses() {
    return this.courses.get();
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    this.resources = resources;
    store.addListener((observable, oldValue, store)
        -> courseSelection.setCourses(store.getCourses()));

    initializeCourses();
    initializeModules();
    initializeAbstractUnits();
    initializeGroups();
    initializeSessions();
    // buttons
    unsatCoreModulesButton.disableProperty().bind(
        solverService.isNull().or(courses.emptyProperty()).or(modules.emptyProperty().not()));
    unsatCoreAbstractUnitsButton.disableProperty().bind(
        modules.emptyProperty().or(abstractUnits.emptyProperty().not()));
    unsatCoreGroupsButton.disableProperty().bind(
        abstractUnits.emptyProperty().or(groups.emptyProperty().not()));
    unsatCoreSessionButton.disableProperty().bind(
        groups.emptyProperty().or(sessions.emptyProperty().not()));
  }

  private void initializeSessions() {
    sessionsPane.visibleProperty().bind(sessions.emptyProperty().not());
    sessionsTable.itemsProperty().bind(sessions);
    sessionDayColumn.setCellValueFactory(new PropertyValueFactory<>("day"));
    sessionTimeColumn.setCellValueFactory(new PropertyValueFactory<>("time"));
    sessionUnitKeyColumn.setCellValueFactory(param
        -> Bindings.selectString(param, "value", "group", "unit", "key"));
    sessionUnitTitleColumn.setCellValueFactory(param
        -> Bindings.selectString(param, "value", "group", "unit", "title"));
  }

  private void initializeGroups() {
    groupsPane.visibleProperty().bind(groups.emptyProperty().not());
    groupsTable.itemsProperty().bind(groups);
    groupUnitKeyColumn.setCellValueFactory(param
        -> Bindings.selectString(param, "value", "unit", "key"));
    groupUnitTitleColumn.setCellValueFactory(param
        -> Bindings.selectString(param, "value", "unit", "title"));
    groupUnitSemestersColumn.setCellValueFactory(param
        -> new SimpleStringProperty(
            Joiner.on(',').join(param.getValue().getUnit().getSemesters())));

    // display a bullet-list of sessions to represent the group
    groupSessionsColumn.setCellValueFactory(new PropertyValueFactory<>("sessions"));
    groupSessionsColumn.setCellFactory(param -> new TableCell<Group, Set<Session>>() {
      @Override
      protected void updateItem(final Set<Session> item, final boolean empty) {
        super.updateItem(item, empty);
        if (item == null || empty) {
          setText(null);
          return;
        }
        final String prefix = getPrefix(item);
        setText(item.stream()
            .map(s -> String.format("%s%s - %s\n", prefix, s.getDayString(), s.getTimeString()))
            .reduce(String::concat).orElse("??"));
      }
    });

    // extract abstract units associated to group (through unit) in the current abstract unit core
    groupAbstractUnits.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(
        param.getValue().getUnit().getAbstractUnits().stream()
          .filter(this.abstractUnits::contains)
            .collect(Collectors.toSet())));

    groupAbstractUnits.setCellFactory(param -> new TableCell<Group, Set<AbstractUnit>>() {
      @Override
      protected void updateItem(final Set<AbstractUnit> item, final boolean empty) {
        super.updateItem(item, empty);
        if (item == null || empty) {
          setText(null);
          return;
        }
        final String prefix = getPrefix(item);
        setText(item.stream()
            .map(e -> String.format("%s%s", prefix, e.getKey())).collect(Collectors.joining("\n")));
      }
    });
  }

  private String getPrefix(final Collection<?> item) {
    if (item.size() > 1) {
      return "• ";
    }
    return "";
  }

  private void initializeAbstractUnits() {
    abstractUnitsPane.visibleProperty().bind(abstractUnits.emptyProperty().not());
    abstractUnitsTable.itemsProperty().bind(abstractUnits);
    abstractUnitKeyColumn.setCellValueFactory(new PropertyValueFactory<>("key"));
    abstractUnitTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));

    abstractUnitModuleSemester.setCellValueFactory(param -> {
      final Set<ModuleAbstractUnitSemester> maus
          = param.getValue().getModuleAbstractUnitSemesters();

      // filter ModuleAbstractUnitSemester by those modules in the current unsat core
      final Stream<ModuleAbstractUnitSemester> filtered = maus.stream().filter(
          moduleAbstractUnitSemester
              -> this.modules.contains(moduleAbstractUnitSemester.getModule()));

      // group entries by module and map to the corresponding semesters as a list
      final Map<Module, List<Integer>> result = filtered.collect(
          Collectors.groupingBy(
            ModuleAbstractUnitSemester::getModule,
            Collectors.mapping(ModuleAbstractUnitSemester::getSemester, Collectors.toList())));
      return new ReadOnlyObjectWrapper<>(result);
    });
    abstractUnitModuleSemester.setCellFactory(param
        -> new TableCell<AbstractUnit, Map<Module, List<Integer>>>() {
            @Override
            protected void updateItem(final Map<Module, List<Integer>> item, final boolean empty) {
              super.updateItem(item, empty);
              if (item == null || empty) {
                setText(null);
                return;
              }
              final String prefix = getPrefix(item.entrySet());
              setText(item.entrySet().stream()
                  .map(e -> String.format("%s%s: %s",
                    prefix,
                    e.getKey().getPordnr(),
                    e.getValue().stream()
                      .sorted()
                      .map(String::valueOf)
                      .collect(Collectors.joining(","))))
                  .collect(Collectors.joining("\n")));
            }
          });
  }

  private void initializeModules() {
    modulesPane.visibleProperty().bind(modules.emptyProperty().not());
    modulesTable.itemsProperty().bind(modules);
    modulePordnrColumn.setCellValueFactory(new PropertyValueFactory<>("pordnr"));
    moduleNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
  }

  private void initializeCourses() {
    courseSelection.disableProperty().bind(store.isNull());
    courseSelection.impossibleCoursesProperty().bind(uiDataService.impossibleCoursesProperty());
    courses.bind(courseSelection.selectedCoursesProperty());
    courses.addListener((observable, oldValue, newValue) -> {
      // reset computed collections
      modules.set(FXCollections.emptyObservableList());
      abstractUnits.set(FXCollections.emptyObservableList());
      groups.set(FXCollections.emptyObservableList());
      sessions.set(FXCollections.emptyObservableList());

      // reset task state labels
      resetTaskState(abstractUnitsTaskStateIcon, abstractUnitsTaskStateLabel);
      resetTaskState(groupsTaskStateIcon, groupsTaskStateLabel);
      resetTaskState(modulesTaskStateIcon, modulesTaskStateLabel);
      resetTaskState(sessionsTaskStateIcon, sessionsTaskStateLabel);
    });
  }

  private void resetTaskState(final Label icon, final Label message) {
    icon.styleProperty().unbind();
    icon.setStyle("");
    //
    icon.graphicProperty().unbind();
    icon.setGraphic(null);
    //
    message.textProperty().unbind();
    message.setText("");
  }

  private ObservableList<Module> getModules() {
    return modules.get();
  }

  private ObservableList<AbstractUnit> getAbstractUnits() {
    return this.abstractUnits.get();
  }

  private ObservableList<Group> getGroups() {
    return this.groups.get();
  }
}
