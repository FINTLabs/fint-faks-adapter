package no.novari.fint.provider.fiks.service.fiks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.novari.fint.provider.fiks.SvarUtConfiguration;
import no.novari.fint.provider.fiks.model.FaxDocument;
import no.novari.fint.provider.fiks.model.FaxShipment;
import no.novari.fint.provider.fiks.repository.InternalRepository;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Service
public class FiksFactory {
    private final SvarUtConfiguration config;
    private final EncryptionService encryption;
    private final InternalRepository repository;
    private final ObjectMapper mapper;

    public FiksFactory(SvarUtConfiguration config, EncryptionService encryption,
                       InternalRepository repository, ObjectMapper mapper) {
        this.config = config;
        this.encryption = encryption;
        this.repository = repository;
        this.mapper = mapper;
    }

    public MultiValueMap<String, Object> createForsendelse(FaxShipment fax, List<FaxDocument> documents) throws java.io.IOException {
        if (documents.isEmpty()) throw new IllegalArgumentException("No documents to send");
        if (config.isKrevNiva4Innlogging() && !config.isKryptert())
            throw new IllegalArgumentException("HOYESTE requires CMS encryption");
        if (config.getOrganization() == null || !StringUtils.hasText(config.getOrganization().getName())
                || config.getOrganization().getNumber() == null
                || !config.getOrganization().getNumber().matches("[0-9]{9}"))
            throw new IllegalArgumentException("SvarUt requires recipient name and organization number");
        if (!StringUtils.hasText(fax.getTitle())) throw new IllegalArgumentException("Shipment needs a title");
        ObjectNode metadata = mapper.createObjectNode();
        metadata.put("tittel", fax.getTitle());
        metadata.put("eksternReferanse", fax.getId());
        put(metadata, "avgivendeSystem", config.getAvgivendeSystem());
        put(metadata, "konteringskode", config.getKonteringsKode());
        metadata.put("kunDigitalLevering", config.isKunDigitalLevering());
        metadata.put("kryptert", config.isKryptert());
        metadata.put("sikkerhetsniva", config.isKrevNiva4Innlogging() ? "HOYESTE" : "BETYDELIG");
        metadata.putObject("utskriftskonfigurasjon").put("tosidig", true);
        var organization = config.getOrganization();
        ObjectNode recipient = metadata.putObject("mottaker");
        recipient.put("navn", organization.getName());
        recipient.put("digitalId", organization.getNumber());
        if (StringUtils.hasText(organization.getAddress1())) {
            ObjectNode address = recipient.putObject("adresse");
            address.put("adresselinje1", organization.getAddress1());
            put(address, "adresselinje2", organization.getAddress2());
            put(address, "adresselinje3", organization.getAddress3());
            put(address, "postnummer", organization.getPostalCode());
            address.put("poststed", organization.getCity());
        }
        String date = fax.getCreatedDate().toInstant().toString();
        ObjectNode source = metadata.putObject("metadataFraAvleverendeSystem");
        source.put("tittel", fax.getTitle()).put("dokumentetsDato", date).put("journaldato", date);
        var extra = source.putArray("ekstraMetadata");
        fax.getMetadata().forEach((key, value) -> extra.addObject().put("key", key).put("value", value));
        String[] caseId = fax.getCaseId().split("/");
        metadata.putObject("metadataForImport").put("tittel", fax.getTitle())
                .put("saksaar", Integer.parseInt(caseId[0])).put("sakssekvensnummer", Long.parseLong(caseId[1]))
                .put("dokumentetsDato", date);
        metadata.putArray("dokumenter");
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        HttpHeaders json = new HttpHeaders();
        json.setContentType(MediaType.APPLICATION_JSON);
        parts.add("forsendelse", new HttpEntity<>(metadata, json));
        for (FaxDocument document : documents) {
            var file = repository.getFile(document.getCacheFileId());
            addDocument(parts, metadata, file.getFilnavn(), file.getFormat(), Base64.getDecoder().decode(file.getData()));
        }
        if (config.isMetadata() && StringUtils.hasText(fax.getPostamble()))
            addDocument(parts, metadata, "metadata.json", "application/json", fax.getPostamble().getBytes(StandardCharsets.UTF_8));
        return parts;
    }

    private void addDocument(MultiValueMap<String, Object> parts, ObjectNode metadata,
                             String filename, String mimeType, byte[] data) {
        if (!StringUtils.hasText(filename)) throw new IllegalArgumentException("Document needs a filename");
        metadata.withArray("dokumenter").addObject().put("filnavn", filename)
                .put("mimeType", mimeType).put("skalEkskluderesFraUtskrift", true);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(mimeType));
        byte[] payload = config.isKryptert() ? encryption.encrypt(data) : data;
        parts.add("dokumenter", new HttpEntity<>(new ByteArrayResource(payload) {
            @Override public String getFilename() { return filename; }
        }, headers));
    }

    private static void put(ObjectNode node, String key, String value) {
        if (StringUtils.hasText(value)) node.put(key, value);
    }
}
