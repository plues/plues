package de.hhu.stups.plues.ui.components;

import static de.hhu.stups.plues.ui.components.OpenFileHandler.tryOpenFile;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.ObservableStore;
import de.hhu.stups.plues.data.entities.Log;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.services.UiDataService;
import de.hhu.stups.plues.ui.components.timetable.TimetableMisc;
import de.hhu.stups.plues.ui.controller.PdfRenderingHelper;
import de.hhu.stups.plues.ui.exceptions.RenderingException;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import org.jtwig.environment.EnvironmentConfiguration;
import org.jtwig.environment.EnvironmentConfigurationBuilder;
import org.reactfx.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class ChangeLog extends VBox implements Initializable {

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final Delayed<ObservableStore> delayedStore;
  private final ObservableList<Log> logs;
  private final ObjectProperty<LocalDateTime> compare;
  private final Map<String, String> resourcesMap;

  private Subscription subscriptions;
  private ResourceBundle resources;
  private String faculty;

  @FXML
  @SuppressWarnings("unused")
  private Button btPrint;
  @FXML
  @SuppressWarnings("unused")
  private TableView<Log> persistentTable;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Log, Session> tableColumnSessionTemporary;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Log, String> tableColumnSourceTemporary;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Log, String> tableColumnTargetTemporary;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Log, LocalDateTime> tableColumnDateTemporary;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Log, Session> tableColumnSessionPersistent;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Log, String> tableColumnSourcePersistent;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Log, String> tableColumnTargetPersistent;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Log, LocalDateTime> tableColumnDatePersistent;
  @FXML
  @SuppressWarnings("unused")
  private TableView<Log> tempTable;

  /**
   * Constructor to create the change log.
   */
  @Inject
  public ChangeLog(final Inflater inflater, final UiDataService uiDataService,
                   final Delayed<ObservableStore> delayedStore) {
    this.delayedStore = delayedStore;
    this.compare = uiDataService.lastSavedDateProperty();
    this.logs = FXCollections.observableArrayList();
    resourcesMap = new HashMap<>();

    inflater.inflate("components/ChangeLog", this, this, "ChangeLog", "Days");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    this.resources = resources;
    initializeTableColumns();
    updateBinding();

    btPrint.disableProperty().bind(Bindings.size(persistentTable.getItems()).isEqualTo(0));

    delayedStore.whenAvailable(store -> {
      logs.addAll(store.getLogEntries());
      setSubscriptions(store);
      faculty = store.getInfoByKey("name");
    });
    resources.keySet().forEach(key -> resourcesMap.put(key, resources.getString(key)));
  }

  /**
   * Print the {@link #persistentTable persistent changes}.
   */
  @FXML
  @SuppressWarnings("unused")
  public void printChangeLog() {
    try {
      final JtwigModel model = getJtwigModel();
      final EnvironmentConfiguration config = EnvironmentConfigurationBuilder.configuration()
          .render().withOutputCharset(Charset.forName("utf8")).and().build();

      // load template
      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      final JtwigTemplate template =
          JtwigTemplate.classpathTemplate("/changelog/templates/ChangeLogTemplate.twig", config);
      template.render(model, out);

      // write to file
      final File file = File.createTempFile("changelog", ".pdf");
      try (final OutputStream stream = new FileOutputStream(file)) {
        final ByteArrayOutputStream pdf = PdfRenderingHelper.toPdf(out);
        pdf.writeTo(stream);
        tryOpenFile(file);
      }
    } catch (final RenderingException | IOException exc) {
      logger.error("Exception while rendering changelogs", exc);
    }
  }

  private JtwigModel getJtwigModel() {
    final URL logo = getClass().getResource("/images/HHU_Logo.jpeg");

    final LocalDate date = LocalDate.now();
    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    final String formattedDate = date.format(formatter);

    final Map<Integer, String> timeMap = new HashMap<>();
    TimetableMisc.timeMap.forEach(timeMap::put);

    return JtwigModel.newModel()
      .with("date", formattedDate)
      .with("faculty", faculty)
      .with("resources", resourcesMap)
      .with("timeMap", timeMap)
      .with("changeLogs", persistentTable.getItems())
      .with("logo", logo);
  }

  private void setSubscriptions(final ObservableStore store) {
    if (store == null) {
      return;
    }
    final Subscription removed = store.getChanges()
        .filter(storeChange -> storeChange.historyChangeType().isBack())
        .subscribe(value -> logs.remove(logs.size() - 1));
    final Subscription added = store.getChanges()
        .filter(storeChange -> storeChange.historyChangeType().isForward())
        .subscribe(value -> logs.add(store.getLastLogEntry()));
    subscriptions = added.and(removed);
  }

  private void initializeTableColumns() {
    final Callback<TableColumn.CellDataFeatures<Log, String>, ObservableValue<String>>
        srcColumnCallback = param -> new ReadOnlyStringWrapper(
        String.format("%s, %s", resources.getString(param.getValue().getSrcDay()),
        TimetableMisc.timeMap.get(param.getValue().getSrcTime())));

    final Callback<TableColumn.CellDataFeatures<Log, String>, ObservableValue<String>>
        targetColumnCallback = param -> new ReadOnlyStringWrapper(
        String.format("%s, %s", resources.getString(param.getValue().getTargetDay()),
        TimetableMisc.timeMap.get(param.getValue().getTargetTime())));

    setTemporaryColumnCellFactories(srcColumnCallback, targetColumnCallback);
    setPersistenColumnCellFactories(srcColumnCallback, targetColumnCallback);
  }

  private void setTemporaryColumnCellFactories(
      final Callback<TableColumn.CellDataFeatures<Log, String>, ObservableValue<String>>
      srcColumnCallback,
      final Callback<TableColumn.CellDataFeatures<Log, String>, ObservableValue<String>>
      targetColumnCallback) {
    tableColumnSessionTemporary.setCellValueFactory(new PropertyValueFactory<>("session"));
    tableColumnSourceTemporary.setCellValueFactory(srcColumnCallback);
    tableColumnTargetTemporary.setCellValueFactory(targetColumnCallback);
    tableColumnDateTemporary.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
  }

  private void setPersistenColumnCellFactories(
      final Callback<TableColumn.CellDataFeatures<Log, String>, ObservableValue<String>>
      srcColumnCallback,
      final Callback<TableColumn.CellDataFeatures<Log, String>, ObservableValue<String>>
      targetColumnCallback) {
    tableColumnSessionPersistent.setCellValueFactory(new PropertyValueFactory<>("session"));
    tableColumnSourcePersistent.setCellValueFactory(srcColumnCallback);
    tableColumnTargetPersistent.setCellValueFactory(targetColumnCallback);
    tableColumnDatePersistent.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
  }

  private void updateBinding() {
    final SortedList<Log> sortedList
        = logs.sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()));

    updateTemporaryBinding(sortedList);
    updatePersistensBindings(sortedList);
  }

  private void updatePersistensBindings(final SortedList<Log> sortedList) {
    final FilteredList<Log> persistentList = new FilteredList<>(sortedList);
    persistentList.predicateProperty().bind(Bindings.createObjectBinding(
        () -> log -> log.getCreatedAt().compareTo(compare.get()) < 0, compare));
    getPersistentTable().itemsProperty().bind(new SimpleListProperty<>(persistentList));
  }

  private void updateTemporaryBinding(final SortedList<Log> sortedList) {
    final FilteredList<Log> tempList = new FilteredList<>(sortedList);
    tempList.predicateProperty().bind(Bindings.createObjectBinding(
        () -> log -> log.getCreatedAt().compareTo(compare.get()) > 0, compare));
    getTempTable().itemsProperty().bind(new SimpleListProperty<>(tempList));
  }

  TableView<Log> getPersistentTable() {
    return persistentTable;
  }

  TableView<Log> getTempTable() {
    return tempTable;
  }

  /**
   * Free resources before closing the window containing this element.
   */
  public void dispose() {
    if (this.subscriptions != null) {
      this.subscriptions.unsubscribe();
    }
  }
}
