package no.fint.provider.fiks.service.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import no.fint.arkiv.CaseDefaults;
import no.fint.event.model.Event;
import no.fint.event.model.Operation;
import no.fint.event.model.Problem;
import no.fint.event.model.ResponseStatus;
import no.fint.model.arkiv.samferdsel.SamferdselActions;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.arkiv.samferdsel.SoknadDrosjeloyveResource;
import no.fint.provider.fiks.service.fint.FaxCaseDefaultsService;
import no.fint.provider.fiks.service.fint.FaxQueryService;
import no.fint.provider.fiks.service.fint.SoknadDrosjeloyveFactory;
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
public class UpdateSoknadDrosjeloyveHandler implements Handler {
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
    private SoknadDrosjeloyveFactory soknadDrosjeloyveFactory;

    @Override
    public void accept(Event<FintLinks> response) {

        if (response.getData().size() != 1) {
            response.setResponseStatus(ResponseStatus.REJECTED);
            response.setStatusCode(HttpStatus.BAD_REQUEST.name());
            response.setMessage("Invalid request");
            return;
        }

        SoknadDrosjeloyveResource soknadDrosjeloyveResource = objectMapper.convertValue(response.getData().get(0), SoknadDrosjeloyveResource.class);

        if (response.getOperation() == Operation.CREATE) {
            createCase(response, soknadDrosjeloyveResource);

        } else if (response.getOperation() == Operation.UPDATE) {
            updateCase(response, response.getQuery(), soknadDrosjeloyveResource);

        } else {
            throw new IllegalArgumentException("Invalid operation: " + response.getOperation());
        }
    }

    private void updateCase(Event<FintLinks> response, String query, SoknadDrosjeloyveResource soknadDrosjeloyveResource) {
        if (!queryService.isValidQuery(query)) {
            throw new IllegalArgumentException("Invalid query: " + query);
        }

        if (Objects.isNull(soknadDrosjeloyveResource.getJournalpost()) || soknadDrosjeloyveResource.getJournalpost().isEmpty()) {
            throw new IllegalArgumentException("Update must contain at least one Journalpost");
        }

        caseDefaultsService.applyDefaultsForUpdate(caseDefaults.getSoknaddrosjeloyve(), soknadDrosjeloyveResource);

        log.trace("Complete document for update: {}", soknadDrosjeloyveResource);

        final List<Problem> problems = validationService.getProblems(soknadDrosjeloyveResource.getJournalpost());
        if (!problems.isEmpty()) {
            response.setResponseStatus(ResponseStatus.REJECTED);
            response.setMessage("Payload fails validation!");
            response.setStatusCode("VALIDATION_FAILURE");
            response.setProblems(problems);
            log.info("Validation problems!\n{}\n{}\n", soknadDrosjeloyveResource, problems);
            return;
        }

        final SoknadDrosjeloyveResource result = soknadDrosjeloyveFactory.toFintResource(faxShipmentState.updateFaxShipment(soknadDrosjeloyveResource, queryService.query(response.getOrgId(), query)));
        log.trace("Response: {}", result);

        response.setData(ImmutableList.of(result));
        response.setResponseStatus(ResponseStatus.ACCEPTED);
    }

    private void createCase(Event<FintLinks> response, SoknadDrosjeloyveResource soknadDrosjeloyveResource) {
        caseDefaultsService.applyDefaultsForCreation(caseDefaults.getSoknaddrosjeloyve(), soknadDrosjeloyveResource);
        log.trace("Complete document for creation: {}", soknadDrosjeloyveResource);

        final List<Problem> problems = validationService.getProblems(soknadDrosjeloyveResource);
        if (!problems.isEmpty()) {
            response.setResponseStatus(ResponseStatus.REJECTED);
            response.setStatusCode("VALIDATION_FAILURE");
            response.setMessage("Payload fails validation!");
            response.setProblems(problems);
            log.info("Validation problems!\n{}\n{}\n", soknadDrosjeloyveResource, problems);
            return;
        }

        final SoknadDrosjeloyveResource result = soknadDrosjeloyveFactory.toFintResource(faxShipmentState.createFaxShipment(caseDefaults.getSoknaddrosjeloyve(), soknadDrosjeloyveResource, soknadDrosjeloyveResource.getOrganisasjonsnummer()));
        log.trace("Response: {}", result);

        response.setData(ImmutableList.of(result));
        response.setResponseStatus(ResponseStatus.ACCEPTED);
    }

    @Override
    public Set<String> actions() {
        return Collections.singleton(SamferdselActions.UPDATE_SOKNADDROSJELOYVE.name());
    }
}
