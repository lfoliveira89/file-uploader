package fileuploader.utils;

import fileuploader.exceptions.StorageException;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Created by luisoliveira on 11/13/17.
 */
public class MultipartFileUtilsTest {

    @Test(expected = StorageException.class)
    public void validateShouldThrowStorageExceptionWhenMultipartFileIsNull() {
        MultipartFileUtils.validate(null);
    }

    @Test(expected = StorageException.class)
    public void validateShouldThrowStorageExceptionWhenMultipartFileIsEmpty() {
        MockMultipartFile multipartFile = dummyMultipartFile("test1", "test1.png", "image/png", "".getBytes());
        MultipartFileUtils.validate(multipartFile);
    }

    @Test(expected = StorageException.class)
    public void validateShouldThrowStorageExceptionWhenMultipartFilenameMayRiskSystemSecurity() {
        MockMultipartFile multipartFile = dummyMultipartFile("test2", "..test2.png", "image/gif", "test2".getBytes());
        MultipartFileUtils.validate(multipartFile);
    }

    @Test
    public void validateShouldReturnSuccessfully() {
        MockMultipartFile multipartFile = dummyMultipartFile("test3", "test3.png", "image/jpeg", "test3".getBytes());
        MultipartFileUtils.validate(multipartFile);
    }

    private MockMultipartFile dummyMultipartFile(String name, String originalFilename, String contentType, byte[] bytes) {
        return new MockMultipartFile(name, originalFilename, contentType, bytes);
    }

}