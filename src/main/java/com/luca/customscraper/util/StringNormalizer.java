package com.luca.customscraper.util;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class StringNormalizer {
  private StringNormalizer() {}

  public static String normalize(String value) {
    if (value == null) {
      return "";
    }
    String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT);
    return normalized.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", " ").trim();
  }

  public static Set<String> tokenize(String value) {
    String normalized = normalize(value);
    if (normalized.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(normalized.split("\\s+"))
        .filter(token -> !token.isBlank())
        .collect(Collectors.toSet());
  }
}
