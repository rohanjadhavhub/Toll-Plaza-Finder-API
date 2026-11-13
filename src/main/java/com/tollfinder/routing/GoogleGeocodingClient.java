package com.tollfinder.routing;

import com.fasterxml.jackson.databind.JsonNode;
import com.tollfinder.application.FinderException;
import com.tollfinder.config.FinderProperties;
import com.tollfinder.domain.GeoPoint;
import com.tollfinder.domain.Pincode;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import static com.tollfinder.application.FinderException.Reason.*;

@Component
public class GoogleGeocodingClient {
    private final GoogleHttpClient http;
    private final FinderProperties.Google configuration;

    public GoogleGeocodingClient(GoogleHttpClient http, FinderProperties properties) {
        this.http = http;
        this.configuration = properties.google();
    }

    public GeoPoint resolve(Pincode pincode) {
        var uri = UriComponentsBuilder.fromUri(configuration.geocodingUrl())
                .queryParam("components", "postal_code:" + pincode.value() + "|country:IN")
                .queryParam("key", configuration.apiKey()).build().encode().toUri();
        JsonNode body = http.get(uri);
        String status = body.path("status").asText();
        if (status.equals("ZERO_RESULTS")) throw invalidPincode();
        if (status.equals("OVER_QUERY_LIMIT") || status.equals("OVER_DAILY_LIMIT")
                || status.equals("REQUEST_DENIED") || status.equals("UNKNOWN_ERROR")) {
            throw new FinderException(UNAVAILABLE, "Routing service is temporarily unavailable");
        }
        if (!status.equals("OK") || !body.path("results").isArray()) throw GoogleHttpClient.invalidResponse();
        for (JsonNode result : body.path("results")) {
            if (result.path("partial_match").asBoolean(false)) continue;
            boolean postalCodeMatches = false;
            boolean countryMatches = false;
            for (JsonNode component : result.path("address_components")) {
                for (JsonNode type : component.path("types")) {
                    if (type.asText().equals("postal_code") && component.path("short_name").asText().equals(pincode.value())) {
                        postalCodeMatches = true;
                    }
                    if (type.asText().equals("country") && component.path("short_name").asText().equals("IN")) {
                        countryMatches = true;
                    }
                }
            }
            if (postalCodeMatches && countryMatches) {
                JsonNode location = result.path("geometry").path("location");
                if (!location.path("lat").isNumber() || !location.path("lng").isNumber()) {
                    throw GoogleHttpClient.invalidResponse();
                }
                try {
                    return new GeoPoint(location.path("lat").doubleValue(), location.path("lng").doubleValue());
                } catch (IllegalArgumentException exception) {
                    throw GoogleHttpClient.invalidResponse();
                }
            }
        }
        throw invalidPincode();
    }

    private FinderException invalidPincode() {
        return new FinderException(INVALID_PINCODE, "Invalid source or destination pincode");
    }
}
