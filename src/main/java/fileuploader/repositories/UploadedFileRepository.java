package fileuploader.repositories;

import fileuploader.domain.UploadedFile;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by luisoliveira on 11/12/17.
 */
@Repository
public interface UploadedFileRepository extends CrudRepository<UploadedFile, Long> {

    // TODO projection: no need to retrieve content
    List<UploadedFile> findAllByOrderByUserIdAscFilenameAsc();

    UploadedFile findByUserIdAndFilename(String userId, String filename);

}
