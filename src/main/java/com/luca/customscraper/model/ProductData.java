package com.luca.customscraper.model;

import java.util.Map;

public record ProductData(
    String url,
    String productName,
    String commercialName,
    String producer,
    String format,
    Map<String, String> attributes
) {}
