package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.hhu.stups.plues.ui.batchgeneration.BatchPdfRenderingTask;
import de.hhu.stups.plues.ui.batchgeneration.CollectPdfRenderingTasksTask;
import de.hhu.stups.plues.ui.components.BatchResultBox;
import de.hhu.stups.plues.ui.components.BatchResultBoxFactory;
import de.hhu.stups.plues.ui.components.ColorSchemeSelection;
import de.hhu.stups.plues.ui.components.ControllerHeader;
import de.hhu.stups.plues.ui.components.PdfGenerationSettings;
import de.hhu.stups.plues.ui.components.UnitDisplayFormatSelection;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import org.jtwig.JtwigModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Generate all possible combinations of major and minor courses and show the results in a {@link
 * #listView}. During generation the pdf files are stored in a temporary directory. When all tasks
 * are finished the user is able to store the pdf files persistently in a {@link
 * #savePersistentFolder folder} or a {@link #savePersistentZip zip archive}.
 */
public class BatchTimetableGeneration extends GridPane implements Initializable {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final Delayed<SolverService> delayedSolverService;

  private final BooleanProperty solverProperty;
  private final BooleanProperty generationRunning;
  private final SimpleListProperty<PdfRenderingTask> generationSucceeded;
  private final ObjectProperty<PdfGenerationSettings> pdfGenerationSettingsProperty;

  private final BatchResultBoxFactory batchResultBoxFactory;
  private final ExecutorService executor;

  private final Provider<CollectPdfRenderingTasksTask> collectPdfRenderingTasksTaskProvider;

  private CollectPdfRenderingTasksTask fillPoolTask;
  private BatchPdfRenderingTask executePoolTask;

  @FXML
  @SuppressWarnings("unused")
  private ControllerHeader controllerHeader;
  @FXML
  @SuppressWarnings("unused")
  private Button btGenerateAll;
  @FXML
  @SuppressWarnings("unused")
  private Button btSaveToZip;
  @FXML
  @SuppressWarnings("unused")
  private Button btSaveToFolder;
  @FXML
  @SuppressWarnings("unused")
  private Button btCancel;
  @FXML
  @SuppressWarnings("unused")
  private Button btPrint;
  @FXML
  @SuppressWarnings("unused")
  private ListView<BatchResultBox> listView;
  @FXML
  @SuppressWarnings("unused")
  private ColorSchemeSelection colorSchemeSelection;
  @FXML
  @SuppressWarnings("unused")
  private UnitDisplayFormatSelection unitDisplayFormatSelection;

  /**
   * Constructor.
   */
  @Inject
  public BatchTimetableGeneration(final Inflater inflater,
                                  final Delayed<SolverService> delayedSolverService,
                                  final BatchResultBoxFactory batchResultBoxFactory,
                                  final Provider<CollectPdfRenderingTasksTask> taskProvider,
                                  final ExecutorService executorService) {

    this.collectPdfRenderingTasksTaskProvider = taskProvider;
    this.delayedSolverService = delayedSolverService;

    this.batchResultBoxFactory = batchResultBoxFactory;

    this.executor = executorService;

    this.solverProperty = new SimpleBooleanProperty(false);
    this.generationRunning = new SimpleBooleanProperty(false);
    this.generationSucceeded = new SimpleListProperty<>();
    pdfGenerationSettingsProperty = new SimpleObjectProperty<>(
      new PdfGenerationSettings(null, null));

    inflater.inflate("BatchTimetableGeneration", this, this, "batchTimetable");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    pdfGenerationSettingsProperty.get().colorSchemeProperty()
      .bind(colorSchemeSelection.selectedColorScheme());
    pdfGenerationSettingsProperty.get().unitDisplayFormatProperty()
      .bind(unitDisplayFormatSelection.selectedDisplayFormatProperty());

    listView.setOnMouseClicked(event -> {
      if (event.getClickCount() == 2) {
        final BatchResultBox batchResultBox = listView.getSelectionModel().getSelectedItem();
        batchResultBox.showPdf();
      }
    });

    colorSchemeSelection.defaultInitialization();
    colorSchemeSelection.setPercentWidth(50.0);
    unitDisplayFormatSelection.setPercentWidth(50.0);

    btGenerateAll.disableProperty().bind(solverProperty.not().or(generationRunning));
    colorSchemeSelection.disableProperty().bind(solverProperty.not().or(generationRunning));
    btCancel.disableProperty().bind(
      solverProperty.not().or(btGenerateAll.disabledProperty().not()));

    btSaveToZip.disableProperty().bind(generationSucceeded.emptyProperty());
    btSaveToFolder.disableProperty().bind(generationSucceeded.emptyProperty());
    btPrint.disableProperty().bind(generationSucceeded.emptyProperty());

    delayedSolverService.whenAvailable(s -> this.solverProperty.set(true));
    initializeControllerHeader(resources);
  }

  private void initializeControllerHeader(final ResourceBundle resources) {
    controllerHeader.setTitle(resources.getString("title"));
    controllerHeader.setInfoText(resources.getString("info"));
  }

  /**
   * The button action of {@link #btGenerateAll} to generate pdf files for all possible combinations
   * of major and minor courses.
   */
  @FXML
  @SuppressWarnings("unused")
  private void generateAll() {
    generationRunning.setValue(true);
    //
    generationSucceeded.clear();
    listView.getItems().clear();

    fillPoolTask = collectPdfRenderingTasksTaskProvider.get();
    fillPoolTask.pdfGenerationSettingsProperty().set(pdfGenerationSettingsProperty.get());

    fillPoolTask.setOnSucceeded(event -> {
      fillPoolTask.getValue().forEach(task -> {
        final BatchResultBox b = batchResultBoxFactory.create(task);
        listView.getItems().add(b);
      });
      executePoolTask = buildBatchPdfRenderingTask(fillPoolTask.getValue());
      executePoolTask.setOnRunning(executePoolEvent -> {
        if (!generationRunning.get()) {
          executePoolTask.cancel(true);
        }
      });
      executor.submit(executePoolTask);
    });

    fillPoolTask.setOnCancelled(event -> {
      generationRunning.setValue(false);
      generationSucceeded.clear();
    });

    fillPoolTask.setOnFailed(event -> {
      generationRunning.setValue(false);
      generationSucceeded.clear();
    });
    executor.submit(fillPoolTask);
  }

  @SuppressWarnings("unused")
  private BatchPdfRenderingTask buildBatchPdfRenderingTask(final Set<PdfRenderingTask> tasks) {
    final BatchPdfRenderingTask renderingTask = new BatchPdfRenderingTask(executor, tasks);

    renderingTask.setOnSucceeded(event -> {
      final Collection<PdfRenderingTask> executedTasks = executePoolTask.getValue();
      final List<PdfRenderingTask> result = getSuccessfulTasks(executedTasks);

      generationSucceeded.set(FXCollections.observableList(result));
      generationRunning.setValue(false);
    });

    renderingTask.setOnCancelled(event -> {
      logger.info("PDF generation task cancelled.");
      generationRunning.setValue(false);
      generationSucceeded.clear();
    });

    renderingTask.setOnFailed(event -> {
      logger.info("PDF generation task failed.");
      generationRunning.setValue(false);
      generationSucceeded.clear();
    });

    return renderingTask;
  }

  private List<PdfRenderingTask> getSuccessfulTasks(final Collection<PdfRenderingTask> tasks) {
    return tasks.stream()
      .filter(pdfRenderingTask -> pdfRenderingTask.getState() == Worker.State.SUCCEEDED)
      .collect(Collectors.toList());
  }

  /**
   * The button action of {@link #btCancel} to cancel the generation of all timetables.
   */
  @FXML
  @SuppressWarnings("unused")
  private void cancelGeneration() {
    if (fillPoolTask.isRunning()) {
      fillPoolTask.cancel(true);
    }
    if (executePoolTask.isRunning()) {
      executePoolTask.cancel(true);
    }
    generationRunning.setValue(false);
  }

  /**
   * The button action of {@link #btSaveToZip} to save the temporarily stored pdf files to a zip
   * archive selected by the user.
   */
  @FXML
  @SuppressWarnings("unused")
  private void savePersistentZip() {
    final FileChooser fileChooser = new FileChooser();
    fileChooser.setInitialFileName("plues_all_timetables.zip");
    fileChooser.setTitle("Choose the zip file's location");

    final FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
      "zip-Archive (*.zip)", "*.zip");
    fileChooser.getExtensionFilters().add(extFilter);

    final File selectedFile = fileChooser.showSaveDialog(null);
    if (selectedFile != null) {
      tempFilesToZip(selectedFile.toPath());
    }
  }

  /**
   * The button action of {@link #btSaveToFolder} to save the temporarily stored pdf files to a
   * folder selected by the user.
   */
  @FXML
  @SuppressWarnings("unused")
  private void savePersistentFolder() {
    final DirectoryChooser directoryChooser = new DirectoryChooser();
    directoryChooser.setTitle("Choose the directory");

    final File selectedDirectory = directoryChooser.showDialog(null);
    if (selectedDirectory == null) {
      return;
    }
    final Path dir = selectedDirectory.toPath();

    this.generationSucceeded.forEach(task -> {
      final String fileName = PdfRenderingHelper.getDocumentName(task.getMajor(), task.getMinor());
      final Path source = task.getValue();

      final Path target = Paths.get(dir.toString(), fileName);

      try {
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
      } catch (final IOException exception) {
        logger.error("Could not save pdf file to the selected folder.", exception);
      }
    });
  }

  /**
   * Export the results from the batch timetable generation to a printable .pdf file.
   */
  @FXML
  @SuppressWarnings("unused")
  public void printBatchResults() {
    PdfRenderingHelper.writeJtwigTemplateToPdfFile(getJtwigModel(),
      "/batchgeneration/templates/BatchGenerationTemplate.twig", "all_courses_checked");
  }

  private JtwigModel getJtwigModel() {
    final URL logo = getClass().getResource("/images/HHU_Logo.jpeg");

    final LocalDate date = LocalDate.now();
    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    final String formattedDate = date.format(formatter);

    return JtwigModel.newModel()
      .with("date", formattedDate)
      .with("batchResultBoxes", listView.getItems())
      .with("logo", logo);
  }

  /**
   * Zip all files generated by the rendering tasks and store the zip archive at the target path
   * given by the user.
   *
   * @param target The path to store the zip archive in.
   */
  private void tempFilesToZip(final Path target) {
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(target))) {
      this.generationSucceeded.forEach(task -> addEntryToZip(zipOutputStream, task));
    } catch (final IOException exception) {
      logger.error("Could not save the zip archive to the selected location.",
        exception);
    }
  }

  private void addEntryToZip(final ZipOutputStream zipOutputStream, final PdfRenderingTask task) {
    final String fileName = PdfRenderingHelper.getDocumentName(
      task.getMajor(), task.getMinor());

    final Path source = task.getValue();

    try {
      zipOutputStream.putNextEntry(new ZipEntry(fileName));
      zipOutputStream.write(Files.readAllBytes(source));
      zipOutputStream.closeEntry();
    } catch (final IOException exception) {
      logger.error("Could not add file to zip archive", exception);
    }
  }
}
