package fileuploader.repositories;

import fileuploader.domain.UploadedFile;
import fileuploader.projection.UploadedFileInfo;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by luisoliveira on 11/12/17.
 */
@Repository
public interface UploadedFileRepository extends CrudRepository<UploadedFile, Long> {

    List<UploadedFileInfo> findAllByOrderByUserIdAscFilenameAsc();

    UploadedFile findByUserIdAndFilename(String userId, String filename);

}
