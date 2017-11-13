package fileuploader.utils;

import fileuploader.exceptions.StorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Created by luisoliveira on 11/12/17.
 */
@Slf4j
public abstract class MultipartFileUtils {

    private static final String EMPTY_MULTIPART_FILE_MSG = "Failed to store empty multipartFile: %s";
    private static final String SECURITY_CHECK_MSG = "Cannot store multipartFile with relative path outside current directory: %s";
    private static final String PREFIX = "[MultipartFileValidator.validate] ";

    private MultipartFileUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static void validate(MultipartFile multipartFile) {
        if (multipartFile == null) {
            throw new StorageException("multipartFile is required");
        }

        if (multipartFile.isEmpty()) {
            String err = EMPTY_MULTIPART_FILE_MSG;
            log.error(PREFIX + err);
            throw new StorageException(err);
        }

        String filename = StringUtils.cleanPath(multipartFile.getOriginalFilename());
        if (filename.contains("..")) {
            String err = String.format(SECURITY_CHECK_MSG, filename);
            log.error(PREFIX + err);
            throw new StorageException(err);
        }
    }

}
