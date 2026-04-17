package no.novari.fint.provider.fiks.service.fint;

import no.novari.fint.arkiv.AdditionalFieldService;
import no.novari.fint.arkiv.CaseDefaults;
import no.novari.fint.arkiv.CaseProperties;
import no.novari.fint.arkiv.TitleService;
import no.novari.fint.model.resource.arkiv.samferdsel.SoknadDrosjeloyveResource;
import no.novari.fint.provider.fiks.model.FaxShipment;
import no.novari.fint.provider.fiks.service.fiks.FiksService;
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
