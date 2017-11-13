package fileuploader.repositories;

import fileuploader.domain.UploadedFile;
import fileuploader.enums.FileUploadStatus;
import fileuploader.projection.UploadedFileInfo;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Created by luisoliveira on 11/12/17.
 */
@Repository
public interface UploadedFileRepository extends CrudRepository<UploadedFile, Long> {

    List<UploadedFileInfo> findAllByOrderByUserIdAscFilenameAsc();

    @Query("SELECT CASE WHEN COUNT(uf) > 0 THEN true ELSE false END FROM UploadedFile uf " +
            "WHERE uf.userId = :userId AND uf.filename = :filename")
    boolean existsByUserIdAndFilename(@Param("userId") String userId, @Param("filename") String filename);

    @Modifying
    @Query("UPDATE UploadedFile uf " +
            "SET uf.lastModifiedAt = :lastModifiedAt, uf.status = :status, uf.chunks = :chunks, uf.content = :content " +
            "WHERE uf.userId = :userId AND uf.filename = :filename")
    void updateByUserIdAndFilename(@Param("userId") String userId, @Param("filename") String filename,
                                   @Param("lastModifiedAt") Instant lastModifiedAt, @Param("status") FileUploadStatus status,
                                   @Param("chunks") Integer chunks, @Param("content") byte[] content);

    @Modifying
    @Query("UPDATE UploadedFile uf " +
            "SET uf.lastModifiedAt = :lastModifiedAt, uf.status = :status, uf.chunks = :chunks, uf.rootCause = :rootCause " +
            "WHERE uf.userId = :userId AND uf.filename = :filename")
    void updateByUserIdAndFilename(@Param("userId") String userId, @Param("filename") String filename,
                                   @Param("lastModifiedAt") Instant lastModifiedAt, @Param("status") FileUploadStatus status,
                                   @Param("chunks") Integer chunks, @Param("rootCause") String rootCause);

}
