package no.fint.provider.fiks.service.fint;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import no.fint.provider.fiks.model.FaxShipment;
import no.fint.provider.fiks.state.FaxShipmentState;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.function.BiFunction;

@Service
@Slf4j
public class FaxQueryService {
    private final ImmutableMap<String, BiFunction<String, String, FaxShipment>> queryMap;
    private final String[] validQueries;

    public FaxQueryService(FaxShipmentState faxShipmentState) {
        queryMap = new ImmutableMap.Builder<String, BiFunction<String, String, FaxShipment>>()
                .put("soknadsnummer/", faxShipmentState::getByApplicationId)
                .put("mappeid/", faxShipmentState::getByCaseId)
                .put("systemid/", faxShipmentState::getById)
                .build();
        validQueries = queryMap.keySet().toArray(new String[0]);
    }

    public boolean isValidQuery(String query) {
        return StringUtils.startsWithAny(StringUtils.lowerCase(query), validQueries);
    }

    public FaxShipment query(String orgId, String query) {
        for (String prefix : queryMap.keySet()) {
            if (StringUtils.startsWithIgnoreCase(query, prefix)) {
                return queryMap.get(prefix).apply(orgId, StringUtils.removeStartIgnoreCase(query, prefix));
            }
        }
        throw new IllegalArgumentException("Invalid query: " + query);
    }
}
