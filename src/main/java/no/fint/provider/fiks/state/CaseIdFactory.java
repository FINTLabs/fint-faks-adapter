package no.fint.provider.fiks.state;

import no.fint.provider.fiks.model.CaseId;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import static org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper.DEFAULT_TYPE_KEY;

@Service
public class CaseIdFactory {
    private final MongoOperations mongoOperations;

    public CaseIdFactory(MongoTemplate mongoTemplate) {
        this.mongoOperations = mongoTemplate;
    }

    public String getCaseId(String orgId, int year) {
        Query query = new Query().restrict(CaseId.class).addCriteria(Criteria.where("orgId").is(orgId).and("year").is(year));
        Update update = new Update().setOnInsert(DEFAULT_TYPE_KEY, CaseId.class.getName()).inc("caseNumber", 1);
        FindAndModifyOptions options = FindAndModifyOptions.options().upsert(true).returnNew(true);
        final CaseId caseId = mongoOperations.findAndModify(query, update, options, CaseId.class);
        return String.format("%d/%d", caseId.getYear(), caseId.getCaseNumber());
    }
}
