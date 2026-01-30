package com.luca.customscraper.model;

import java.util.Map;

public record ReferenceProduct(
    String referenceId,
    String denomination,
    String commercialName,
    String producer,
    String format,
    Map<String, String> attributes
) {}
