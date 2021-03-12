package no.fint.provider.fiks.state;

import no.fint.provider.fiks.model.FaxShipment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface FaxShipmentRepository extends MongoRepository<FaxShipment, String> {
    FaxShipment findByOrgIdAndId(String orgId, String id);

    FaxShipment findByOrgIdAndApplicationId(String orgId, String applicationId);

    FaxShipment findByOrgIdAndCaseId(String orgId, String caseId);

    @Query("{'documents.shipmentStatus': ?0, orgId: ?1}")
    List<FaxShipment> findByOrgIdAndDocumentsShipmentStatus(String shipmentStatus, String orgId);

    List<FaxShipment> findAllByOrgId(String orgId);

}
