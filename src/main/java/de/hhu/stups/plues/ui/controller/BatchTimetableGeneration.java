package de.hhu.stups.plues.ui.controller;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.ui.components.BatchResultBox;
import de.hhu.stups.plues.ui.components.BatchResultBoxFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class BatchTimetableGeneration extends GridPane implements Initializable {

  private final Logger logger = Logger.getLogger(getClass().getSimpleName());
  private final Delayed<Store> delayedStore;
  private final Delayed<SolverService> delayedSolverService;

  private final BooleanProperty solverProperty;
  private final BooleanProperty generationStarted;
  private final BooleanProperty generationSucceeded;
  private final BatchResultBoxFactory resultBoxFactory;
  private final ExecutorService executor;
  private Path tempDirectoryPath;
  private Set<PdfRenderingTask> taskPool;
  private Set<PdfRenderingTask> executableTaskPool;
  private Set<BatchResultBox> boxPool;
  private Task<Void> fillPoolTask;
  private Task<Void> executePoolTask;

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
  public BatchTimetableGeneration(final FXMLLoader loader, final Delayed<Store> delayedStore,
                                  final Delayed<SolverService> delayedSolverService,
                                  final BatchResultBoxFactory resultBoxFactory,
                                  final ExecutorService executorService) {
    this.delayedStore = delayedStore;
    this.delayedSolverService = delayedSolverService;
    this.resultBoxFactory = resultBoxFactory;
    this.executor = executorService;

    this.solverProperty = new SimpleBooleanProperty(false);
    this.generationStarted = new SimpleBooleanProperty(false);
    this.generationSucceeded = new SimpleBooleanProperty(false);
    this.taskPool = new HashSet<>();
    this.executableTaskPool = new HashSet<>();
    this.boxPool = new HashSet<>();

    try {
      this.tempDirectoryPath = Files.createTempDirectory("plues_timetables");
    } catch (final IOException exception) {
      logger.log(Level.INFO, "Could not create temporary directory.");
    }

    loader.setLocation(getClass().getResource("/fxml/BatchTimetableGeneration.fxml"));

    loader.setRoot(this);
    loader.setController(this);

    try {
      loader.load();
    } catch (final IOException exception) {
      throw new RuntimeException(exception);
    }
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    btGenerateAll.setDefaultButton(true);
    btGenerateAll.disableProperty().bind(solverProperty.not().or(generationStarted));

    btCancel.disableProperty().bind(solverProperty.not().or(btGenerateAll.disableProperty().not()));

    btSaveToZip.disableProperty().bind(generationSucceeded.not());
    btSaveToFolder.disableProperty().bind(generationSucceeded.not());

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
    listView.getItems().clear();
    generationStarted.setValue(true);
    generationSucceeded.setValue(false);
    final List<Course> courses = delayedStore.get().getCourses();

    final List<Course> majorCourseList = courses.stream()
        .filter(Course::isMajor)
        .collect(Collectors.toList());

    final List<Course> minorCourseList = courses.stream()
        .filter(Course::isMinor)
        .collect(Collectors.toList());

    fillPoolTask = new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        updateTitle("Preparing generation");
        majorCourseList.stream().forEach(c -> combineMajorMinor(c, minorCourseList));
        return null;
      }
    };

    // when the task pool is filled we start invoking all tasks
    fillPoolTask.setOnSucceeded(event -> {
      boxPool.forEach(b -> listView.getItems().add(b));
      executableTaskPool = new HashSet<>(taskPool);
      taskPool.clear();
      executor.submit(executePoolTask);
    });

    fillPoolTask.setOnCancelled(t -> {
      logger.log(Level.INFO, "Generation cancelled.");
      generationStarted.setValue(false);
      generationSucceeded.setValue(false);
      taskPool.clear();
      boxPool.clear();
    });

    executePoolTask = new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        updateTitle("Generating all timetables");
        final List<Future<?>> futurePool
            = executableTaskPool.stream().map(executor::submit).collect(Collectors.toList());

        boolean workLeft;
        final int totalTasks = futurePool.size();

        do {
          final long finishedTasks = futurePool.stream().filter(Future::isDone).count();
          updateProgress(finishedTasks, totalTasks);

          if (isCancelled()) {
            updateMessage("Cancelled");
            break;
          }
          // to check the interrupted exception for cancellation!
          try {
            TimeUnit.MILLISECONDS.sleep(100);
          } catch (final InterruptedException interrupted) {
            if (isCancelled()) {
              updateMessage("Cancelled");
              break;
            }
          }
          workLeft = !(totalTasks == finishedTasks);
        }
        while (workLeft);

        generationSucceeded.setValue(true);
        generationStarted.setValue(false);
        return null;
      }
    };

    executePoolTask.setOnCancelled(event -> {
      executableTaskPool.forEach(PdfRenderingTask::cancel);
      executableTaskPool.clear();
      boxPool.clear();
      generationStarted.setValue(false);
      generationSucceeded.setValue(false);
    });

    executor.submit(fillPoolTask);
  }

  /**
   * Generate all possible combinations of the given major course and all minor courses (one at a
   * time). If the major course is not combinable just generate a single pdf for this course.
   *
   * @param majorCourse     The currently given major course.
   * @param minorCourseList The list of all minor courses.
   */
  private void combineMajorMinor(Course majorCourse, List<Course> minorCourseList) {
    final String majorCourseName = majorCourse.getShortName();
    if (!majorCourse.isCombinable()) {
      boxPool.add(resultBoxFactory.create(majorCourse, null, tempDirectoryPath, taskPool));
    } else {
      minorCourseList.stream().forEach(c -> {
        if (!c.getShortName().equals(majorCourseName)) {
          boxPool.add(resultBoxFactory.create(majorCourse, c, tempDirectoryPath, taskPool));
        }
      });
    }
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
    boxPool = new HashSet<>();
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

    FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
        "zip-Archive (*.zip)", "*.zip");
    fileChooser.getExtensionFilters().add(extFilter);

    final File selectedFile = fileChooser.showSaveDialog(null);
    if (selectedFile != null) {
      tempFolderToZip(tempDirectoryPath, selectedFile.toPath());
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
    if (selectedDirectory != null) {
      try {
        Files.walk(tempDirectoryPath)
            .filter(path -> !path.toFile().isDirectory())
            .forEach(path -> {
              try {
                Files.copy(path, new File(selectedDirectory.toPath().toString() + "/"
                    + path.getFileName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
              } catch (IOException exception) {
                exception.printStackTrace();
              }
            });
      } catch (IOException exception) {
        logger.log(Level.INFO, "Could not save pdf files to the selected folder.");
      }
    }
  }

  /**
   * Zip all files located in the temporary folder and store the zip archive at the target path
   * given by the user.
   *
   * @param target The path to store the zip archive in.
   */
  private void tempFolderToZip(Path destination, Path target) {
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(target))) {
      Files.walk(destination)
          .filter(path -> !path.toFile().isDirectory())
          .forEach(path -> {
            try {
              zipOutputStream.putNextEntry(new ZipEntry(path.getFileName().toString()));
              zipOutputStream.write(Files.readAllBytes(path));
              zipOutputStream.closeEntry();
            } catch (IOException exception) {
              exception.printStackTrace();
            }
          });
    } catch (IOException exception) {
      logger.log(Level.INFO, "Could not save the zip archive to the selected location.");
    }
  }
}
