package de.hhu.stups.plues.tasks;

public interface SolverLoaderTaskFactory {
    SolverLoaderTask create(StoreLoaderTask storeLoaderTask);
}
