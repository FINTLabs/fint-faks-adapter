package no.fint.provider.fiks.service.fint;

import no.fint.arkiv.AdditionalFieldService;
import no.fint.arkiv.CaseDefaults;
import no.fint.arkiv.CaseProperties;
import no.fint.arkiv.TitleService;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.resource.arkiv.kulturminnevern.TilskuddFartoyResource;
import no.fint.provider.fiks.model.FaxShipment;
import no.fint.provider.fiks.service.fiks.FiksService;
import org.springframework.stereotype.Service;

@Service
public class TilskuddFartoyFactory extends SaksmappeFactory {

    private final CaseProperties caseProperties;

    public TilskuddFartoyFactory(FiksService fiksService, TitleService titleService, AdditionalFieldService additionalFieldService, CaseDefaults caseDefaults) {
        super(fiksService, titleService, additionalFieldService);
        caseProperties = caseDefaults.getTilskuddfartoy();
    }

    public TilskuddFartoyResource toFintResource(FaxShipment faxShipment) {
        TilskuddFartoyResource tilskuddFartoyResource = new TilskuddFartoyResource();
        tilskuddFartoyResource.setSoknadsnummer(new Identifikator());

        updateSaksmappeFromFaxShipment(caseProperties, faxShipment, tilskuddFartoyResource);

        return tilskuddFartoyResource;

    }

}
