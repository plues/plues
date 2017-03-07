package de.hhu.stups.plues.routes;

@FunctionalInterface
public interface HandbookRouteFactory {

  HandbookRoute create(HandbookRoute.Format format);
}
