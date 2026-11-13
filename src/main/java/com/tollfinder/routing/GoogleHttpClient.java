package com.tollfinder.routing;

import com.fasterxml.jackson.databind.JsonNode;
import com.tollfinder.application.FinderException;
import com.tollfinder.config.FinderProperties;
import java.net.URI;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import static com.tollfinder.application.FinderException.Reason.*;

@Component
public class GoogleHttpClient {
    private final RestClient client;
    private final FinderProperties.Google configuration;

    public GoogleHttpClient(RestClient googleRestClient, FinderProperties properties) {
        this.client = googleRestClient;
        this.configuration = properties.google();
    }

    public JsonNode get(URI uri) {
        return execute(() -> client.get().uri(uri).retrieve().body(JsonNode.class));
    }

    public JsonNode postRoute(Object request) {
        return execute(() -> client.post().uri(configuration.routesUrl())
                .header("X-Goog-Api-Key", configuration.apiKey())
                .header("X-Goog-FieldMask", "routes.distanceMeters,routes.polyline.encodedPolyline")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(request).retrieve().body(JsonNode.class));
    }

    private JsonNode execute(Supplier<JsonNode> request) {
        if (configuration.apiKey() == null || configuration.apiKey().isBlank()) {
            throw new FinderException(UNAVAILABLE, "Routing service is not configured");
        }
        try {
            JsonNode body = request.get();
            if (body == null || !body.isObject()) throw invalidResponse();
            return body;
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            if (status == 408 || status == 504) throw new FinderException(TIMEOUT, "Routing service timed out");
            if (status == 429 || status >= 500 || status == 401 || status == 403) {
                throw new FinderException(UNAVAILABLE, "Routing service is temporarily unavailable");
            }
            throw invalidResponse();
        } catch (ResourceAccessException exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException) {
                    throw new FinderException(TIMEOUT, "Routing service timed out");
                }
            }
            throw new FinderException(UNAVAILABLE, "Routing service is temporarily unavailable");
        } catch (RestClientException exception) {
            throw invalidResponse();
        }
    }

    static FinderException invalidResponse() {
        return new FinderException(UPSTREAM_FAILURE, "Routing service returned an invalid response");
    }
}
