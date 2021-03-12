package no.fint.provider.fiks.service.fint;

import com.google.common.collect.Streams;
import no.fint.arkiv.AdditionalFieldService;
import no.fint.arkiv.CaseProperties;
import no.fint.arkiv.TitleService;
import no.fint.arkiv.fiks.svarut.ForsendelsesStatus;
import no.fint.model.arkiv.kodeverk.DokumentStatus;
import no.fint.model.resource.Link;
import no.fint.model.resource.arkiv.noark.*;
import no.fint.provider.fiks.model.FaxDocument;
import no.fint.provider.fiks.model.FaxShipment;
import no.fint.provider.fiks.model.ShipmentStatus;
import no.fint.provider.fiks.service.fiks.FiksService;

import javax.xml.ws.soap.SOAPFaultException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

import static no.fint.provider.fiks.utils.FintUtils.createIdentifikator;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;

public abstract class SaksmappeFactory {
    public static final String KLART_TIL_SVARUT = "KLART_TIL_SVARUT";

    private final FiksService fiksService;
    private final TitleService titleService;
    private final AdditionalFieldService additionalFieldService;

    protected SaksmappeFactory(FiksService fiksService, TitleService titleService, AdditionalFieldService additionalFieldService) {
        this.fiksService = fiksService;
        this.titleService = titleService;
        this.additionalFieldService = additionalFieldService;
    }

    protected void updateSaksmappeFromFaxShipment(CaseProperties caseProperties, FaxShipment faxShipment, SaksmappeResource saksmappeResource) {
        saksmappeResource.setSystemId(createIdentifikator(faxShipment.getId()));
        saksmappeResource.setMappeId(createIdentifikator(faxShipment.getCaseId()));
        saksmappeResource.setSaksaar(substringBefore(faxShipment.getCaseId(), "/"));
        saksmappeResource.setSakssekvensnummer(substringAfter(faxShipment.getCaseId(), "/"));

        saksmappeResource.setTittel(faxShipment.getTitle());
        saksmappeResource.setOpprettetDato(faxShipment.getCreatedDate());
        faxShipment.getDocuments()
                .stream()
                .map(FaxDocument::getShipmentTime)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .ifPresent(saksmappeResource::setSaksdato);
        saksmappeResource.setJournalpost(
                Streams.mapWithIndex(
                        faxShipment.getDocuments().stream(),
                        this::getJournalpostFromFaxDocument)
                        .peek(jp -> {
                            jp.setJournalAr(saksmappeResource.getSaksaar());
                            jp.setJournalSekvensnummer(Long.valueOf(saksmappeResource.getSakssekvensnummer()));
                        })
                        .collect(Collectors.toList())
        );

        titleService.parseCaseTitle(caseProperties.getTitle(), saksmappeResource, faxShipment.getTitle());
        additionalFieldService.setFieldsForResource(caseProperties.getField(), saksmappeResource,
                faxShipment.getMetadata()
                        .entrySet()
                        .stream()
                        .map(e -> new AdditionalFieldService.Field(e.getKey(), e.getValue()))
                        .collect(Collectors.toList()));
    }

    protected JournalpostResource getJournalpostFromFaxDocument(FaxDocument doc, long index) {
        JournalpostResource journalpostResource = new JournalpostResource();
        journalpostResource.setRegistreringsId(doc.getSvarUtShipmentId());
        journalpostResource.setTittel("Sendt via SvarUT");
        journalpostResource.setBeskrivelse(doc.getErrorMessage());
        journalpostResource.setJournalPostnummer(index + 1);
        journalpostResource.setJournalDato(doc.getShipmentTime());
        DokumentbeskrivelseResource dokumentbeskrivelseResource = new DokumentbeskrivelseResource();
        if (doc.getShipmentStatus().equals(ShipmentStatus.READY)) {
            dokumentbeskrivelseResource.addDokumentstatus(Link.with(DokumentStatus.class, "systemid", KLART_TIL_SVARUT));
        } else {
            try {
                ForsendelsesStatus forsendelsesStatus = fiksService.getForsendelsesStatus(doc.getSvarUtShipmentId());
                dokumentbeskrivelseResource.addDokumentstatus(Link.with(DokumentStatus.class, "systemid", forsendelsesStatus.getStatus().name()));
                journalpostResource.setArkivertDato(forsendelsesStatus.getSisteStatusEndring().toGregorianCalendar().getTime());
            } catch (SOAPFaultException e) {
                dokumentbeskrivelseResource.setBeskrivelse(e.getMessage());
            }

        }
        DokumentobjektResource dokumentobjektResource = new DokumentobjektResource();
        dokumentobjektResource.addReferanseDokumentfil(
                Link.with(DokumentfilResource.class,
                        "systemid",
                        doc.getCacheFileId()));
        dokumentbeskrivelseResource.setDokumentobjekt(Collections.singletonList(dokumentobjektResource));
        journalpostResource.setDokumentbeskrivelse(Collections.singletonList(dokumentbeskrivelseResource));
        return journalpostResource;
    }
}
