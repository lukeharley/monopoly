package com.luca.customscraper.util;

import java.util.ArrayList;
import java.util.List;

public final class CsvLineParser {
  private CsvLineParser() {}

  public static List<String> parseLine(String line) {
    List<String> values = new ArrayList<>();
    if (line == null) {
      return values;
    }
    StringBuilder current = new StringBuilder();
    boolean inQuotes = false;
    for (int i = 0; i < line.length(); i++) {
      char ch = line.charAt(i);
      if (ch == '"') {
        if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
          current.append('"');
          i++;
        } else {
          inQuotes = !inQuotes;
        }
        continue;
      }
      if (ch == ',' && !inQuotes) {
        values.add(current.toString().trim());
        current.setLength(0);
      } else {
        current.append(ch);
      }
    }
    values.add(current.toString().trim());
    return values;
  }
}
