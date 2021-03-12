package no.fint.provider.fiks.utils;

import no.fint.provider.fiks.model.FaxShipment;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {

    private Utils() {
    }

    public static String createTitleString(FaxShipment faxShipment) {
        StringBuilder stringBuilder = new StringBuilder(faxShipment.getType() + " - " + faxShipment.getTitle());
        faxShipment.getMetadata().forEach((k, v) -> stringBuilder.append(" - ").append(v));
        return stringBuilder.toString();
    }

    public static String createFileName(FaxShipment faxShipment, String type) throws MimeTypeException {
        MimeTypes defaultMimeTypes = MimeTypes.getDefaultMimeTypes();
        MimeType mimeType = defaultMimeTypes.forName(type);

        StringBuilder stringBuilder = new StringBuilder(faxShipment.getType().replace(" ", "_"));
        faxShipment.getMetadata().forEach((k, v) -> stringBuilder.append("_").append(v));

        String pattern = "dd-MM-yyyy-HH-mm-ss";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        String date = simpleDateFormat.format(new Date());
        return stringBuilder.append("_").append(date).append(mimeType.getExtension()).toString();

    }


}
