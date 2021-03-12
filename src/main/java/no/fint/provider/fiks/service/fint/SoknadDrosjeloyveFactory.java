package no.fint.provider.fiks.service.fint;

import no.fint.arkiv.AdditionalFieldService;
import no.fint.arkiv.CaseDefaults;
import no.fint.arkiv.CaseProperties;
import no.fint.arkiv.TitleService;
import no.fint.model.resource.arkiv.samferdsel.SoknadDrosjeloyveResource;
import no.fint.provider.fiks.model.FaxShipment;
import no.fint.provider.fiks.service.fiks.FiksService;
import org.springframework.stereotype.Service;

@Service
public class SoknadDrosjeloyveFactory extends SaksmappeFactory {

    private final CaseProperties caseProperties;

    public SoknadDrosjeloyveFactory(FiksService fiksService, TitleService titleService, AdditionalFieldService additionalFieldService, CaseDefaults caseDefaults) {
        super(fiksService, titleService, additionalFieldService);
        caseProperties = caseDefaults.getSoknaddrosjeloyve();
    }

    public SoknadDrosjeloyveResource toFintResource(FaxShipment faxShipment) {
        SoknadDrosjeloyveResource soknadDrosjeloyveResource = new SoknadDrosjeloyveResource();
        updateSaksmappeFromFaxShipment(caseProperties, faxShipment, soknadDrosjeloyveResource);
        return soknadDrosjeloyveResource;

    }

}
