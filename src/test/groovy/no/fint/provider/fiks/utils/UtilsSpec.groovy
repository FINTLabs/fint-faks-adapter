package no.fint.provider.fiks.utils

import no.fint.provider.fiks.model.FaxShipment
import spock.lang.Specification

import static Utils.createTitleString
import static no.fint.provider.fiks.utils.Utils.createFileName

class UtilsSpec extends Specification {

    def "Should return a title based on fax shipment"() {
        given:
        def shipment = new FaxShipment(title: "Test", type: "Tilskudd Fartøy", metadata: [test1: "test1", test2: "test2"])

        when:
        def titleString = createTitleString(shipment)

        then:
        titleString != null
        titleString.contains("Test")
        titleString.contains("test1")
        titleString.contains("test2")

    }

    def "Create filename"() {
        given:
        def shipment = new FaxShipment(title: "Test", type: "Tilskudd Fartøy", metadata: [test1: "test1", test2: "test2"])

        when:
        def fileName = createFileName(shipment, "application/pdf")

        then:
        fileName
        fileName.endsWith(".pdf")
        fileName.startsWith(shipment.type.replace(" ", "_"))
        fileName.contains("_")
    }
}
