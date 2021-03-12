package no.fint.provider.fiks.service.fiks;

import lombok.extern.slf4j.Slf4j;
import no.fint.arkiv.fiks.svarut.*;
import no.fint.model.resource.arkiv.noark.DokumentfilResource;
import no.fint.provider.fiks.SvarUtConfiguration;
import no.fint.provider.fiks.model.FaxDocument;
import no.fint.provider.fiks.model.FaxShipment;
import no.fint.provider.fiks.model.ShipmentStatus;
import no.fint.provider.fiks.repository.InternalRepository;
import no.fint.provider.fiks.utils.XmlUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import javax.activation.DataHandler;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FiksFactory {

    @Autowired
    private SvarUtConfiguration svarUtConfiguration;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private InternalRepository internalRepository;

    @Autowired
    private XmlUtils xmlUtils;

    public Forsendelse createForsendelse(FaxShipment fax, String shipmentId) {
        Forsendelse forsendelse = new Forsendelse();
        forsendelse.setAvgivendeSystem(svarUtConfiguration.getAvgivendeSystem());
        forsendelse.setKunDigitalLevering(svarUtConfiguration.isKunDigitalLevering());
        forsendelse.setKonteringsKode(svarUtConfiguration.getKonteringsKode());
        forsendelse.setKryptert(svarUtConfiguration.isKryptert());
        forsendelse.setKrevNiva4Innlogging(svarUtConfiguration.isKrevNiva4Innlogging());

        forsendelse.setEksternReferanse(fax.getId());
        forsendelse.setMetadataFraAvleverendeSystem(createMetadataFraAvleverendeSystem(fax));
        forsendelse.setMetadataForImport(createMetadataForImport(fax));

        forsendelse.setMottaker(createAdresse(svarUtConfiguration.getOrganization()));
        forsendelse.setUtskriftsKonfigurasjon(createUtskriftsKonfigurasjon());
        forsendelse.setTittel(fax.getTitle());

        forsendelse.getDokumenter().addAll(createDocuments(fax, shipmentId));

        if (svarUtConfiguration.isMetadata() && StringUtils.isNotBlank(fax.getPostamble())) {
            forsendelse.getDokumenter().add(createMetadataDokument(fax.getPostamble()));
        }
        return forsendelse;
    }

    private NoarkMetadataForImport createMetadataForImport(FaxShipment fax) {
        NoarkMetadataForImport metadata = new NoarkMetadataForImport();
        metadata.setTittel(fax.getTitle());
        metadata.setSaksAar(Integer.parseInt(StringUtils.substringBefore(fax.getCaseId(), "/")));
        metadata.setSaksSekvensNummer(Integer.parseInt(StringUtils.substringAfter(fax.getCaseId(), "/")));
        metadata.setDokumentetsDato(xmlUtils.xmlDate(fax.getCreatedDate()));
        return metadata;
    }

    private NoarkMetadataFraAvleverendeSaksSystem createMetadataFraAvleverendeSystem(FaxShipment fax) {
        NoarkMetadataFraAvleverendeSaksSystem metadata = new NoarkMetadataFraAvleverendeSaksSystem();
        metadata.setSaksSekvensNummer(0);
        metadata.setSaksAar(0);
        metadata.setJournalAar(0);
        metadata.setJournalSekvensNummer(0);
        metadata.setJournalPostNummer(0);
        metadata.setDokumentetsDato(xmlUtils.xmlDate(fax.getCreatedDate()));
        metadata.setJournalDato(xmlUtils.xmlDate(fax.getCreatedDate()));

        metadata.setTittel(fax.getTitle());
        fax.getMetadata().entrySet().stream().map(this::createEntry).forEach(metadata.getEkstraMetadata()::add);
        return metadata;
    }

    private Entry createEntry(Map.Entry<String, String> input) {
        Entry entry = new Entry();
        entry.setKey(input.getKey());
        entry.setValue(input.getValue());
        return entry;
    }

    private Dokument createMetadataDokument(String json) {
        Dokument dokument = new Dokument();
        dokument.setFilnavn("metadata.json");
        dokument.setEkskluderesFraUtskrift(true);
        dokument.setSkalSigneres(false);
        dokument.setMimeType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        if (svarUtConfiguration.isKryptert()) {
            dokument.setData(new DataHandler(encryptionService.encrypt(json.getBytes(StandardCharsets.UTF_8)), MediaType.APPLICATION_JSON_UTF8_VALUE));
        } else {
            dokument.setData(new DataHandler(json.getBytes(StandardCharsets.UTF_8), MediaType.APPLICATION_JSON_UTF8_VALUE));
        }
        return dokument;
    }

    private List<Dokument> createDocuments(FaxShipment fax, String shipmentId) {
        return fax.getDocuments()
                .stream()
                .filter(document -> document.getShipmentStatus().equals(ShipmentStatus.READY))
                .peek(document -> document.setSvarUtShipmentId(shipmentId))
                .map(this::createDokument)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Dokument createDokument(FaxDocument document) {
        if (document.getErrorCount() > svarUtConfiguration.getErrorLimit()) {
            document.setShipmentStatus(ShipmentStatus.ERROR);
            return null;
        }
        try {
            Dokument dokument = new Dokument();
            DokumentfilResource dokumentfilResource = internalRepository.getFile(document.getCacheFileId());
            dokument.setMimeType(dokumentfilResource.getFormat());
            //dokument.setFilnavn(Utils.createFileName(faxShipment, dokumentfilResource.getFormat()));
            dokument.setFilnavn(dokumentfilResource.getFilnavn());
            dokument.setEkskluderesFraUtskrift(true);
            dokument.setSkalSigneres(false);

            if (svarUtConfiguration.isKryptert()) {
                dokument.setData(new DataHandler(encryptionService.encrypt(decodeFile(dokumentfilResource)), dokumentfilResource.getFormat()));
            } else {
                dokument.setData(new DataHandler(decodeFile(dokumentfilResource), dokumentfilResource.getFormat()));
            }
            return dokument;

        } catch (Exception e) {
            log.debug("Unable to add document! {}", document, e);
            document.setShipmentStatus(ShipmentStatus.ERROR);
            document.setErrorMessage(e.toString());
        }
        return null;
    }

    private byte[] decodeFile(DokumentfilResource dokumentfilResource) {
        return Base64.getDecoder().decode(dokumentfilResource.getData());
    }

    private static Adresse createAdresse(SvarUtConfiguration.Organization organization) {
        Adresse adresse = new Adresse();
        adresse.setDigitalAdresse(createOrganisationDigialAdresse(organization.getNumber()));
        adresse.setPostAdresse(createPostAdresse(organization));

        return adresse;
    }

    private static PostAdresse createPostAdresse(SvarUtConfiguration.Organization organization) {
        PostAdresse postAdresse = new PostAdresse();
        postAdresse.setNavn(organization.getName());
        postAdresse.setAdresse1(organization.getAddress1());
        postAdresse.setAdresse2(organization.getAddress2());
        postAdresse.setAdresse3(organization.getAddress3());
        postAdresse.setPostNummer(organization.getPostalCode());
        postAdresse.setPostSted(organization.getCity());
        return postAdresse;
    }

    private static UtskriftsKonfigurasjon createUtskriftsKonfigurasjon() {
        UtskriftsKonfigurasjon utskriftsKonfigurasjon = new UtskriftsKonfigurasjon();
        utskriftsKonfigurasjon.setTosidig(true);
        utskriftsKonfigurasjon.setUtskriftMedFarger(false);

        return utskriftsKonfigurasjon;
    }

    private static OrganisasjonDigitalAdresse createOrganisationDigialAdresse(String organisationNumber) {
        OrganisasjonDigitalAdresse organisasjonDigitalAdresse = new OrganisasjonDigitalAdresse();
        OrganisasjonsNummer organisasjonsNummer = new OrganisasjonsNummer();
        organisasjonsNummer.setId(organisationNumber);
        organisasjonDigitalAdresse.setOrganisasjonsNummer(organisasjonsNummer);

        return organisasjonDigitalAdresse;
    }
}
