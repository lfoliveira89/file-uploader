package fileuploader.services;

import fileuploader.controller.resources.DownloadableFileResource;
import fileuploader.controller.resources.UploadedFileResource;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

/**
 * Created by luisoliveira on 11/11/17.
 */
public interface StorageService {

    List<UploadedFileResource> findAll();

    DownloadableFileResource findById(Long id);

    void store(String userId, MultipartFile file, Integer totalChunks, boolean lastChunk, Instant uploadedTime);

}
