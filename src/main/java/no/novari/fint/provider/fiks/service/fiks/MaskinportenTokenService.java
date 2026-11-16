package no.novari.fint.provider.fiks.service.fiks;

import no.ks.fiks.maskinporten.*;
import no.novari.fint.provider.fiks.SvarUtConfiguration;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import org.springframework.util.Assert;

@Service
public class MaskinportenTokenService {
    private final SvarUtConfiguration configuration;
    private Maskinportenklient client;

    public MaskinportenTokenService(SvarUtConfiguration configuration) {
        this.configuration = configuration;
    }

    public synchronized String getAccessToken() {
        if (client == null) {
            var config = configuration.getMaskinporten();
            try {
                Assert.hasText(config.getClientId(), "Maskinporten client-id (iss) is required");
                Assert.hasText(config.getKeyId(), "Maskinporten key-id (kid) is required");
                String pem = config.getPrivateKey();
                Assert.hasText(pem, "Maskinporten private-key is required");
                pem = pem.strip();
                Assert.isTrue(pem.startsWith("-----BEGIN PRIVATE KEY-----")
                                && pem.endsWith("-----END PRIVATE KEY-----"),
                        "Maskinporten private-key must be an unencrypted PKCS8 PEM RSA key");
                String encoded = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                        .replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", "");
                var privateKey = KeyFactory.getInstance("RSA")
                        .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(encoded)));
                client = Maskinportenklient.builder()
                        .withPrivateKey(privateKey)
                        .withProperties(MaskinportenklientProperties.builder()
                                .audience(config.getAudience()).tokenEndpoint(config.getTokenEndpoint())
                                .issuer(config.getClientId()).numberOfSecondsLeftBeforeExpire(30).build())
                        .usingAsymmetricKey(config.getKeyId())
                        .build();
            } catch (Exception e) {
                throw new IllegalStateException("Unable to initialize Maskinporten client", e);
            }
        }
        return client.getAccessToken(AccessTokenRequest.builder().scope("ks:fiks").build());
    }
}
