package fileuploader.enums;

import lombok.Getter;

/**
 * Created by luisoliveira on 11/11/17.
 */
@Getter
public enum FileUploadStatus {

    COMPLETED("Completed"),
    FAILED("Failed"),
    PENDING("Pending");

    FileUploadStatus(String description) {
        this.description = description;
    }

    private String description;

}
