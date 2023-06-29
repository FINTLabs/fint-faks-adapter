package no.fint.provider.fiks.service.fint;

import no.fint.arkiv.AdditionalFieldService;
import no.fint.arkiv.CaseDefaults;
import no.fint.arkiv.CaseProperties;
import no.fint.arkiv.TitleService;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.resource.arkiv.kulturminnevern.DispensasjonAutomatiskFredaKulturminneResource;
import no.fint.model.resource.felles.kompleksedatatyper.MatrikkelnummerResource;
import no.fint.provider.fiks.model.FaxShipment;
import no.fint.provider.fiks.service.fiks.FiksService;
import org.springframework.stereotype.Service;

@Service
public class DispensasjonAutomatiskFredaKulturminneFactory extends SaksmappeFactory {

    private final CaseProperties caseProperties;

    public DispensasjonAutomatiskFredaKulturminneFactory(FiksService fiksService,
                                                         TitleService titleService,
                                                         AdditionalFieldService additionalFieldService,
                                                         CaseDefaults caseDefaults) {
        super(fiksService, titleService, additionalFieldService);
        caseProperties = caseDefaults.getDispensasjonautomatiskfredakulturminne();
    }

    public DispensasjonAutomatiskFredaKulturminneResource toFintResource(FaxShipment faxShipment) {
        DispensasjonAutomatiskFredaKulturminneResource resource = new DispensasjonAutomatiskFredaKulturminneResource();
        resource.setSoknadsnummer(new Identifikator());
        resource.setMatrikkelnummer(new MatrikkelnummerResource());

        updateSaksmappeFromFaxShipment(caseProperties, faxShipment, resource);

        return resource;
    }

}
