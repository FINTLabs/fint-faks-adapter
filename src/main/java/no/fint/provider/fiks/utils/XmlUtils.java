package no.fint.provider.fiks.utils;

import org.springframework.stereotype.Service;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;
import java.util.GregorianCalendar;

@Service
public class XmlUtils {
    private final DatatypeFactory datatypeFactory;

    public XmlUtils() throws DatatypeConfigurationException {
        datatypeFactory = DatatypeFactory.newInstance();
    }

    public XMLGregorianCalendar xmlDate(Date input) {
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(input.getTime());
        return datatypeFactory.newXMLGregorianCalendar(gc);
    }

    public static Date javaDate(XMLGregorianCalendar input) {
        return input.toGregorianCalendar().getTime();
    }
}
