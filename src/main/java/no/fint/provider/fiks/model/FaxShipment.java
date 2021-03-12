package no.fint.provider.fiks.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@Document(collection = "fax")
public class FaxShipment {
    @Id
    public String id;
    private String applicationId;
    private String caseId;
    private Map<String, String> metadata;
    private String title;
    private String type;
    private String orgId;
    private Date createdDate;
    private String postamble;
    private List<FaxDocument> documents;
}
