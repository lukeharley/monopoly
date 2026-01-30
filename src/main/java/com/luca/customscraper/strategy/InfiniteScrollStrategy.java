package com.luca.customscraper.strategy;

import java.util.Objects;
import java.util.function.Function;

public class InfiniteScrollStrategy implements PageTraversalStrategy {
  private final Function<Integer, String> scrollUrlBuilder;
  private final int maxScrolls;
  private int currentScroll;

  public InfiniteScrollStrategy(Function<Integer, String> scrollUrlBuilder, int maxScrolls) {
    this.scrollUrlBuilder = Objects.requireNonNull(scrollUrlBuilder, "scrollUrlBuilder is required");
    this.maxScrolls = maxScrolls;
    this.currentScroll = 0;
  }

  @Override
  public boolean hasNext() {
    return currentScroll < maxScrolls;
  }

  @Override
  public String nextPageUrl() {
    if (!hasNext()) {
      return null;
    }
    currentScroll += 1;
    return scrollUrlBuilder.apply(currentScroll);
  }
}
