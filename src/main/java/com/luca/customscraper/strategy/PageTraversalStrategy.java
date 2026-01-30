package com.luca.customscraper.strategy;

public interface PageTraversalStrategy {
  boolean hasNext();

  String nextPageUrl();
}
