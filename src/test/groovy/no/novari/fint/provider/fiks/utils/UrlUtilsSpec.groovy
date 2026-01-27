package no.novari.fint.provider.fiks.utils

import no.novari.fint.provider.fiks.utils.UrlUtils
import spock.lang.Specification

class UrlUtilsSpec extends Specification {

    def "Get file id from Dokumentfil URI"() {
        when:
        def fileId = UrlUtils.getFileIdFromUri("https://localhost/administrasjon/arkiv/dokumentfil/systemid/I_20192741413001")

        then:
        fileId == "I_20192741413001"
    }
}
