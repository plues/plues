package de.hhu.stups.plues.tasks;

import java.util.Properties;

public interface StoreLoaderTaskFactory {
  StoreLoaderTask create(final String storePath, final Properties properties);
}
