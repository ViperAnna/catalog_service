package ru.klimovich.catalog_service.util.imageUtils;

import lombok.experimental.UtilityClass;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;


@UtilityClass
public class ImageValidationUtils {

    public static boolean hasValidMimeType(MultipartFile file, Set<String> allowedTypes) {
        String mime = file.getContentType();
        if (mime != null && allowedTypes.contains(mime)) {
            return true;
        }

        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = new byte[3];
            if (inputStream.read(header) != 3) return false;
            return (header[0] & 0xFF) == 0xFF &&
                    (header[1] & 0xFF) == 0xD8 &&
                    (header[2] & 0xFF) == 0xFF;

        } catch (IOException e) {
            return false;
        }
    }
}
