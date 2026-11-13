package no.novari.fint

import no.novari.fint.provider.adapter.sse.SseInitializer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.actuate.metrics.AutoConfigureMetrics
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [
        'fint.adapter.organizations=fintlabs.no',
        'fint.internal-files.type=FILE',
        'fint.fiks.svarut.service-url=http://127.0.0.1:1/svarut',
        'fint.faks.delay.initial=3600000'
])
@AutoConfigureMetrics
@ActiveProfiles('test')
class ApplicationSpec extends Specification {
    @MockBean
    SseInitializer sseInitializer

    @Autowired
    TestRestTemplate http

    def 'Application starts with REST and FINT OAuth and exposes existing monitoring paths'() {
        expect:
        http.getForEntity('/health', Map).body.status == 'UP'
        http.getForEntity('/prometheus', String).statusCodeValue == 200
        http.getForEntity('/metrics', Map).statusCodeValue == 200
    }
}
