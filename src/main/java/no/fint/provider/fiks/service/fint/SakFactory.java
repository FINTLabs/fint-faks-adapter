package no.fint.provider.fiks.service.fint;

import no.fint.arkiv.AdditionalFieldService;
import no.fint.arkiv.CaseProperties;
import no.fint.arkiv.TitleService;
import no.fint.model.resource.arkiv.noark.SakResource;
import no.fint.provider.fiks.model.FaxShipment;
import no.fint.provider.fiks.service.fiks.FiksService;
import org.springframework.stereotype.Service;

@Service
public class SakFactory extends SaksmappeFactory {

    private final CaseProperties caseProperties;

    public SakFactory(FiksService fiksService, TitleService titleService, AdditionalFieldService additionalFieldService) {
        super(fiksService, titleService, additionalFieldService);
        caseProperties = new CaseProperties();
    }

    public SakResource toFintResource(FaxShipment faxShipment) {
        SakResource sakResource = new SakResource();
        updateSaksmappeFromFaxShipment(caseProperties, faxShipment, sakResource);
        return sakResource;

    }

}
