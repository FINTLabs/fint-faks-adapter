package no.novari.fint.provider.fiks.service.fiks

import com.fasterxml.jackson.databind.ObjectMapper
import no.novari.fint.provider.fiks.SvarUtConfiguration
import no.novari.fint.provider.fiks.model.*
import no.novari.fint.provider.fiks.state.FaxShipmentState
import no.novari.fint.provider.fiks.repository.InternalRepository
import no.novari.fint.model.resource.arkiv.noark.DokumentfilResource
import org.springframework.http.*
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.RestTemplate
import spock.lang.Specification
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*
import static org.springframework.test.web.client.response.MockRestResponseCreators.*

class RestSvarUtSpec extends Specification {
    def config = new SvarUtConfiguration(enabled: true, kontoId: UUID.randomUUID().toString(),
            integrasjonId: 'integration', integrasjonPassord: 'secret',
            organization: new SvarUtConfiguration.Organization(number: '123456789', name: 'Test'))
    def repository = Mock(InternalRepository)
    def factory = new FiksFactory(config, Mock(EncryptionService), repository, new ObjectMapper())
    def state = Mock(FaxShipmentState)
    def tokens = Mock(MaskinportenTokenService) { getAccessToken() >> 'token' }
    def service = new FiksService(state, config, factory, tokens)
    def server = MockRestServiceServer.bindTo((RestTemplate) ReflectionTestUtils.getField(service, 'http')).build()
    def doc = FaxDocument.builder().cacheFileId('file').shipmentStatus(ShipmentStatus.READY).build()
    def fax = new FaxShipment(id: 'case', caseId: '2026/1', title: 'Test', metadata: [key: 'value'],
            createdDate: new Date(0), documents: [doc])

    def setup() {
        repository.getFile('file') >> new DokumentfilResource(filnavn: 'test.pdf', format: 'application/pdf',
                data: Base64.encoder.encodeToString('document'.bytes))
    }

    def 'sends multipart with auth and persists unconfirmed state before sending'() {
        given:
        def id = UUID.randomUUID().toString()
        def savedStates = []
        state.save(_) >> { FaxShipment value -> savedStates << value.documents[0].shipmentStatus; value }
        server.expect(requestTo(config.serviceUrl + '/v3/kontoer/' + config.kontoId + '/forsendelser/organisasjon'))
                .andExpect(method(HttpMethod.POST)).andExpect(header('Authorization', 'Bearer token'))
                .andExpect(header('IntegrasjonId', 'integration')).andExpect(header('IntegrasjonPassord', 'secret'))
                .andExpect { request ->
                    assert request.headers.getContentType().isCompatibleWith(MediaType.MULTIPART_FORM_DATA)
                    def body = request.bodyAsString
                    assert body.contains('filename="test.pdf"')
                    assert body.contains('"digitalId":"123456789"')
                    assert body.contains('"saksaar":2026')
                    assert !body.contains('"saksaar":0')
                    assert body.contains('document')
                }.andRespond(withSuccess('{"id":"' + id + '"}', MediaType.APPLICATION_JSON))
        when:
        service.fax(fax)
        then:
        savedStates == [ShipmentStatus.DELIVERY_UNCONFIRMED, ShipmentStatus.SENT_TO_SVARUT]
        doc.svarUtShipmentId == id
        doc.errorMessage == null
        server.verify()
    }

    def 'uncertain send is not automatically retried'() {
        given:
        server.expect(anything()).andRespond { throw new java.net.SocketTimeoutException('timeout') }
        when:
        service.fax(fax)
        service.fax(fax)
        then:
        doc.shipmentStatus == ShipmentStatus.DELIVERY_UNCONFIRMED
        doc.svarUtShipmentId == null
        server.verify()
    }

    def 'maps v3 status and offset timestamp'() {
        given:
        def id = UUID.randomUUID().toString()
        server.expect(anything()).andExpect(content().json('{"id":["' + id + '"]}'))
                .andRespond(withSuccess('{"statuser":[{"id":"' + id + '","status":"FEILET_UNDER_MOTTAK","sisteStatusEndring":"2026-01-01T12:00:00+01:00"}]}', MediaType.APPLICATION_JSON))
        when:
        def result = service.getForsendelsesStatus(id)
        then:
        result.status() == 'FEILET_UNDER_MOTTAK'
        result.sisteStatusEndring().toInstant().toString() == '2026-01-01T11:00:00Z'
        server.verify()
    }
    def 'Maskinporten service signs with PEM key and caches token without certificate'() {
        given:
        def calls = 0
        def assertions = []
        def httpServer = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress('127.0.0.1', 0), 0)
        def encode = { String value -> Base64.urlEncoder.withoutPadding().encodeToString(value.bytes) }
        def token = encode('{"alg":"RS256"}') + '.' + encode('{"exp":' + (System.currentTimeMillis().intdiv(1000) + 300) + '}') + '.AA'
        httpServer.createContext('/token') { exchange ->
            calls++
            def form = exchange.requestBody.getText('UTF-8')
            def assertion = form.split('&').find { it.startsWith('assertion=') }.substring('assertion='.length())
            assertions << com.nimbusds.jwt.SignedJWT.parse(java.net.URLDecoder.decode(assertion, 'UTF-8'))
            def response = ('{"access_token":"' + token + '","expires_in":300}').bytes
            exchange.sendResponseHeaders(200, response.length)
            exchange.responseBody.withCloseable { it.write(response) }
        }
        httpServer.start()
        def generator = java.security.KeyPairGenerator.getInstance('RSA')
        generator.initialize(2048)
        def pair = generator.generateKeyPair()
        config.maskinporten.clientId = 'client'
        config.maskinporten.keyId = 'test-key'
        config.maskinporten.audience = 'test'
        config.maskinporten.tokenEndpoint = 'http://127.0.0.1:' + httpServer.address.port + '/token'
        config.maskinporten.privateKey = '-----BEGIN PRIVATE KEY-----\n' +
                Base64.mimeEncoder.encodeToString(pair.private.encoded) + '\n-----END PRIVATE KEY-----'
        def client = new MaskinportenTokenService(config)
        expect:
        client.getAccessToken() == token
        client.getAccessToken() == token
        calls == 1
        assertions[0].header.keyID == 'test-key'
        assertions[0].header.X509CertChain == null
        assertions[0].JWTClaimsSet.issuer == 'client'
        assertions[0].JWTClaimsSet.getStringClaim('scope') == 'ks:fiks'
        assertions[0].verify(new com.nimbusds.jose.crypto.RSASSAVerifier((java.security.interfaces.RSAPublicKey) pair.public))
        !config.toString().contains('BEGIN PRIVATE KEY')
        cleanup:
        httpServer?.stop(0)
    }

    def 'invalid recipient is rejected before sending and stops after the retry limit'() {
        given:
        config.organization.number = 'invalid'
        config.errorLimit = 0
        when:
        service.fax(fax)
        then:
        doc.shipmentStatus == ShipmentStatus.ERROR
        doc.errorCount == 1
        server.verify()
    }

    def 'disabled sending does not change queued documents'() {
        given:
        config.enabled = false
        when:
        service.fax(fax)
        then:
        doc.shipmentStatus == ShipmentStatus.READY
        0 * state.save(_)
        server.verify()
    }

    def 'rejects missing or malformed private key without exposing it'() {
        given:
        config.maskinporten.clientId = 'client'
        config.maskinporten.keyId = 'key'
        config.maskinporten.privateKey = key
        when:
        new MaskinportenTokenService(config).getAccessToken()
        then:
        def error = thrown(IllegalStateException)
        !error.toString().contains('sensitive-value')
        !error.cause.toString().contains('sensitive-value')
        where:
        key << [null, 'sensitive-value', '-----BEGIN PRIVATE KEY-----\nsensitive-value\n-----END PRIVATE KEY-----']
    }

}
