package com.luca.customscraper.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SimilarityScorerTest {
  @Test
  void calculatesJaccardSimilarity() {
    SimilarityScorer scorer = new SimilarityScorer();
    double score = scorer.jaccardSimilarity("Latte Parzialmente Scremato 1L", "Latte Parzialmente");
    assertTrue(score > 0.0);
    assertTrue(score < 1.0);
  }

  @Test
  void weightedScoreUsesAllWeights() {
    SimilarityScorer scorer = new SimilarityScorer();
    double score = scorer.weightedScore(1.0, 0.0, 0.0, 0.0);
    assertEquals(0.4, score, 0.0001);
  }
}
