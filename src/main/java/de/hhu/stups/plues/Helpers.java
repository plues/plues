package de.hhu.stups.plues;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public final class Helpers {
  private Helpers() {
  }

  /**
   * Expand a path (gives as string) to an absolute path. If the path starts with ~ it is replaced
   * with the current user's home directory.
   * @param base String
   * @return Path absulute path representation of the argument
   */
  public static Path expandPath(String base) {
    // handle ~ in paths
    if (base.startsWith("~" + File.separator)) {
      base = System.getProperty("user.home") + base.substring(1);
    }
    return FileSystems.getDefault().getPath(base).toAbsolutePath();
  }
}
