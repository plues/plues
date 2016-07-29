package de.hhu.stups.plues;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class Helpers {
    public static Path expandPath(String base) {
        // handle ~ in paths
        if (base.startsWith("~" + File.separator)) {
            base = System.getProperty("user.home") + base.substring(1);
        }
        return FileSystems.getDefault().getPath(base).toAbsolutePath();
    }
}
