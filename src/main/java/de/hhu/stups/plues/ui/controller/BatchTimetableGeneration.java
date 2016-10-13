package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.ui.batchgeneration.BatchPdfRenderingTask;
import de.hhu.stups.plues.ui.batchgeneration.CollectPdfRenderingTasksTask;
import de.hhu.stups.plues.ui.components.BatchResultBox;
import de.hhu.stups.plues.ui.components.BatchResultBoxFactory;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class BatchTimetableGeneration extends GridPane implements Initializable {

  private final Logger logger = Logger.getLogger(getClass().getSimpleName());

  private final Delayed<SolverService> delayedSolverService;

  private final BooleanProperty solverProperty;
  private final BooleanProperty generationStarted;
  private final SimpleListProperty<PdfRenderingTask> generationSucceeded;

  private final BatchResultBoxFactory batchResultBoxFactory;
  private final ExecutorService executor;

  private final Set<PdfRenderingTask> pdfRenderingTasks;
  private final Provider<CollectPdfRenderingTasksTask> provider;

  private Task<Set<PdfRenderingTask>> fillPoolTask;
  private BatchPdfRenderingTask executePoolTask;

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
  private ListView<BatchResultBox> listView;

  /**
   * Generate all possible combinations of major and minor courses. While generating the pdf files
   * save them in a temporary directory. When all tasks are finished the user is able to store the
   * pdf files persistently in a folder or a zip archive.
   */
  @Inject
  public BatchTimetableGeneration(final Inflater inflater,
                                  final Delayed<SolverService> delayedSolverService,
                                  final BatchResultBoxFactory batchResultBoxFactory,
                                  final Provider<CollectPdfRenderingTasksTask> provider,
                                  final ExecutorService executorService) {

    this.provider = provider;
    this.delayedSolverService = delayedSolverService;

    this.batchResultBoxFactory = batchResultBoxFactory;

    this.executor = executorService;
    this.pdfRenderingTasks = new HashSet<>();

    this.solverProperty = new SimpleBooleanProperty(false);
    this.generationStarted = new SimpleBooleanProperty(false);
    this.generationSucceeded = new SimpleListProperty<>();

    inflater.inflate("BatchTimetableGeneration", this, this, "batchTimetable");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    btGenerateAll.setDefaultButton(true);
    btGenerateAll.disableProperty().bind(solverProperty.not().or(generationStarted));
    btCancel.disableProperty().bind(
        solverProperty.not().or(btGenerateAll.disabledProperty().not()));

    btSaveToZip.disableProperty().bind(generationSucceeded.emptyProperty());
    btSaveToFolder.disableProperty().bind(generationSucceeded.emptyProperty());

    listView.visibleProperty().bind(Bindings.size(listView.getItems()).greaterThan(0));
    listView.setId("batchListView");

    delayedSolverService.whenAvailable(s -> this.solverProperty.set(true));
  }

  /**
   * The button action of {@link #btGenerateAll} to generate pdf files for all possible combinations
   * of major and minor courses.
   */
  @FXML
  @SuppressWarnings("unused")
  private void generateAll() {
    generationStarted.setValue(true);
    //
    generationSucceeded.clear();
    listView.getItems().clear();

    fillPoolTask = provider.get();
    fillPoolTask.setOnSucceeded(event -> {
      fillPoolTask.getValue().forEach(task -> {
        final BatchResultBox b = batchResultBoxFactory.create(task);
        listView.getItems().add(b);
      });
      pdfRenderingTasks.clear();
      pdfRenderingTasks.addAll(fillPoolTask.getValue());
      executor.submit(executePoolTask);
    });

    fillPoolTask.setOnCancelled(event -> {
      generationStarted.setValue(false);
      generationSucceeded.clear();
    });

    fillPoolTask.setOnFailed(event -> {
      generationStarted.setValue(false);
      generationSucceeded.clear();
    });


    executePoolTask = new BatchPdfRenderingTask(executor, pdfRenderingTasks);

    executePoolTask.setOnSucceeded(event -> {
      final Collection<PdfRenderingTask> tasks = executePoolTask.getValue();
      final List<PdfRenderingTask> result = getSuccessfulTasks(tasks);

      generationSucceeded.set(FXCollections.observableList(result));
      generationStarted.setValue(false);
    });

    executePoolTask.setOnCancelled(event -> {
      logger.info("PDF generation task cancelled.");
      generationStarted.setValue(false);
      generationSucceeded.clear();
    });

    executePoolTask.setOnFailed(event -> {
      logger.info("PDF generation task failed.");
      generationStarted.setValue(false);
      generationSucceeded.clear();
    });

    executor.submit(fillPoolTask);
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
    generationStarted.setValue(false);
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
        logger.log(Level.SEVERE, "Could not save pdf file to the selected folder.", exception);
      }
    });
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
      logger.log(Level.SEVERE, "Could not save the zip archive to the selected location.",
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
      logger.log(Level.SEVERE, "Could not add file to zip archive", exception);
    }
  }
}
