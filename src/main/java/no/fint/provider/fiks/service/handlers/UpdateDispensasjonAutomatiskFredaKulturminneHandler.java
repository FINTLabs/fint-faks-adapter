package no.fint.provider.fiks.service.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import no.fint.arkiv.CaseDefaults;
import no.fint.event.model.Event;
import no.fint.event.model.Operation;
import no.fint.event.model.Problem;
import no.fint.event.model.ResponseStatus;
import no.fint.model.arkiv.kulturminnevern.KulturminnevernActions;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.arkiv.kulturminnevern.DispensasjonAutomatiskFredaKulturminneResource;
import no.fint.provider.fiks.service.fint.DispensasjonAutomatiskFredaKulturminneFactory;
import no.fint.provider.fiks.service.fint.FaxCaseDefaultsService;
import no.fint.provider.fiks.service.fint.FaxQueryService;
import no.fint.provider.fiks.service.fint.ValidationService;
import no.fint.provider.fiks.state.FaxShipmentState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Component
public class UpdateDispensasjonAutomatiskFredaKulturminneHandler implements Handler {
    @Autowired
    private FaxShipmentState faxShipmentState;

    @Autowired
    private FaxQueryService queryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private FaxCaseDefaultsService caseDefaultsService;

    @Autowired
    private CaseDefaults caseDefaults;

    @Autowired
    private DispensasjonAutomatiskFredaKulturminneFactory dispensasjonAutomatiskFredaKulturminneFactory;

    @Override
    public void accept(Event<FintLinks> response) {

        if (response.getData().size() != 1) {
            response.setResponseStatus(ResponseStatus.REJECTED);
            response.setStatusCode(HttpStatus.BAD_REQUEST.name());
            response.setMessage("Invalid request");

            return;
        }

        DispensasjonAutomatiskFredaKulturminneResource dispensasjonAutomatiskFredaKulturminneResource = objectMapper
                .convertValue(response.getData().get(0), DispensasjonAutomatiskFredaKulturminneResource.class);

        if (response.getOperation() == Operation.CREATE) {
            createCase(response, dispensasjonAutomatiskFredaKulturminneResource);

        } else if (response.getOperation() == Operation.UPDATE) {
            updateCase(response, response.getQuery(), dispensasjonAutomatiskFredaKulturminneResource);

        } else {
            throw new IllegalArgumentException("Invalid operation: " + response.getOperation());
        }
    }

    private void updateCase(Event<FintLinks> response, String query, DispensasjonAutomatiskFredaKulturminneResource dispensasjonAutomatiskFredaKulturminneResource) {
        if (!queryService.isValidQuery(query)) {
            throw new IllegalArgumentException("Invalid query: " + query);
        }

        if (Objects.isNull(dispensasjonAutomatiskFredaKulturminneResource.getJournalpost())
                || dispensasjonAutomatiskFredaKulturminneResource.getJournalpost().isEmpty()) {
            throw new IllegalArgumentException("Update must contain at least one Journalpost");
        }

        caseDefaultsService.applyDefaultsForUpdate(caseDefaults.getDispensasjonautomatiskfredakulturminne(),
                dispensasjonAutomatiskFredaKulturminneResource);

        log.trace("Complete document for update: {}", dispensasjonAutomatiskFredaKulturminneResource);

        final List<Problem> problems = validationService.getProblems(dispensasjonAutomatiskFredaKulturminneResource.getJournalpost());
        if (!problems.isEmpty()) {
            response.setResponseStatus(ResponseStatus.REJECTED);
            response.setMessage("Payload fails validation!");
            response.setStatusCode("VALIDATION_FAILURE");
            response.setProblems(problems);

            log.info("Validation problems!\n{}\n{}\n", dispensasjonAutomatiskFredaKulturminneResource, problems);
            return;
        }

        final DispensasjonAutomatiskFredaKulturminneResource result = dispensasjonAutomatiskFredaKulturminneFactory
                .toFintResource(faxShipmentState
                        .updateFaxShipment(dispensasjonAutomatiskFredaKulturminneResource, queryService.query(response.getOrgId(), query)));
        log.trace("Response: {}", result);

        response.setData(ImmutableList.of(result));
        response.setResponseStatus(ResponseStatus.ACCEPTED);
    }

    private void createCase(Event<FintLinks> response, DispensasjonAutomatiskFredaKulturminneResource dispensasjonAutomatiskFredaKulturminneResource) {
        caseDefaultsService.applyDefaultsForCreation(caseDefaults.getDispensasjonautomatiskfredakulturminne(), dispensasjonAutomatiskFredaKulturminneResource);
        log.trace("Complete document for creation: {}", dispensasjonAutomatiskFredaKulturminneResource);

        final List<Problem> problems = validationService.getProblems(dispensasjonAutomatiskFredaKulturminneResource);
        if (!problems.isEmpty()) {
            response.setResponseStatus(ResponseStatus.REJECTED);
            response.setStatusCode("VALIDATION_FAILURE");
            response.setMessage("Payload fails validation!");
            response.setProblems(problems);

            log.info("Validation problems!\n{}\n{}\n", dispensasjonAutomatiskFredaKulturminneResource, problems);
            return;
        }

        final DispensasjonAutomatiskFredaKulturminneResource result = dispensasjonAutomatiskFredaKulturminneFactory
                .toFintResource(faxShipmentState.createFaxShipment(caseDefaults.getDispensasjonautomatiskfredakulturminne(),
                        dispensasjonAutomatiskFredaKulturminneResource,
                        dispensasjonAutomatiskFredaKulturminneResource.getSoknadsnummer().getIdentifikatorverdi()));
        log.trace("Response: {}", result);

        response.setData(ImmutableList.of(result));
        response.setResponseStatus(ResponseStatus.ACCEPTED);
    }

    @Override
    public Set<String> actions() {
        return Collections.singleton(KulturminnevernActions.UPDATE_DISPENSASJONAUTOMATISKFREDAKULTURMINNE.name());
    }
}
