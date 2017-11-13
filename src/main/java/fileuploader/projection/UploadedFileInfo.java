package fileuploader.projection;

import fileuploader.enums.FileUploadStatus;

import java.time.Instant;

public interface UploadedFileInfo {

    Long getId();
    String getUserId();
    String getFilename();
    FileUploadStatus getStatus();
    Instant getCreatedAt();
    Instant getLastModifiedAt();
    Integer getChunks();

}
