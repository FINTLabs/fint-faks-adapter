package no.fint.provider.fiks;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Data
@Component
@ConfigurationProperties("fint.fiks.svarut")
public class SvarUtConfiguration {
    private String
            serviceUrl = "https://svarut.ks.no/tjenester/forsendelseservice/ForsendelsesServiceV10",
            username,
            password,
            orgId;

    private int
            connectionTimeout = 120000,
            receiveTimeout = 120000,
            errorLimit = 5;

    private Organization organization;

    private String avgivendeSystem;
    private String konteringsKode;
    private boolean kunDigitalLevering = true;
    private boolean kryptert = false;
    private boolean krevNiva4Innlogging = false;
    private boolean metadata = false;

    private Path certificate;

    @Data
    public static class Organization {
        private String
                number,
                name,
                address1,
                address2,
                address3,
                postalCode,
                city;
    }
}
