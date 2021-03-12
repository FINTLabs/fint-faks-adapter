package no.fint.provider.fiks.utils;

import org.apache.commons.lang3.StringUtils;

public class UrlUtils {

    private UrlUtils() {
    }

    /*
    public static String getFilenameFromUri(String uri) {
        String[] uriParts = uri.split("/");
        return String.format("%s.json", uriParts[uriParts.length - 1]);
    }
     */

    public static String getFileIdFromUri(String uri) {
        return StringUtils.substringAfterLast(uri, "/");
    }
}
