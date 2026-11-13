package com.tollfinder.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("finder")
public record FinderProperties(@Min(1) @Max(1000) double corridorMeters,
                               @Min(1) int cacheMaximumCells, @NotNull Duration cacheTtl,
                               @NotNull @Valid Google google) {
    public record Google(String apiKey, @NotNull URI geocodingUrl, @NotNull URI routesUrl,
                         @NotNull Duration connectTimeout, @NotNull Duration readTimeout) {}
}
