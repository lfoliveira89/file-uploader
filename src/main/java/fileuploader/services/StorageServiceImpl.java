package fileuploader.services;

import fileuploader.controller.resources.UploadedFileResource;
import fileuploader.domain.UploadedFile;
import fileuploader.exceptions.ResourceNotFoundException;
import fileuploader.exceptions.StorageException;
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
        List<UploadedFile> uploadedFiles = repository.findAllByOrderByUserIdAscFilenameAsc();
        return uploadedFiles.stream()
                .map(this::apply)
                .collect(Collectors.toList());
    }

    private UploadedFileResource apply(UploadedFile uploadedFile) {
        return UploadedFileResource.builder()
                .id(uploadedFile.getId())
                .userId(uploadedFile.getUserId())
                .filename(uploadedFile.getFilename())
                .status(uploadedFile.getStatus().getDescription())
                .uploadedTimeInMilliseconds(getUploadedTimeInMilliseconds(uploadedFile.getCreatedAt(), uploadedFile.getLastModifiedAt()))
                .chunks(uploadedFile.getChunks())
                .build();
    }

    private Long getUploadedTimeInMilliseconds(Instant createdAt, Instant lastModifiedAt) {
        if (createdAt == null || lastModifiedAt == null) {
            return null;
        }

        return lastModifiedAt.toEpochMilli() - createdAt.toEpochMilli();
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
        UploadedFile uploadedFile = repository.findByUserIdAndFilename(userId, filename);
        try {
            Files.write(tempFile, file.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            if (lastChunk) {
                saveOrUpdateCompletedUploadedFile(userId, totalChunks, uploadedTime, filename, uploadedFile, tempFile);
                deleteTmpFileIfExists(tempFile);
            } else {
                savePendingUploadedFile(userId, uploadedTime, filename, uploadedFile);
            }
        } catch (Exception e) {
            String err = format(COULD_NOT_PROCESS_FILE_ERROR, userId, filename, e.getMessage());
            log.error("[StorageServiceImpl.store] " + err);

            saveOrUpdateFailedUploadedFile(userId, totalChunks, uploadedTime, filename, uploadedFile, err);
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

    private void saveOrUpdateCompletedUploadedFile(String userId, Integer totalChunks, Instant uploadedTime,
                                                   String filename, UploadedFile uploadedFile, Path tmpLocation)
            throws IOException {
        if (uploadedFile == null) {
            UploadedFile file = UploadedFile.builder()
                    .userId(userId)
                    .filename(filename)
                    .createdAt(uploadedTime)
                    .lastModifiedAt(Instant.now())
                    .status(COMPLETED)
                    .chunks(1)
                    .content(readAllBytes(tmpLocation))
                    .build();

            repository.save(file);
        } else {
            uploadedFile.setLastModifiedAt(uploadedTime);
            uploadedFile.setStatus(COMPLETED);
            uploadedFile.setChunks(totalChunks);
            uploadedFile.setContent(readAllBytes(tmpLocation));

            repository.save(uploadedFile);
        }
    }

    private void savePendingUploadedFile(String userId, Instant uploadedTime, String filename, UploadedFile uploadedFile) {
        if (uploadedFile == null) {
            UploadedFile file = UploadedFile.builder()
                    .userId(userId)
                    .filename(filename)
                    .createdAt(uploadedTime)
                    .status(PENDING)
                    .build();

            repository.save(file);
        }
    }

    private void saveOrUpdateFailedUploadedFile(String userId, Integer totalChunks, Instant uploadedTime,
                                                String filename, UploadedFile uploadedFile, String rootCause) {
        if (uploadedFile == null) {
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
            uploadedFile.setLastModifiedAt(uploadedTime);
            uploadedFile.setStatus(FAILED);
            uploadedFile.setChunks(totalChunks);
            uploadedFile.setRootCause(rootCause);

            repository.save(uploadedFile);
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
