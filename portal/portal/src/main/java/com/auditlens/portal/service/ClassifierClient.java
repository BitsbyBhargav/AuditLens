package com.auditlens.portal.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/** Calls the FastAPI classifier (nlp/serve.py). In the cloud phase the same model runs in Lambda. */
@Component
public class ClassifierClient {
    private static final Logger log = LoggerFactory.getLogger(ClassifierClient.class);

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Prediction(@JsonProperty("risk_label") String riskLabel,
                             double confidence,
                             Map<String, Double> probabilities) {}

    private final RestClient client;

    public ClassifierClient(RestClient.Builder builder, @Value("${auditlens.classifier-url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(60));
        this.client = builder.baseUrl(baseUrl).requestFactory(factory).build();
    }

    public Optional<Prediction> classify(String modelInput) {
        try {
            Prediction p = client.post().uri("/classify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", modelInput))
                    .retrieve()
                    .body(Prediction.class);
            return Optional.ofNullable(p);
        } catch (Exception e) {
            log.warn("Classifier call failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public boolean isAvailable() {
        try {
            client.get().uri("/health").retrieve().toBodilessEntity();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
