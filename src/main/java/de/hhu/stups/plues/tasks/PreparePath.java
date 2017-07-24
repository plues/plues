package de.hhu.stups.plues.tasks;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public interface PreparePath {

  /**
   * Expand a path (gives as string) to an absolute path. If the path starts with ~ it is replaced
   * with the current user's home directory.
   *
   * @param base String
   * @return Path absolute path representation of the argument
   */
  static Path expandPath(final String base) {
    // handle ~ in paths
    final String basePath;
    if (base.startsWith("~" + File.separator)) {
      basePath = System.getProperty("user.home") + base.substring(1);
    } else {
      basePath = base;
    }
    return FileSystems.getDefault().getPath(basePath).toAbsolutePath();
  }
}
