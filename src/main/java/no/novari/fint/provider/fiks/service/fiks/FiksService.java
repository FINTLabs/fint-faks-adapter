package no.novari.fint.provider.fiks.service.fiks;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import no.novari.fint.provider.fiks.SvarUtConfiguration;
import no.novari.fint.provider.fiks.model.*;
import no.novari.fint.provider.fiks.state.FaxShipmentState;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FiksService {
    private final FaxShipmentState state;
    private final SvarUtConfiguration config;
    private final FiksFactory factory;
    private final MaskinportenTokenService tokens;
    private final RestTemplate http;

    public FiksService(FaxShipmentState state, SvarUtConfiguration config, FiksFactory factory,
                       MaskinportenTokenService tokens) {
        this.state = state;
        this.config = config;
        this.factory = factory;
        this.tokens = tokens;
        var requests = new SimpleClientHttpRequestFactory();
        requests.setConnectTimeout(config.getConnectionTimeout());
        requests.setReadTimeout(config.getReceiveTimeout());
        http = new RestTemplate(requests);
    }

    private String endpoint(String suffix) {
        UUID.fromString(config.getKontoId());
        return config.getServiceUrl().replaceAll("/+$", "") + "/v3/kontoer/" + config.getKontoId() + "/forsendelser" + suffix;
    }

    private HttpHeaders headers(MediaType type) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokens.getAccessToken());
        headers.set("IntegrasjonId", Objects.requireNonNull(config.getIntegrasjonId()));
        headers.set("IntegrasjonPassord", Objects.requireNonNull(config.getIntegrasjonPassord()));
        headers.setContentType(type);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    public Boolean healthy() {
        if (!config.isEnabled()) return false;
        try {
            return statuses(Collections.emptyList()) != null;
        } catch (RuntimeException e) {
            log.warn("SvarUt health check failed ({})", e.getClass().getSimpleName());
            return false;
        }
    }

    private JsonNode statuses(List<String> ids) {
        return http.postForObject(endpoint("/status/sok"),
                new HttpEntity<>(Map.of("id", ids), headers(MediaType.APPLICATION_JSON)), JsonNode.class);
    }

    public record ForsendelsesStatus(String status, Date sisteStatusEndring) { }

    public ForsendelsesStatus getForsendelsesStatus(String id) {
        UUID.fromString(id);
        JsonNode response = statuses(List.of(id));
        if (response != null) {
            for (JsonNode status : response.path("statuser")) {
                if (id.equals(status.path("id").asText()))
                    return new ForsendelsesStatus(status.required("status").asText(),
                            Date.from(Instant.parse(status.required("sisteStatusEndring").asText())));
            }
        }
        throw new IllegalStateException("SvarUt returned no status for " + id);
    }

    public synchronized void fax(FaxShipment fax) {
        if (!config.isEnabled()) return;
        List<FaxDocument> documents = fax.getDocuments().stream()
                .filter(doc -> doc.getShipmentStatus() == ShipmentStatus.READY).collect(Collectors.toList());
        if (documents.isEmpty()) return;
        boolean sending = false;
        try {
            var body = factory.createForsendelse(fax, documents);
            var url = endpoint("/organisasjon");
            var request = new HttpEntity<>(body, headers(MediaType.MULTIPART_FORM_DATA));
            // Persist before POST: a crash or timeout must not cause an automatic duplicate send.
            // shortcut: Unconfirmed sends need manual reconciliation until automatic lookup is implemented.
            documents.forEach(doc -> {
                doc.setSvarUtShipmentId(null);
                doc.setShipmentStatus(ShipmentStatus.DELIVERY_UNCONFIRMED);
                doc.setErrorMessage("Delivery unconfirmed; reconcile with SvarUt before retrying");
            });
            state.save(fax);
            sending = true;
            JsonNode response = http.postForObject(url, request, JsonNode.class);
            String id = UUID.fromString(Objects.requireNonNull(response).required("id").asText()).toString();
            documents.forEach(doc -> {
                doc.setSvarUtShipmentId(id);
                doc.setShipmentStatus(ShipmentStatus.SENT_TO_SVARUT);
                doc.setShipmentTime(new Date());
                doc.setErrorMessage(null);
            });
        } catch (Exception e) {
            log.warn("SvarUt send failed for {} ({})", fax.getId(), e.getClass().getSimpleName());
            for (FaxDocument doc : documents) {
                doc.setErrorCount(doc.getErrorCount() + 1);
                if (!sending) {
                    doc.setShipmentStatus(doc.getErrorCount() > config.getErrorLimit() ? ShipmentStatus.ERROR : ShipmentStatus.READY);
                    doc.setErrorMessage("Preparation failed: " + e.getClass().getSimpleName());
                }
            }
        } finally {
            state.save(fax);
        }
    }

    @Scheduled(initialDelayString = "${fint.faks.delay.initial:30000}", fixedDelayString = "${fint.faks.delay.fixed:30000}")
    public void sendFaxes() {
        if (config.isEnabled()) state.getNotSent().forEach(this::fax);
    }
}
