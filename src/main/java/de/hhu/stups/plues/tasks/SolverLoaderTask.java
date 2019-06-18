package de.hhu.stups.plues.tasks;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.modelgenerator.FileType;
import de.hhu.stups.plues.modelgenerator.Renderer;
import de.hhu.stups.plues.prob.ProBSolver;
import de.hhu.stups.plues.prob.Solver;
import de.hhu.stups.plues.prob.SolverException;
import de.hhu.stups.plues.prob.SolverFactory;
import de.hhu.stups.plues.ui.controller.MainController;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javafx.concurrent.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class SolverLoaderTask extends Task<Solver> {
  private static final int MAX_STEPS = 4;
  private static final String MODEL_FILE = "Solver.mch";
  private static final String MODEL_PATH = "models";
  private static final String MODELS_ZIP = "models.zip";
  private final Properties properties;
  private final SolverFactory solverFactory;
  private final Store store;
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final ResourceBundle resources;
  private Path modelDirectory;


  /**
   * Create a new solver loader instance to setup and instantiate the requested solver type.
   *
   * @param sf    SolverFactory factory to create solver instances
   * @param pp    Properties
   * @param store tore loader task, task that setups and loads a data store to be used for the
   *              solver instance
   */
  @Inject
  public SolverLoaderTask(final SolverFactory sf,
                          final Properties pp, @Assisted final Store store) {

    this.resources = ResourceBundle.getBundle("lang.tasks");
    this.solverFactory = sf;
    this.store = store;
    this.properties = pp;
    this.properties.putIfAbsent("solver", "prob");
    this.updateTitle(resources.getString("solverTitle"));
    this.progressProperty().addListener((observable, oldValue, newValue)
        -> logger.trace(newValue.toString()));

    this.messageProperty().addListener((observable, oldValue, newValue) -> logger.trace(newValue));

  }

  private void prepareModels() throws IOException {
    final String modelBase = (String) this.properties.get("modelpath");
    if (modelBase == null) {
      // use bundled files
      this.copyModelsToTemp();
    } else {
      final Path p = PreparePath.expandPath(modelBase);
      if (!new File(p.toString()).exists()) {
        logger.error("Path does not exist");
        throw new IllegalArgumentException("Path does not exist");
      }

      logger.info("Using models from {}", p);
      this.modelDirectory = p;
    }
  }


  private void copyModelsToTemp() throws IOException {

    final Path tmpDirectory = Files.createTempDirectory("slottool");
    tmpDirectory.toFile().deleteOnExit();

    this.modelDirectory = tmpDirectory.resolve(MODEL_PATH);

    Files.createDirectory(this.modelDirectory);
    logger.info("Exporting models to {}", this.modelDirectory);
    //
    final Path zipPath = moveModelsArchive(tmpDirectory);
    //
    // read zip-file entries
    try (final ZipFile zipFile = new ZipFile(zipPath.toFile())) {
      final Enumeration<? extends ZipEntry> entries = zipFile.entries();

      while (entries.hasMoreElements()) {
        final ZipEntry entry = entries.nextElement();
        final String name = entry.getName();

        if ("".equals(name)) {
          logger.trace("Empty File");
          continue;
        }

        final InputStream stream = zipFile.getInputStream(entry);
        final Path modelPath = Paths.get(MODEL_PATH).resolve(name);

        logger.info("Exporting {}", modelPath);
        Files.copy(stream, tmpDirectory.resolve(modelPath));
      }
    }
    logger.trace("Done exporting model files.");
  }

  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
  private Path moveModelsArchive(final Path tmpDirectory) throws IOException {
    final ClassLoader classLoader = MainController.class.getClassLoader();
    try (final InputStream zipStream = classLoader.getResourceAsStream(MODELS_ZIP)) {
      //
      if (zipStream == null) {
        throw new MissingResourceException("Could not find models.zip resource!!",
            this.getClass().getName(), MODELS_ZIP);
      }
      // copy zip-file to tmpDirectory
      final Path zipPath = tmpDirectory.resolve(MODELS_ZIP);
      Files.copy(zipStream, zipPath);
      return zipPath;
    }
  }

  @Override
  protected final Solver call() throws Exception {
    final String solverName = (String) this.properties.get("solver");
    //
    logger.info("Using {} solver", solverName);
    //
    if ("mock".equals(solverName)) {
      return this.startMockSolver();
    }
    return this.startProbSolver();
  }

  @SuppressWarnings("unused")
  private Solver startMockSolver() {
    this.updateProgress(1, 1);
    this.updateMessage(resources.getString("startMockSolver"));
    return solverFactory.createMockSolver();
  }

  @SuppressWarnings("unused")
  private Solver startProbSolver() throws IOException, SolverException {
    ///
    this.updateMessage(resources.getString("prepareModels"));
    this.updateProgress(0, MAX_STEPS);
    //
    this.prepareModels();
    this.updateProgress(1, MAX_STEPS);
    //
    this.updateMessage(resources.getString("exportData"));
    this.exportDataModel();
    this.updateProgress(2, MAX_STEPS);
    //
    this.updateMessage(resources.getString("initSolver"));

    final long start = System.nanoTime();
    final Solver solver = this.initSolver();
    final long end = System.nanoTime();

    this.updateProgress(3, MAX_STEPS);
    logger.info("Loaded solver in {} ms.", TimeUnit.NANOSECONDS.toMillis(end - start));
    //
    this.updateMessage(resources.getString("modelVersion"));
    solver.checkModelVersion((String) this.properties.get("model_version"));
    this.updateProgress(4, MAX_STEPS);
    //
    return solver;
  }

  @Override
  protected final void succeeded() {
    super.succeeded();
    this.updateMessage(resources.getString("finished"));
    logger.info("loading Solver succeeded");
  }

  @Override
  protected final void cancelled() {
    this.store.close();
    logger.warn("Loading solver cancelled");
  }

  @Override
  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  protected final void failed() {
    this.store.close();
    logger.error("Loading solver failed", this.getException());
  }

  private ProBSolver initSolver() throws SolverException {
    final String modelPath = this.modelDirectory.resolve(MODEL_FILE).toString();
    return this.solverFactory.createProbSolver(modelPath);
  }

  private void exportDataModel() throws IOException {
    final Renderer renderer = new Renderer(this.store);

    final File targetFile = this.modelDirectory.resolve("data.mch").toFile();

    renderer.renderFor(FileType.B_MACHINE, targetFile);
    //
    final String flavor = this.store.getInfoByKey("short-name");

    final File targetXmlFile = this.modelDirectory.resolve(flavor + "-data.xml").toFile();

    renderer.renderFor(FileType.MODULE_COMBINATION, targetXmlFile);


    logger.info("Wrote model data to " + targetFile.getAbsolutePath());
    logger.info("Wrote module combination data to " + targetXmlFile.getAbsolutePath());

    this.store.clear();
  }
}
