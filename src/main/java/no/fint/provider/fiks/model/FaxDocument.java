package no.fint.provider.fiks.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Builder
@Document
public class FaxDocument {
    private String cacheFileId;
    private String fileUri;
    private ShipmentStatus shipmentStatus;
    private Date shipmentTime;
    private String svarUtShipmentId;
    private String errorMessage;
    private int errorCount;
}
