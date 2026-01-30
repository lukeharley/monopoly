package com.luca.customscraper.service;

import com.luca.customscraper.model.ProductData;
import com.luca.customscraper.util.CsvLineParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductDataLoader {
  public List<ProductData> load(Path filePath) throws IOException {
    List<String> lines = Files.readAllLines(filePath);
    if (lines.isEmpty()) {
      return List.of();
    }
    List<String> headers = CsvLineParser.parseLine(lines.getFirst());
    List<ProductData> products = new ArrayList<>();
    for (int i = 1; i < lines.size(); i++) {
      List<String> values = CsvLineParser.parseLine(lines.get(i));
      Map<String, String> attributes = new HashMap<>();
      for (int index = 0; index < headers.size(); index++) {
        String header = headers.get(index);
        String value = index < values.size() ? values.get(index) : "";
        attributes.put(header, value);
      }
      products.add(new ProductData(
          attributes.getOrDefault("url", ""),
          attributes.getOrDefault("productName", ""),
          attributes.getOrDefault("commercialName", ""),
          attributes.getOrDefault("producer", ""),
          attributes.getOrDefault("format", ""),
          attributes
      ));
    }
    return products;
  }
}
