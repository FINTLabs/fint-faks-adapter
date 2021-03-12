package no.fint.provider.fiks.state

import com.fasterxml.jackson.databind.ObjectMapper
import no.fint.arkiv.AdditionalFieldService
import no.fint.arkiv.CaseProperties
import no.fint.arkiv.TitleService
import no.fint.model.felles.kompleksedatatyper.Identifikator
import no.fint.model.resource.arkiv.kulturminnevern.TilskuddFartoyResource
import no.fint.model.resource.arkiv.noark.JournalpostResource
import no.fint.provider.fiks.SvarUtConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import spock.lang.Specification

import java.util.stream.Stream

@DataMongoTest
class FaxShipmentStateSpec extends Specification {

    @Autowired
    private FaxShipmentRepository faxShipmentRepository

    private FaxShipmentState state

    CaseProperties caseProperties

    void setup() {
        TitleService titleService = Mock {
            _ * getCaseTitle(_, _) >> "The Test Title"
        }
        AdditionalFieldService additionalFieldService = Mock {
            _ * getFieldsForResource(_, _) >> { Stream.empty() }
        }
        def caseIdFactory = Mock(CaseIdFactory) {
            _ * getCaseId(_, _) >> '2048/12'
        }
        caseProperties = Mock(CaseProperties)
        state = new FaxShipmentState(
                svarUtConfiguration: new SvarUtConfiguration(orgId: "test.no"),
                caseIdFactory: caseIdFactory,
                faxShipmentFactory: new FaxShipmentFactory(
                        objectMapper: new ObjectMapper(),
                        titleService: titleService,
                        additionalFieldService: additionalFieldService),
                faxShipmentRepository: faxShipmentRepository)

        state.createFaxShipment(caseProperties, new TilskuddFartoyResource(
                soknadsnummer: new Identifikator(identifikatorverdi: "12345"),
                journalpost: [new JournalpostResource(dokumentbeskrivelse: [])],
                tittel: "Test title",
                fartoyNavn: "Johanne Karine",
                kallesignal: "LKQD",
                kulturminneId: "1"
        ), '12345')
        state.createFaxShipment(caseProperties, new TilskuddFartoyResource(
                soknadsnummer: new Identifikator(identifikatorverdi: "56789"),
                journalpost: [new JournalpostResource(dokumentbeskrivelse: [])],
                tittel: "Test title",
                fartoyNavn: "Kaia",
                kallesignal: "LM2374",
                kulturminneId: "2"
        ), '56789')

    }

    def "Get all should return a list of 2 states"() {
        when:
        def size = state.getAll().size()

        then:
        size == 2
    }

    def "Add should increase state size by 1"() {
        given:
        def beforeSize = state.getAll().size()
        state.createFaxShipment(caseProperties, new TilskuddFartoyResource(
                soknadsnummer: new Identifikator(identifikatorverdi: "2222"),
                journalpost: [new JournalpostResource(dokumentbeskrivelse: [])],
                tittel: "Test title",
                fartoyNavn: "Båten",
                kallesignal: "LM000",
                kulturminneId: "3"
        ), '2222')


        when:
        def afterSize = state.getAll().size()
        then:
        afterSize - beforeSize == 1
    }

    def "Get by application id should return 1 state"() {
        when:
        def applicationId = state.getByApplicationId("test.no", "12345")

        then:
        applicationId

    }
}
