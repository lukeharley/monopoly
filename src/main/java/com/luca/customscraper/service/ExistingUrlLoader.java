package com.luca.customscraper.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class ExistingUrlLoader {
  public List<String> load(Path filePath) throws IOException {
    try (var lines = Files.lines(filePath)) {
      return lines.map(String::trim)
          .filter(line -> !line.isBlank())
          .collect(Collectors.toList());
    }
  }
}
