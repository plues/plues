package de.hhu.stups.plues.tasks;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.be4.classicalb.core.parser.exceptions.BException;
import de.hhu.stups.plues.Helpers;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.modelgenerator.FileType;
import de.hhu.stups.plues.modelgenerator.Renderer;
import de.hhu.stups.plues.prob.Solver;
import de.hhu.stups.plues.prob.SolverFactory;
import de.hhu.stups.plues.ui.controller.MainController;

import javafx.concurrent.Task;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class SolverLoaderTask extends Task<Solver> {
  private static final int MAX_STEPS = 5;
  private static final String MODEL_FILE = "Solver.mch";
  private static final String MODEL_PATH = "models";
  private static final String MODELS_ZIP = "models.zip";
  private final StoreLoaderTask storeLoader;
  private final Properties properties;
  private final SolverFactory solverFactory;
  private Path modelDirectory;
  private Store store;
  private Solver solver;

  private final Logger logger = Logger.getLogger(getClass().getSimpleName());


  /**
   * Create a new solver loader instance to setup and instantiate the requested solver type.
   * @param sf SolverFactory factory to create solver instances
   * @param pp Properties
   * @param storeLoaderTask tore loader task, task that setups and loads a data store to be used
   *                        for the solver instance
   */
  @Inject
  public SolverLoaderTask(
      final SolverFactory sf, final Properties pp,
      @Assisted final StoreLoaderTask storeLoaderTask) {

    this.solverFactory = sf;
    this.storeLoader = storeLoaderTask;
    this.properties = pp;
    this.properties.putIfAbsent("solver", "prob");
    this.updateTitle("Loading ProB"); // TODO i18n
    this.progressProperty().addListener((observable, oldValue, newValue)
        -> logger.fine(newValue.toString()));

    this.messageProperty().addListener((observable, oldValue, newValue) -> logger.fine(newValue));

  }

  private void prepareModels() throws Exception {
    final String modelBase = (String) this.properties.get("modelpath");
    if (modelBase == null) {
      // use bundled files
      this.copyModelsToTemp();
    } else {
      final Path p = Helpers.expandPath(modelBase);
      if (!new File(p.toString()).exists()) {
        logger.severe("Path does not exist");
        throw new IllegalArgumentException("Path does not exist");
      }

      logger.fine("Using models from " + p);
      this.modelDirectory = p;
    }
  }


  private void copyModelsToTemp() throws Exception {

    final Path tmpDirectory = Files.createTempDirectory("slottool");
    this.modelDirectory = tmpDirectory.resolve(MODEL_PATH);

    Files.createDirectory(this.modelDirectory);
    logger.info("Exporting models to " + this.modelDirectory);
    //
    final ClassLoader classLoader = MainController.class.getClassLoader();
    final InputStream zipStream = classLoader.getResourceAsStream(MODELS_ZIP);
    //
    if (zipStream == null) {
      throw new MissingResourceException(
        "Could not find models.zip resource!!",
        this.getClass().getName(),
        MODELS_ZIP);
    }
    // copy zip-file to tmpDirectory
    final Path zipPath = tmpDirectory.resolve(MODELS_ZIP);
    Files.copy(zipStream, zipPath);

    // read zip-file entries
    final ZipFile zipFile = new ZipFile(zipPath.toFile());
    final Enumeration<? extends ZipEntry> entries = zipFile.entries();

    while (entries.hasMoreElements()) {
      final ZipEntry entry = entries.nextElement();
      final String name = entry.getName();

      if (name.equals("")) {
        logger.fine("Empty File");
        continue;
      }

      final InputStream stream = zipFile.getInputStream(entry);
      final Path modelPath = Paths.get(MODEL_PATH).resolve(name);

      logger.fine("Exporting " + modelPath);
      Files.copy(stream, tmpDirectory.resolve(modelPath));
    }
    zipFile.close();
    logger.fine("Done exporting model files.");
  }

  @Override
  protected final Solver call() throws Exception {
    this.updateMessage("Prepare models"); // TODO i18n
    this.updateProgress(0, MAX_STEPS);
    //
    this.prepareModels();
    this.updateProgress(1, MAX_STEPS);
    //
    this.updateMessage("Waiting for Store"); // TODO i18n
    this.store = this.storeLoader.get();
    this.updateProgress(2, MAX_STEPS);
    //
    this.updateMessage("Export data model (this can take a while)"); // TODO i18n
    this.exportDataModel();
    this.updateProgress(3, MAX_STEPS);
    //
    this.updateMessage("Init solver (this can take a while)"); // TODO i18n

    final long start = System.nanoTime();
    this.initSolver();
    final long end = System.nanoTime();

    this.updateProgress(4, MAX_STEPS);
    logger.info("Loaded solver in " + TimeUnit.NANOSECONDS.toMillis(end - start) + " ms");
    //
    this.updateMessage("Checking model version"); // TODO i18n
    this.solver.checkModelVersion((String) this.properties.get("model_version"));
    this.updateProgress(5, MAX_STEPS);
    //
    return this.solver;
  }

  @Override
  protected final void succeeded() {
    super.succeeded();
    this.updateMessage("Done!"); // TODO i18n
    logger.log(Level.FINE, "loading Solver succeeded");
  }

  @Override
  protected final void cancelled() {
    this.store.close();
    logger.warning("Loading solver cancelled");
  }

  @Override
  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  protected final void failed() {
    this.store.close();
    logger.severe("Loading solver failed");
    this.getException().printStackTrace();
  }

  private void initSolver() throws IOException, BException,
      NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    final String modelPath = this.modelDirectory.resolve(MODEL_FILE).toString();

    final String solverName = (String) this.properties.get("solver");
    //
    logger.info("Using " + solverName + " solver");
    //
    final String solver = solverName.substring(0, 1).toUpperCase() + solverName.substring(1);
    //
    try {
      final Method method = this.solverFactory.getClass()
          .getMethod("create" + solver + "Solver", String.class);

      this.solver = (Solver) method.invoke(this.solverFactory, modelPath);

    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
      exception.printStackTrace();
      throw exception;
    }
  }

  private void exportDataModel() throws IOException {
    final Renderer renderer = new Renderer(this.store);

    final File targetFile = this.modelDirectory.resolve("data.mch").toFile();

    renderer.renderFor(FileType.BMachine, targetFile);
    //
    final String flavor = this.store.getInfoByKey("short-name");

    final File targetXmlFile = this.modelDirectory.resolve(flavor + "-data.xml").toFile();

    renderer.renderFor(FileType.ModuleCombination, targetXmlFile);


    logger.info("Wrote model data to " + targetFile.getAbsolutePath());
    logger.info("Wrote module combination data to " + targetXmlFile.getAbsolutePath());

    this.store.clear();
  }

}
