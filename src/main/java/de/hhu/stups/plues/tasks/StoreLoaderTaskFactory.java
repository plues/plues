package de.hhu.stups.plues.tasks;

public interface StoreLoaderTaskFactory {
  StoreLoaderTask create(final String storePath);
}
