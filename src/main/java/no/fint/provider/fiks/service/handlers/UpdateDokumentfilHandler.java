package no.fint.provider.fiks.service.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.event.model.Event;
import no.fint.event.model.Operation;
import no.fint.event.model.ResponseStatus;
import no.fint.model.arkiv.noark.NoarkActions;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.arkiv.noark.DokumentfilResource;
import no.fint.provider.fiks.repository.InternalRepository;
import no.fint.provider.fiks.utils.FintUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class UpdateDokumentfilHandler implements Handler {
    @Autowired
    private InternalRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    private AtomicLong identifier =
            new AtomicLong(Long
                    .parseLong(DateTimeFormatter
                            .ofPattern("yyyyDDDHHmm'000'")
                            .format(LocalDateTime
                                    .now())));


    @Override
    public Set<String> actions() {
        return new HashSet<>(Collections.singletonList(NoarkActions.UPDATE_DOKUMENTFIL.name()));
    }

    @Override
    public void accept(Event<FintLinks> response) {

        if (response.getOperation() != Operation.CREATE || StringUtils.isNoneBlank(response.getQuery()) || response.getData().size() != 1) {
            response.setResponseStatus(ResponseStatus.REJECTED);
            response.setStatusCode("ILLEGAL_REQUEST");
            response.setMessage("Illegal request");
            return;
        }
        DokumentfilResource dokumentfilResource = objectMapper.convertValue(response.getData().get(0), DokumentfilResource.class);

        log.trace("Format: {}, data: {}...", dokumentfilResource.getFormat(), StringUtils.substring(dokumentfilResource.getData(), 0, 25));

        dokumentfilResource.setSystemId(FintUtils.createIdentifikator(String.format("I_%d", identifier.incrementAndGet())));
        response.getData().clear();
        try {
            repository.putFile(response, dokumentfilResource);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        response.addData(dokumentfilResource);
        response.setResponseStatus(ResponseStatus.ACCEPTED);

    }
}
