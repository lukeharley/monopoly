package com.luca.customscraper.service;

import com.luca.customscraper.util.StringNormalizer;
import java.util.Set;

public class SimilarityScorer {
  public double jaccardSimilarity(String left, String right) {
    Set<String> leftTokens = StringNormalizer.tokenize(left);
    Set<String> rightTokens = StringNormalizer.tokenize(right);
    if (leftTokens.isEmpty() && rightTokens.isEmpty()) {
      return 1.0;
    }
    if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
      return 0.0;
    }
    int intersection = 0;
    for (String token : leftTokens) {
      if (rightTokens.contains(token)) {
        intersection++;
      }
    }
    int union = leftTokens.size() + rightTokens.size() - intersection;
    if (union == 0) {
      return 0.0;
    }
    return (double) intersection / union;
  }

  public double weightedScore(
      double nameScore,
      double commercialScore,
      double producerScore,
      double formatScore
  ) {
    double nameWeight = 0.4;
    double commercialWeight = 0.3;
    double producerWeight = 0.2;
    double formatWeight = 0.1;
    return nameScore * nameWeight
        + commercialScore * commercialWeight
        + producerScore * producerWeight
        + formatScore * formatWeight;
  }
}
