package de.hhu.stups.plues.tasks;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;


import de.be4.classicalb.core.parser.exceptions.BException;
import de.hhu.stups.plues.Helpers;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.modelgenerator.FileType;
import de.hhu.stups.plues.modelgenerator.Renderer;
import de.hhu.stups.plues.prob.Solver;
import de.hhu.stups.plues.ui.controller.MainController;
import de.prob.scripting.Api;
import groovy.lang.Writable;
import javafx.concurrent.Task;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class SolverLoaderTask extends Task<Solver> {
    public static final int MAX_STEPS = 4;
    private static final String MODEL_FILE = "Solver.mch";
    private static final String MODEL_PATH = "models";
    private static final String MODELS_ZIP = "models.zip";
    private static Path modelDirectory;
    private final Api api;
    private final StoreLoaderTask storeLoader;
    private Store store;
    private Solver solver;
    private Properties properties;


    @Inject
    public SolverLoaderTask(Api api, Properties properties, @Assisted StoreLoaderTask storeLoader) {
        this.api = api;
        this.storeLoader = storeLoader;
        this.properties = properties;
        updateTitle("Loading ProB"); // TODO i18n
    }

    private void prepareModels() throws Exception {
        String modelBase = (String) properties.get("modelpath");
        if (modelBase == null) {
            // use bundled files
            copyModelsToTemp();
        } else {
            Path p = Helpers.expandPath(modelBase);
            if (!new File(p.toString()).exists()) {
                throw new IllegalArgumentException("Path does not exist");
            }
            System.out.println("Using models from " + p);
            modelDirectory = p;
        }
    }


    private void copyModelsToTemp() throws Exception {
        Path tmpDirectory = Files.createTempDirectory("slottool");
        Path modelDirectory = tmpDirectory.resolve(MODEL_PATH);
        Files.createDirectory(modelDirectory);
        System.out.println("Exporting models to " + modelDirectory);

        //
        InputStream zipStream = MainController.class.getClassLoader().getResourceAsStream(MODELS_ZIP);
        if (zipStream == null) {
//            throw new AnomalousMaterialsException("Could not find models.zip resource!!");
            throw new Exception("Foo");
        }
        // copy zip-file to tmpDirectory
        Path zipPath = tmpDirectory.resolve(MODELS_ZIP);
        Files.copy(zipStream, zipPath);

        // read zip-file entries
        ZipFile zipFile = new ZipFile(zipPath.toFile());
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();

            if (name.equals("")) {
                System.out.println("Empty File");
                continue;
            }
            InputStream stream = zipFile.getInputStream(entry);
            Path modelPath = Paths.get(MODEL_PATH).resolve(name);
            System.out.println("Exporting " + modelPath);
            Files.copy(stream, tmpDirectory.resolve(modelPath));
        }
        zipFile.close();
        System.out.println("Done exporting model files.");
    }

    @Override
    protected Solver call() throws Exception {

        updateMessage("Prepare models"); // TODO i18n
        updateProgress(0, MAX_STEPS);

        prepareModels();
        updateProgress(1, MAX_STEPS);

        updateMessage("Waiting for Store"); // TODO i18n
        this.store = this.storeLoader.get();
        updateProgress(2, MAX_STEPS);

        updateMessage("Export data model (this can take a while)"); // TODO i18n
        exportDataModel();
        updateProgress(3, MAX_STEPS);

        updateMessage("Init solver (this can take a while)"); // TODO i18n
        initSolver();
        updateProgress(4, MAX_STEPS);

        return solver;
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        updateMessage("Done!"); // TODO i18n
    }

    private void initSolver() throws IOException, BException {
        String modelPath = modelDirectory.resolve(MODEL_FILE).toString();
        this.solver = new Solver(this.api, modelPath);
    }

    private void exportDataModel() throws IOException {
        Renderer renderer = new Renderer(this.store);

        String target = "data.mch";
        Writable modeldata = renderer.renderFor(FileType.BMachine);
        File targetFile = new File(modelDirectory.resolve(target).toString());

        String flavor = this.store.getInfoByKey("short-name");
        String targetxml = flavor + "-data.xml";
        Writable xmldata = renderer.renderFor(FileType.ModuleCombination);
        File targetXMLFile = new File(modelDirectory.resolve(targetxml).toString());


        OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(targetFile), "UTF-8");
        try {
            modeldata.writeTo(fw);
        } finally {
            fw.close();
        }
        System.out.println("Wrote model data to " + targetFile.getAbsolutePath());

        fw = new OutputStreamWriter(new FileOutputStream(targetXMLFile), "UTF-8");
        try {
            xmldata.writeTo(fw);
        } finally {
            fw.close();
        }

        System.out.println("Wrote module combination data to " + targetXMLFile.getAbsolutePath());
    }

}
