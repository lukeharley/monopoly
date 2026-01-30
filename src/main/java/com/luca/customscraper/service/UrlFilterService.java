package com.luca.customscraper.service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class UrlFilterService {
  public Set<String> filterNewUrls(Collection<String> scrapedUrls, Collection<String> existingUrls) {
    Objects.requireNonNull(scrapedUrls, "scrapedUrls is required");
    Set<String> known = existingUrls == null ? Set.of() : new LinkedHashSet<>(existingUrls);
    Set<String> result = new LinkedHashSet<>();
    for (String url : scrapedUrls) {
      if (url != null && !url.isBlank() && !known.contains(url)) {
        result.add(url);
      }
    }
    return result;
  }
}
