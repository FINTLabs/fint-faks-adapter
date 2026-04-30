package no.novari.fint.provider.fiks.state;

import lombok.extern.slf4j.Slf4j;
import no.novari.fint.provider.fiks.model.CaseId;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import static org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper.DEFAULT_TYPE_KEY;

@Service
@Slf4j
public class CaseIdFactory {
    private final MongoOperations mongoOperations;

    public CaseIdFactory(MongoTemplate mongoTemplate) {
        this.mongoOperations = mongoTemplate;
    }

    public String getCaseId(String orgId, int year) {
        Criteria criteria = new Criteria().andOperator(
                Criteria.where("orgId").is(orgId),
                Criteria.where("year").is(year),
                new Criteria().orOperator(
                        Criteria.where("_class").is("no.fint.provider.fiks.model.CaseId"),
                        Criteria.where("_class").is("no.novari.fint.provider.fiks.model.CaseId")
                )
        );

        Query query = new Query().addCriteria(criteria);

        Update update = new Update()
                .setOnInsert(DEFAULT_TYPE_KEY, CaseId.class.getName()).inc("caseNumber", 1);

        FindAndModifyOptions options = FindAndModifyOptions.options().upsert(true).returnNew(true);

        final CaseId caseId = mongoOperations.findAndModify(query, update, options, CaseId.class);
        log.debug("Arkivlandslaget proudly present an new fake caseId: {} ({})", caseId, orgId);
        return String.format("%d/%d", caseId.getYear(), caseId.getCaseNumber());
    }
}
