package no.fint.provider.fiks.state;

import lombok.extern.slf4j.Slf4j;
import no.fint.arkiv.CaseProperties;
import no.fint.model.resource.arkiv.noark.SaksmappeResource;
import no.fint.provider.fiks.SvarUtConfiguration;
import no.fint.provider.fiks.exception.GetSakNotFoundException;
import no.fint.provider.fiks.model.FaxShipment;
import no.fint.provider.fiks.model.ShipmentStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
public class FaxShipmentState {

    @Autowired
    private FaxShipmentRepository faxShipmentRepository;

    @Autowired
    private SvarUtConfiguration svarUtConfiguration;

    @Autowired
    private FaxShipmentFactory faxShipmentFactory;

    @Autowired
    private CaseIdFactory caseIdFactory;

    public FaxShipment getById(String orgId, String id) {
        FaxShipment faxShipment = faxShipmentRepository.findByOrgIdAndId(orgId, id);
        if (faxShipment == null) {
            throw new GetSakNotFoundException("Case could not be found by ID " + id);
        }
        if (faxShipment.getCaseId() == null) {
            save(faxShipment);
        }
        return faxShipment;
    }

    public FaxShipment getByApplicationId(String orgId, String applicationId) {
        FaxShipment faxShipment = faxShipmentRepository.findByOrgIdAndApplicationId(orgId, applicationId);
        if (faxShipment == null) {
            throw new GetSakNotFoundException("Case could not be found by application Id " + applicationId);
        }
        if (faxShipment.getCaseId() == null) {
            save(faxShipment);
        }
        return faxShipment;
    }

    public FaxShipment getByCaseId(String orgId, String caseId) {
        FaxShipment faxShipment = faxShipmentRepository.findByOrgIdAndCaseId(orgId, caseId);
        log.info("faxShipment: {}", faxShipment);
        if (faxShipment == null) {
            throw new GetSakNotFoundException("Case could not be found by Case ID " + caseId);
        }
        return faxShipment;
    }

    public FaxShipment createFaxShipment(CaseProperties caseProperties, SaksmappeResource saksmappeResource, String applicationId) {
        return save(faxShipmentFactory.saksmappeToFaxShipment(caseProperties, saksmappeResource, applicationId, svarUtConfiguration.getOrgId()));
    }

    public FaxShipment updateFaxShipment(SaksmappeResource saksmappeResource, FaxShipment faxShipment) {
        faxShipment.getDocuments().addAll(faxShipmentFactory.getFaxDocumentsFromJournalpost(saksmappeResource.getJournalpost()));
        save(faxShipment);
        return faxShipment;
    }

    public FaxShipment save(FaxShipment faxShipment) {
        if (faxShipment.getCaseId() == null) {
            faxShipment.setCaseId(
                    caseIdFactory.getCaseId(
                            faxShipment.getOrgId(),
                            faxShipment.getCreatedDate()
                                    .toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .getYear()));
        }
        faxShipmentRepository.save(faxShipment);
        return faxShipment;
    }

    public List<FaxShipment> getNotSent() {
        return faxShipmentRepository.findByOrgIdAndDocumentsShipmentStatus(ShipmentStatus.READY.name(), svarUtConfiguration.getOrgId());
    }

    public List<FaxShipment> getAll() {
        return faxShipmentRepository.findAllByOrgId(svarUtConfiguration.getOrgId());
    }

}
