package no.fint;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import no.fint.oauth.OAuthTokenProps;
import org.opensaml.xmlsec.signature.J;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class Config {

    static {
        StreamReadConstraints
                .overrideDefaultStreamReadConstraints(StreamReadConstraints.builder()
                        .maxStringLength(Integer.MAX_VALUE).build());
    }

    @ConditionalOnProperty(name = OAuthTokenProps.ENABLE_OAUTH,
            matchIfMissing = true, havingValue = "false")

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setDateFormat(new ISO8601DateFormat());

        return objectMapper;
    }

    @Bean
    @Qualifier("responseHandler")
    public Executor responseHander(@Value("${fint.adapter.response.workers:1}") int workers) {
        return Executors.newFixedThreadPool(workers);
    }

}
