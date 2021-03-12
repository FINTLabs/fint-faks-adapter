package no.fint.provider.fiks.service.fiks;

import lombok.extern.slf4j.Slf4j;
import no.fint.arkiv.fiks.svarut.*;
import no.fint.provider.fiks.SvarUtConfiguration;
import no.fint.provider.fiks.model.FaxShipment;
import no.fint.provider.fiks.model.ShipmentStatus;
import no.fint.provider.fiks.state.FaxShipmentState;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FiksService {

    @Autowired
    private FaxShipmentState faxShipmentState;

    @Autowired
    private SvarUtConfiguration svarUtConfiguration;

    @Autowired
    private FiksFactory fiksFactory;

    private ForsendelsesServiceV10 service;

    @PostConstruct
    private void init() {
        Map<String, Object> props = new HashMap<>();
        props.put("mtom-enabled", Boolean.TRUE);

        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(ForsendelsesServiceV10.class);
        factory.setAddress(svarUtConfiguration.getServiceUrl());
        factory.setUsername(svarUtConfiguration.getUsername());
        factory.setPassword(svarUtConfiguration.getPassword());
        factory.setProperties(props);
        factory.getFeatures().add(new WSAddressingFeature());
        service = (ForsendelsesServiceV10) factory.create();
        Client proxy = ClientProxy.getClient(service);
        HTTPConduit conduit = (HTTPConduit) proxy.getConduit();

        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
        httpClientPolicy.setConnectionTimeout(svarUtConfiguration.getConnectionTimeout());
        httpClientPolicy.setReceiveTimeout(svarUtConfiguration.getReceiveTimeout());
        conduit.setClient(httpClientPolicy);

    }

    public Boolean healthy() {
        List<String> shipmentTypes = service.retreiveForsendelsesTyper();
        log.info("Forsendelsestyper: {}", shipmentTypes);
        OrganisasjonsNummer org = new OrganisasjonsNummer();
        org.setId("911024403");
        List<MottakerForsendelsesTyper> forsendelsesTyper = service.retrieveMottakerSystemForOrgnr(org);
        log.info("Mottakersystemer: {}", forsendelsesTyper);
        return shipmentTypes.size() > 0;
    }

    public ForsendelsesStatus getForsendelsesStatus(String id) {
        ForsendelsesId forsendelsesId = new ForsendelsesId();
        forsendelsesId.setId(id);
        return service.retrieveForsendelsesStatus(forsendelsesId);
    }

    public void fax(FaxShipment fax) {
        log.debug("Fax: {}", fax);
        final ForsendelsesId forsendelsesId = service.startNyForsendelse();
        log.info("ForsendelsesId: {}", forsendelsesId.getId());
        try {
            Forsendelse forsendelse = fiksFactory.createForsendelse(fax, forsendelsesId.getId());
            log.debug("Forsendelse: {}", forsendelse);
            service.sendForsendelseMedId(forsendelse, forsendelsesId);
            log.info("ForsendelsesId: {}", forsendelsesId.getId());

            fax.getDocuments()
                    .stream()
                    .filter(document -> document.getSvarUtShipmentId().equals(forsendelsesId.getId()))
                    .forEach(doc -> {
                        doc.setShipmentStatus(ShipmentStatus.SENT_TO_SVARUT);
                        doc.setShipmentTime(new Date());
                    });
        } catch (Exception e) {
            log.warn("Crap!", e);
            fax.getDocuments()
                    .stream()
                    .filter(document -> document.getSvarUtShipmentId().equals(forsendelsesId.getId()))
                    .forEach(doc -> {
                        doc.setErrorMessage(e.toString());
                        doc.setErrorCount(doc.getErrorCount() + 1);
                    });

        } finally {
            faxShipmentState.save(fax);
        }
    }

    @Scheduled(initialDelayString = "${fint.faks.delay.initial:30000}", fixedDelayString = "${fint.faks.delay.fixed:30000}")
    public void sendFaxes() {
        List<FaxShipment> notSent = faxShipmentState.getNotSent();
        if (!notSent.isEmpty()) {
            log.info("The fax machine will shoot {} faxes into the galaxy... ", StringUtils.repeat("\uD83D\uDCE0", notSent.size()));
            notSent.forEach(this::fax);
            log.debug("The fax machine will enjoy a \uD83C\uDF7A waiting for the next deployment!");
        }
    }
}
