package de.hhu.stups.plues.tasks;

@FunctionalInterface
public interface StoreLoaderTaskFactory {
  StoreLoaderTask create(final String storePath);
}
