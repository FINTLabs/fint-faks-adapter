package no.fint.provider.fiks.controller;

import lombok.extern.slf4j.Slf4j;
import no.fint.provider.fiks.model.FaxShipment;
import no.fint.provider.fiks.state.FaxShipmentState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class FaxShipmentController {

    @Autowired
    private FaxShipmentState faxShipmentState;

    @GetMapping("/shipments")
    public ResponseEntity<List<FaxShipment>> getShipments() {
        return ResponseEntity.ok(faxShipmentState.getAll());
    }
}
