package no.fint.provider.fiks.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "fax")
public class CaseId {
    private final String orgId;
    private final int year;
    private final int caseNumber;
}
