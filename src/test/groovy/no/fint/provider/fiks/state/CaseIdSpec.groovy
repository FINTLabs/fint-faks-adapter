package no.fint.provider.fiks.state

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.core.MongoTemplate
import spock.lang.Specification

@DataMongoTest
class CaseIdSpec extends Specification{

    @Autowired
    MongoTemplate mongoTemplate

    def "First call creates case number 1"() {
        given:
        def caseIdFactory = new CaseIdFactory(mongoTemplate)

        when:
        def result = caseIdFactory.getCaseId('mock.no', 1876)

        then:
        result == '1876/1'
    }

    def "Two calls creates case number 2"() {
        given:
        def caseIdFactory = new CaseIdFactory(mongoTemplate)

        when:
        def result = caseIdFactory.getCaseId('fake.no', 1972)

        then:
        result == '1972/1'

        when:
        result = caseIdFactory.getCaseId('fake.no', 1972)

        then:
        result == '1972/2'

    }
}
