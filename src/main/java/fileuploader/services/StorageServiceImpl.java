package fileuploader.services;

import fileuploader.controller.resources.UploadedFileResource;
import fileuploader.domain.UploadedFile;
import fileuploader.exceptions.ResourceNotFoundException;
import fileuploader.exceptions.StorageException;
import fileuploader.projection.UploadedFileInfo;
import fileuploader.repositories.UploadedFileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static fileuploader.enums.FileUploadStatus.COMPLETED;
import static fileuploader.enums.FileUploadStatus.FAILED;
import static fileuploader.enums.FileUploadStatus.PENDING;
import static java.lang.String.format;
import static java.nio.file.Files.readAllBytes;

/**
 * Created by luisoliveira on 11/11/17.
 */
@Slf4j
@Service
public class StorageServiceImpl implements StorageService {

    private static final String COULD_NOT_CREATE_TMP_DIR_ERROR = "Could not create temporary location at %s. Exception: %s";
    private static final String COULD_NOT_DELETE_TMP_FILE_ERROR = "Could not delete temporary file at %s. Exception: %s";
    private static final String COULD_NOT_PROCESS_FILE_ERROR = "Could not process given file: userId %s, filename %s. Exception: %s";
    private static final String ERROR_UPLOADED_FILE_NOT_FOUND_MSG = "Resource not found for id: %s";

    @Value("${upload.tmp.directory}")
    private String tmpDirectory;

    @Autowired
    private UploadedFileRepository repository;

    @Override
    public List<UploadedFileResource> findAll() {
        List<UploadedFileInfo> uploadedFiles = repository.findAllByOrderByUserIdAscFilenameAsc();
        return uploadedFiles.stream()
                .map(this::apply)
                .collect(Collectors.toList());
    }

    private UploadedFileResource apply(UploadedFileInfo uploadedFile) {
        return UploadedFileResource.builder()
                .id(uploadedFile.getId())
                .userId(uploadedFile.getUserId())
                .filename(uploadedFile.getFilename())
                .status(uploadedFile.getStatus().getDescription())
                .uploadedTimeInMilliseconds(getUploadedTimeInMilliseconds(uploadedFile))
                .chunks(uploadedFile.getChunks())
                .build();
    }

    private Long getUploadedTimeInMilliseconds(UploadedFileInfo uploadedFile) {
        if (uploadedFile == null || uploadedFile.getCreatedAt() == null || uploadedFile.getLastModifiedAt() == null) {
            return null;
        }

        return uploadedFile.getLastModifiedAt().toEpochMilli() - uploadedFile.getCreatedAt().toEpochMilli();
    }

    @Override
    public UploadedFileResource findById(Long id) {
        UploadedFile uploadedFile = repository.findOne(id);
        if (uploadedFile == null) {
            String err = format(ERROR_UPLOADED_FILE_NOT_FOUND_MSG, id);
            log.error("[StorageServiceImpl.findById] " + err);
            throw new ResourceNotFoundException(err);
        }

        return UploadedFileResource.builder()
                .filename(uploadedFile.getFilename())
                .inputStream(new ByteArrayInputStream(uploadedFile.getContent()))
                .build();
    }

    @Transactional
    @Override
    public void store(String userId, MultipartFile file, Integer totalChunks, boolean lastChunk, Instant uploadedTime) {
        String filename = StringUtils.cleanPath(file.getOriginalFilename());

        Path tempFile = getTempFile(userId, filename);

        boolean exists = repository.existsByUserIdAndFilename(userId, filename);
        try {
            Files.write(tempFile, file.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            if (lastChunk) {
                saveOrUpdateCompletedUploadedFile(exists, userId, totalChunks, uploadedTime, filename, tempFile);
                deleteTmpFileIfExists(tempFile);
            } else {
                savePendingUploadedFile(exists, userId, uploadedTime, filename);
            }
        } catch (Exception e) {
            String err = format(COULD_NOT_PROCESS_FILE_ERROR, userId, filename, e.getMessage());
            log.error("[StorageServiceImpl.store] " + err);

            saveOrUpdateFailedUploadedFile(exists, userId, totalChunks, uploadedTime, filename, err);
            deleteTmpFileIfExists(tempFile);

            throw new StorageException(err);
        }
    }

    private Path getTempFile(String userId, String filename) {
        String home = System.getProperty("user.home");

        Path path = Paths.get(home, tmpDirectory);
        if (!path.toFile().exists()) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                String err = format(COULD_NOT_CREATE_TMP_DIR_ERROR, tmpDirectory, e.getMessage());
                log.error("[StorageServiceImpl.getTmpLocation] " + err);
                throw new StorageException(err);
            }
        }

        return Paths.get(home, tmpDirectory, userId + "_" + filename);
    }

    private void saveOrUpdateCompletedUploadedFile(boolean exists, String userId, Integer totalChunks, Instant uploadedTime,
                                                   String filename, Path tmpLocation)
            throws IOException {
        if (!exists) {
            UploadedFile uploadedFile = UploadedFile.builder()
                    .userId(userId)
                    .filename(filename)
                    .createdAt(uploadedTime)
                    .lastModifiedAt(Instant.now())
                    .status(COMPLETED)
                    .chunks(1)
                    .content(readAllBytes(tmpLocation))
                    .build();

            repository.save(uploadedFile);
        } else {
            repository.updateByUserIdAndFilename(userId, filename, uploadedTime, COMPLETED,
                    totalChunks, readAllBytes(tmpLocation));
        }
    }

    private void savePendingUploadedFile(boolean exists, String userId, Instant uploadedTime, String filename) {
        if (!exists) {
            UploadedFile file = UploadedFile.builder()
                    .userId(userId)
                    .filename(filename)
                    .createdAt(uploadedTime)
                    .status(PENDING)
                    .build();

            repository.save(file);
        }
    }

    private void saveOrUpdateFailedUploadedFile(boolean exists, String userId, Integer totalChunks, Instant uploadedTime,
                                                String filename, String rootCause) {
        if (!exists) {
            UploadedFile file = UploadedFile.builder()
                    .userId(userId)
                    .filename(filename)
                    .createdAt(uploadedTime)
                    .lastModifiedAt(Instant.now())
                    .status(FAILED)
                    .chunks(1)
                    .rootCause(rootCause)
                    .build();

            repository.save(file);
        } else {
            repository.updateByUserIdAndFilename(userId, filename, uploadedTime, FAILED, totalChunks, rootCause);
        }
    }

    private void deleteTmpFileIfExists(Path tmpLocation) {
        try {
            Files.deleteIfExists(tmpLocation);
        } catch (IOException e) {
            String err = format(COULD_NOT_DELETE_TMP_FILE_ERROR, tmpLocation, e.getMessage());
            log.error("[StorageServiceImpl.deleteTmpFileIfExists] " + err);
            throw new StorageException(err);
        }
    }

}
