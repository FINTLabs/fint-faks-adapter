package no.fint.provider.fiks.service.handlers;

import com.google.common.collect.ImmutableList;
import no.fint.event.model.Event;
import no.fint.event.model.ResponseStatus;
import no.fint.model.arkiv.noark.NoarkActions;
import no.fint.model.resource.FintLinks;
import no.fint.provider.fiks.exception.GetSakNotFoundException;
import no.fint.provider.fiks.service.fint.FaxQueryService;
import no.fint.provider.fiks.service.fint.SakFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

@Service
public class GetSakHandler implements Handler {

    private final FaxQueryService queryService;
    private final SakFactory sakFactory;

    public GetSakHandler(FaxQueryService queryService, SakFactory sakFactory) {
        this.queryService = queryService;
        this.sakFactory = sakFactory;
    }

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
            response.setData(ImmutableList.of(sakFactory.toFintResource(queryService.query(response.getOrgId(), query))));
            response.setResponseStatus(ResponseStatus.ACCEPTED);
        } catch (GetSakNotFoundException e) {
            response.setResponseStatus(ResponseStatus.REJECTED);
            response.setStatusCode("NOT_FOUND");
            response.setMessage(e.getMessage());
        }
    }

    @Override
    public Set<String> actions() {
        return Collections.singleton(NoarkActions.GET_SAK.name());
    }
}
