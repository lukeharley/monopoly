package com.luca.customscraper.model;

import java.util.Map;

public record MatchCandidate(
    ReferenceProduct referenceProduct,
    double score,
    Map<String, Double> attributeScores
) {}
