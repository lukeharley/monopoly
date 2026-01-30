package com.luca.customscraper.model;

import java.util.List;

public record MatchingResult(
    ProductData product,
    List<MatchCandidate> candidates
) {}
