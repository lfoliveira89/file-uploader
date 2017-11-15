package fileuploader.domain;

import fileuploader.enums.FileUploadStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.time.Instant;

/**
 * Created by luisoliveira on 11/12/17.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = UploadedFile.TABLE_NAME)
public class UploadedFile {

    public static final String TABLE_NAME = "uploadedFile";

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO, generator="uploadedFileNative")
    @GenericGenerator(name = "uploadedFileNative", strategy = "native")
    protected Long id;

    @Column(name = "user_id", nullable = false)
    protected String userId;

    @Column(name = "filename", nullable = false)
    protected String filename;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "status", nullable = false)
    protected FileUploadStatus status;

    @Column(name = "created_at", nullable = false)
    protected Instant createdAt;

    @Column(name = "last_modified_at")
    protected Instant lastModifiedAt;

    @Column(name = "chunks")
    protected Integer chunks;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "content")
    protected byte[] content;

    @Column(name = "root_cause")
    protected String rootCause;

}
