package no.fint.provider.fiks.state;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import no.fint.arkiv.AdditionalFieldService;
import no.fint.arkiv.CaseProperties;
import no.fint.arkiv.TitleService;
import no.fint.model.arkiv.noark.Dokumentfil;
import no.fint.model.resource.Link;
import no.fint.model.resource.arkiv.noark.DokumentbeskrivelseResource;
import no.fint.model.resource.arkiv.noark.DokumentobjektResource;
import no.fint.model.resource.arkiv.noark.JournalpostResource;
import no.fint.model.resource.arkiv.noark.SaksmappeResource;
import no.fint.provider.fiks.model.FaxDocument;
import no.fint.provider.fiks.model.FaxShipment;
import no.fint.provider.fiks.model.ShipmentStatus;
import no.fint.provider.fiks.utils.FintUtils;
import no.fint.provider.fiks.utils.UrlUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FaxShipmentFactory {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TitleService titleService;

    @Autowired
    private AdditionalFieldService additionalFieldService;

    public FaxShipment saksmappeToFaxShipment(CaseProperties caseProperties, SaksmappeResource saksmappeResource, String applicationId, String orgId) {
        FaxShipment fax = new FaxShipment();
        fax.setApplicationId(applicationId);
        fax.setTitle(titleService.getCaseTitle(caseProperties.getTitle(), saksmappeResource));

        fax.setMetadata(additionalFieldService.getFieldsForResource(caseProperties.getField(), saksmappeResource)
                .collect(Collectors.toMap(AdditionalFieldService.Field::getName, AdditionalFieldService.Field::getValue)));

        try {
            fax.setPostamble(objectMapper
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                    .writeValueAsString(saksmappeResource));
        } catch (JsonProcessingException ignore) {
        }
        fax.setType(saksmappeResource.getClass().getSimpleName());
        fax.setOrgId(orgId);
        fax.setCreatedDate(FintUtils.getDate(saksmappeResource));
        fax.setDocuments(getFaxDocumentsFromJournalpost(saksmappeResource.getJournalpost()));
        return fax;
    }

    public List<FaxDocument> getFaxDocumentsFromJournalpost(List<JournalpostResource> journalposts) {
        if (journalposts == null) {
            return Collections.emptyList();
        }
        return journalposts
                .stream()
                .map(JournalpostResource::getDokumentbeskrivelse)
                .flatMap(List::stream)
                .map(DokumentbeskrivelseResource::getDokumentobjekt)
                .flatMap(List::stream)
                .map(DokumentobjektResource::getReferanseDokumentfil)
                .flatMap(List::stream)
                .map(Link::getHref)
                .peek(log::debug)
                .map(UrlUtils::getFileIdFromUri)
                .map(fileId ->
                        FaxDocument.builder()
                                .cacheFileId(fileId)
                                .shipmentStatus(ShipmentStatus.READY)
                                .fileUri(Link.with(Dokumentfil.class, "systemid", fileId).getHref())
                                .build())
                .collect(Collectors.toCollection(ArrayList::new));
    }

}
