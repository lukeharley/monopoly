package com.luca.customscraper.strategy;

import java.util.Objects;

public class PaginationStrategy implements PageTraversalStrategy {
  private final String baseUrl;
  private final String pageParameter;
  private final int maxPages;
  private int currentPage;

  public PaginationStrategy(String baseUrl, String pageParameter, int startPage, int maxPages) {
    this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl is required");
    this.pageParameter = Objects.requireNonNull(pageParameter, "pageParameter is required");
    this.currentPage = startPage;
    this.maxPages = maxPages;
  }

  @Override
  public boolean hasNext() {
    return currentPage <= maxPages;
  }

  @Override
  public String nextPageUrl() {
    if (!hasNext()) {
      return null;
    }
    String url = String.format("%s?%s=%d", baseUrl, pageParameter, currentPage);
    currentPage += 1;
    return url;
  }
}
