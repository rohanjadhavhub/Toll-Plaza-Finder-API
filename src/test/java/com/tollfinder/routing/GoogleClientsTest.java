package com.tollfinder.routing;

import com.tollfinder.application.FinderException;
import com.tollfinder.config.FinderProperties;
import com.tollfinder.domain.GeoPoint;
import com.tollfinder.domain.Pincode;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GoogleClientsTest {
    private MockRestServiceServer server;
    private GoogleGeocodingClient geocoding;
    private GoogleRoutesClient routes;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        var configuration = new FinderProperties(50, 32, Duration.ofMinutes(15),
                new FinderProperties.Google("test-key", URI.create("https://example.test/geocode"),
                        URI.create("https://example.test/routes"), Duration.ofSeconds(1), Duration.ofSeconds(2)));
        var http = new GoogleHttpClient(builder.build(), configuration);
        geocoding = new GoogleGeocodingClient(http, configuration);
        routes = new GoogleRoutesClient(http, new PolylineDecoder());
    }

    @Test
    void checksIndianCountryAndExactPostalCode() {
        server.expect(method(HttpMethod.GET))
                .andExpect(request -> assertThat(request.getURI().getQuery())
                        .contains("components=postal_code:110001|country:IN"))
                .andRespond(withSuccess(geocodingResponse("110001", "IN"), MediaType.APPLICATION_JSON));
        assertThat(geocoding.resolve(new Pincode("110001"))).isEqualTo(new GeoPoint(28.6, 77.2));
        server.verify();
    }

    @Test
    void rejectsForeignOrSubstitutedPostalResults() {
        for (String body : new String[]{geocodingResponse("110002", "IN"), geocodingResponse("110001", "US"),
                "{\"status\":\"ZERO_RESULTS\",\"results\":[]}"}) {
            server.reset();
            server.expect(method(HttpMethod.GET)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
            assertThatThrownBy(() -> geocoding.resolve(new Pincode("110001")))
                    .isInstanceOfSatisfying(FinderException.class,
                            error -> assertThat(error.reason()).isEqualTo(FinderException.Reason.INVALID_PINCODE));
            server.verify();
        }
    }

    @Test
    void requestsDetailedDrivingRouteWithoutAlternativesAndDecodesIt() {
        server.expect(requestTo("https://example.test/routes")).andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Goog-Api-Key", "test-key"))
                .andExpect(jsonPath("$.polylineQuality").value("HIGH_QUALITY"))
                .andExpect(jsonPath("$.computeAlternativeRoutes").value(false))
                .andExpect(jsonPath("$.origin.location.latLng.latitude").value(28.6))
                .andRespond(withSuccess("""
                        {"routes":[{"distanceMeters":1000,"polyline":{"encodedPolyline":"_p~iF~ps|U_ulLnnqC_mqNvxq`@"}}]}
                        """, MediaType.APPLICATION_JSON));
        var route = routes.compute(new GeoPoint(28.6, 77.2), new GeoPoint(12.9, 77.6));
        assertThat(route.distanceMeters()).isEqualTo(1000);
        assertThat(route.points()).hasSize(3);
        server.verify();
    }

    @Test
    void mapsQuotaTimeoutMalformedAndNoRouteFailures() {
        server.expect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        assertRouteError(FinderException.Reason.UNAVAILABLE);
        server.reset();
        server.expect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.GATEWAY_TIMEOUT));
        assertRouteError(FinderException.Reason.TIMEOUT);
        server.reset();
        server.expect(method(HttpMethod.POST)).andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));
        assertRouteError(FinderException.Reason.UPSTREAM_FAILURE);
        server.reset();
        server.expect(method(HttpMethod.POST)).andRespond(withSuccess("{\"routes\":[]}", MediaType.APPLICATION_JSON));
        assertRouteError(FinderException.Reason.NO_ROUTE);
    }

    private void assertRouteError(FinderException.Reason reason) {
        assertThatThrownBy(() -> routes.compute(new GeoPoint(28, 77), new GeoPoint(29, 78)))
                .isInstanceOfSatisfying(FinderException.class, error -> assertThat(error.reason()).isEqualTo(reason));
        server.verify();
    }

    private String geocodingResponse(String postalCode, String country) {
        return """
                {"status":"OK","results":[{"address_components":[
                    {"short_name":"%s","types":["postal_code"]},
                    {"short_name":"%s","types":["country"]}],
                    "geometry":{"location":{"lat":28.6,"lng":77.2}}}]}
                """.formatted(postalCode, country);
    }
}
