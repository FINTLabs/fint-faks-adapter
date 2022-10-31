package no.fint.provider.fiks.service.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import no.fint.arkiv.CaseProperties;
import no.fint.event.model.Event;
import no.fint.event.model.Operation;
import no.fint.event.model.Problem;
import no.fint.event.model.ResponseStatus;
import no.fint.model.arkiv.noark.NoarkActions;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.arkiv.noark.SakResource;
import no.fint.provider.fiks.service.fint.FaxCaseDefaultsService;
import no.fint.provider.fiks.service.fint.FaxQueryService;
import no.fint.provider.fiks.service.fint.SakFactory;
import no.fint.provider.fiks.service.fint.ValidationService;
import no.fint.provider.fiks.state.FaxShipmentState;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Component
public class UpdateSakHandler implements Handler {
    private final FaxShipmentState faxShipmentState;
    private final FaxQueryService queryService;
    private final ObjectMapper objectMapper;
    private final ValidationService validationService;
    private final FaxCaseDefaultsService caseDefaultsService;
    private final SakFactory sakFactory;

    public UpdateSakHandler(FaxShipmentState faxShipmentState,
                            FaxQueryService queryService,
                            ObjectMapper objectMapper,
                            ValidationService validationService,
                            FaxCaseDefaultsService caseDefaultsService,
                            SakFactory sakFactory) {
        this.faxShipmentState = faxShipmentState;
        this.queryService = queryService;
        this.objectMapper = objectMapper;
        this.validationService = validationService;
        this.caseDefaultsService = caseDefaultsService;
        this.sakFactory = sakFactory;
    }

    @Override
    public void accept(Event<FintLinks> response) {

        if (response.getData().size() != 1) {
            response.setResponseStatus(ResponseStatus.REJECTED);
            response.setStatusCode(HttpStatus.BAD_REQUEST.name());
            response.setMessage("Invalid request");
            return;
        }

        SakResource sakResource = objectMapper.convertValue(response.getData().get(0), SakResource.class);

        if (response.getOperation() == Operation.CREATE) {
            createCase(response, sakResource);

        } else if (response.getOperation() == Operation.UPDATE) {
            updateCase(response, response.getQuery(), sakResource);

        } else {
            throw new IllegalArgumentException("Invalid operation: " + response.getOperation());
        }
    }

    private void updateCase(Event<FintLinks> response, String query, SakResource sakResource) {
        if (!queryService.isValidQuery(query)) {
            throw new IllegalArgumentException("Invalid query: " + query);
        }

        if (Objects.isNull(sakResource.getJournalpost()) || sakResource.getJournalpost().isEmpty()) {
            throw new IllegalArgumentException("Update must contain at least one Journalpost");
        }

        caseDefaultsService.applyDefaultsForUpdate(new CaseProperties(), sakResource);

        log.trace("Complete document for update: {}", sakResource);

        final List<Problem> problems = validationService.getProblems(sakResource.getJournalpost());
        if (!problems.isEmpty()) {
            response.setResponseStatus(ResponseStatus.REJECTED);
            response.setMessage("Payload fails validation!");
            response.setStatusCode("VALIDATION_FAILURE");
            response.setProblems(problems);
            log.info("Validation problems!\n{}\n{}\n", sakResource, problems);
            return;
        }

        final SakResource result = sakFactory.toFintResource(faxShipmentState.updateFaxShipment(sakResource,
                queryService.query(response.getOrgId(), query)));
        log.trace("Response: {}", result);

        response.setData(ImmutableList.of(result));
        response.setResponseStatus(ResponseStatus.ACCEPTED);
    }

    private void createCase(Event<FintLinks> response, SakResource sakResource) {
        caseDefaultsService.applyDefaultsForCreation(new CaseProperties(), sakResource);
        log.trace("Complete document for creation: {}", sakResource);

        final List<Problem> problems = validationService.getProblems(sakResource);
        if (!problems.isEmpty()) {
            response.setResponseStatus(ResponseStatus.REJECTED);
            response.setStatusCode("VALIDATION_FAILURE");
            response.setMessage("Payload fails validation!");
            response.setProblems(problems);
            log.info("Validation problems!\n{}\n{}\n", sakResource, problems);
            return;
        }

        final SakResource result = sakFactory.toFintResource(faxShipmentState.createFaxShipment(new CaseProperties(),
                sakResource,
                null));
        log.trace("Response: {}", result);

        response.setData(ImmutableList.of(result));
        response.setResponseStatus(ResponseStatus.ACCEPTED);
    }

    @Override
    public Set<String> actions() {
        return Collections.singleton(NoarkActions.UPDATE_SAK.name());
    }
}
