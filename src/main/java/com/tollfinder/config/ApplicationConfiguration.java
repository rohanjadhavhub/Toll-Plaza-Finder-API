package com.tollfinder.config;

import com.tollfinder.cache.PlazaCandidateCache;
import com.tollfinder.routing.PolylineDecoder;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import java.time.Clock;
import java.net.http.HttpClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class ApplicationConfiguration {
    @Bean
    PolylineDecoder polylineDecoder() { return new PolylineDecoder(); }

    @Bean
    RestClient googleRestClient(RestClient.Builder builder, FinderProperties properties) {
        var google = properties.google();
        var client = HttpClient.newBuilder().connectTimeout(google.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER).build();
        var factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(google.readTimeout());
        return builder.requestFactory(factory).build();
    }

    @Bean
    PlazaCandidateCache plazaCandidateCache(FinderProperties properties) {
        return new PlazaCandidateCache(properties.cacheMaximumCells(), properties.cacheTtl(), Clock.systemUTC());
    }

    @Bean
    TransactionTemplate readTransaction(PlatformTransactionManager manager) {
        var template = new TransactionTemplate(manager);
        template.setReadOnly(true);
        template.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        template.setTimeout(15);
        return template;
    }

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI().info(new Info().title("Toll Plaza Finder API").version("v1")
                .description("Indian PIN-code route searches. Distances are in kilometers. "
                        + "Plazas are matched within a 50-meter corridor by default; parallel roads and "
                        + "opposite carriageways can produce matches. Routing: Google Maps."));
    }
}
