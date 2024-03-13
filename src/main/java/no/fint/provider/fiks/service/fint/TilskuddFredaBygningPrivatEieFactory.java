package no.fint.provider.fiks.service.fint;

import lombok.extern.slf4j.Slf4j;
import no.fint.arkiv.AdditionalFieldService;
import no.fint.arkiv.CaseDefaults;
import no.fint.arkiv.CaseProperties;
import no.fint.arkiv.TitleService;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.resource.arkiv.kulturminnevern.TilskuddFredaBygningPrivatEieResource;
import no.fint.model.resource.felles.kompleksedatatyper.MatrikkelnummerResource;
import no.fint.provider.fiks.model.FaxShipment;
import no.fint.provider.fiks.service.fiks.FiksService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TilskuddFredaBygningPrivatEieFactory extends SaksmappeFactory {

    private final CaseProperties caseProperties;

    public TilskuddFredaBygningPrivatEieFactory(FiksService fiksService, TitleService titleService, AdditionalFieldService additionalFieldService, CaseDefaults caseDefaults) {
        super(fiksService, titleService, additionalFieldService);
        caseProperties = caseDefaults.getTilskuddfredabygningprivateie();
    }

    public TilskuddFredaBygningPrivatEieResource toFintResource(FaxShipment faxShipment) {
        TilskuddFredaBygningPrivatEieResource resource = new TilskuddFredaBygningPrivatEieResource();
        resource.setSoknadsnummer(new Identifikator());
        resource.setMatrikkelnummer(new MatrikkelnummerResource());

        log.debug("Let's try to make som FINT goodies, based on this fax shipment: {}", faxShipment);

        updateSaksmappeFromFaxShipment(caseProperties, faxShipment, resource);

        return resource;

    }

}
