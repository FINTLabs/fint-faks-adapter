package no.fint.provider.fiks.service.handlers;

import com.google.common.collect.ImmutableList;
import no.fint.event.model.Event;
import no.fint.event.model.ResponseStatus;
import no.fint.model.arkiv.samferdsel.SamferdselActions;
import no.fint.model.resource.FintLinks;
import no.fint.provider.fiks.exception.GetTilskuddFartoyNotFoundException;
import no.fint.provider.fiks.service.fint.FaxQueryService;
import no.fint.provider.fiks.service.fint.SoknadDrosjeloyveFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

@Service
public class GetSoknadDrosjeloyveHandler implements Handler {

    @Autowired
    private FaxQueryService queryService;

    @Autowired
    private SoknadDrosjeloyveFactory soknadDrosjeloyveFactory;

    @Override
    public void accept(Event<FintLinks> response) {
        String query = response.getQuery();
        if (!queryService.isValidQuery(query)) {
            response.setResponseStatus(ResponseStatus.REJECTED);
            response.setMessage("Invalid query: " + query);
            response.setStatusCode("INVALID_QUERY");
            return;
        }
        try {
            response.setData(ImmutableList.of(soknadDrosjeloyveFactory.toFintResource(queryService.query(response.getOrgId(), query))));
            response.setResponseStatus(ResponseStatus.ACCEPTED);
        } catch (GetTilskuddFartoyNotFoundException e) {
            response.setResponseStatus(ResponseStatus.REJECTED);
            response.setStatusCode("NOT_FOUND");
            response.setMessage(e.getMessage());
        }
    }

    @Override
    public Set<String> actions() {
        return Collections.singleton(SamferdselActions.GET_SOKNADDROSJELOYVE.name());
    }
}
