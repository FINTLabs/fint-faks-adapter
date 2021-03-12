package no.fint.provider.fiks.service.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import lombok.extern.slf4j.Slf4j;
import no.fint.event.model.Event;
import no.fint.event.model.ResponseStatus;
import no.fint.model.arkiv.kodeverk.KodeverkActions;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.arkiv.kodeverk.DokumentStatusResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class GetAllDokumentStatusHandler implements Handler {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void accept(Event<FintLinks> response) {
        Resource resource = new ClassPathResource("kodeverk/dokumentstatus.json");
        CollectionType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, DokumentStatusResource.class);

        try {
            List<DokumentStatusResource> dokumentStatusList =
                    objectMapper.readValue(resource.getInputStream(), collectionType);
            dokumentStatusList.forEach(response::addData);
            response.setResponseStatus(ResponseStatus.ACCEPTED);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public Set<String> actions() {
        return Collections.singleton(KodeverkActions.GET_ALL_DOKUMENTSTATUS.name());
    }
}
