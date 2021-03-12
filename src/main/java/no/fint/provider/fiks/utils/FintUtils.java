package no.fint.provider.fiks.utils;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.resource.arkiv.noark.SaksmappeResource;

import java.util.Date;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
public enum FintUtils {
    ;

    public static Identifikator createIdentifikator(String value) {
        Identifikator identifikator = new Identifikator();
        identifikator.setIdentifikatorverdi(value);
        return identifikator;
    }

    public static Date getDate(SaksmappeResource saksmappe) {
        return Stream.of(
                saksmappe.getSaksdato(),
                saksmappe.getOpprettetDato())
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(new Date());
    }
}
