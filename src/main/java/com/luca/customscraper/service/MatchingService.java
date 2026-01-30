package com.luca.customscraper.service;

import com.luca.customscraper.model.MatchCandidate;
import com.luca.customscraper.model.MatchingResult;
import com.luca.customscraper.model.ProductData;
import com.luca.customscraper.model.ReferenceProduct;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MatchingService {
  private final SimilarityScorer similarityScorer;

  public MatchingService(SimilarityScorer similarityScorer) {
    this.similarityScorer = Objects.requireNonNull(similarityScorer, "similarityScorer is required");
  }

  public List<MatchingResult> matchProducts(List<ProductData> products, List<ReferenceProduct> references) {
    Objects.requireNonNull(products, "products is required");
    Objects.requireNonNull(references, "references is required");
    List<MatchingResult> results = new ArrayList<>();
    for (ProductData product : products) {
      List<MatchCandidate> candidates = new ArrayList<>();
      for (ReferenceProduct reference : references) {
        Map<String, Double> scores = attributeScores(product, reference);
        double score = similarityScorer.weightedScore(
            scores.getOrDefault("name", 0.0),
            scores.getOrDefault("commercial", 0.0),
            scores.getOrDefault("producer", 0.0),
            scores.getOrDefault("format", 0.0)
        );
        candidates.add(new MatchCandidate(reference, score, scores));
      }
      candidates.sort(Comparator.comparingDouble(MatchCandidate::score).reversed());
      results.add(new MatchingResult(product, candidates));
    }
    return results;
  }

  private Map<String, Double> attributeScores(ProductData product, ReferenceProduct reference) {
    Map<String, Double> scores = new HashMap<>();
    scores.put("name", similarityScorer.jaccardSimilarity(product.productName(), reference.denomination()));
    scores.put("commercial", similarityScorer.jaccardSimilarity(product.commercialName(), reference.commercialName()));
    scores.put("producer", similarityScorer.jaccardSimilarity(product.producer(), reference.producer()));
    scores.put("format", similarityScorer.jaccardSimilarity(product.format(), reference.format()));
    return scores;
  }
}
