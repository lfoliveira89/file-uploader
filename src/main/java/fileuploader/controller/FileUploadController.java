package fileuploader.controller;

import fileuploader.controller.resources.UploadedFileResource;
import fileuploader.exceptions.StorageException;
import fileuploader.services.StorageService;
import fileuploader.utils.MultipartFileUtils;
import io.swagger.annotations.Api;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static java.lang.Long.valueOf;
import static java.lang.Math.ceil;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

/**
 * Created by luisoliveira on 11/11/17.
 */
@RestController
@RequestMapping("/files")
@Api(value = "/files", description = "provides endpoints for file upload management")
public class FileUploadController {

    private static final String PREFIX_REGEX = "[b][y][t][e][s][ ]";
    private static final String RANGE_SEPARATOR_REGEX = "[/]";
    private static final String RANGE_INNER_SEPARATOR_REGEX = "[-]";
    private static final String CONTENT_RANGE_REGEX = "^" + PREFIX_REGEX + "[0-9]+" +
            RANGE_INNER_SEPARATOR_REGEX + "[0-9]+" + RANGE_SEPARATOR_REGEX + "[0-9]+$";


    @Value("${upload.max.chunk.size.bytes}")
    private String maxChunkSize;

    @Autowired
    private StorageService storageService;

    @GetMapping
    public ResponseEntity<List<UploadedFileResource>> getUploadedFiles() {
        List<UploadedFileResource> uploadedFileResources = storageService.findAll();
        uploadedFileResources.forEach(
                file -> file.setLinks(linkTo(FileUploadController.class).slash(file.getId()).withRel("uploadedFile")));

        return ResponseEntity.ok(uploadedFileResources);
    }

    @GetMapping("/{id}")
    public void getUploadedFile(@PathVariable String id, HttpServletResponse response)
            throws MissingServletRequestParameterException, IOException {

        checkParams(id, "id");

        UploadedFileResource uploadedFileResource = storageService.findById(valueOf(id));
        response.setHeader("Content-Disposition", "attachment; filename=" + uploadedFileResource.getFilename());
        IOUtils.copy(uploadedFileResource.getInputStream(), response.getOutputStream());
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String> upload(@RequestParam(value = "userId") String userId,
                                         @RequestParam("file") MultipartFile file,
                                         @RequestHeader(value = "Content-Range", required = false) String contentRange)
            throws MissingServletRequestParameterException {

        Instant now = Instant.now();

        checkParams(userId, "userId");

        MultipartFileUtils.validate(file);

        Integer totalChunks = null;
        boolean lastChunk = true;
        // chunked transfer
        if (isChunkedRequest(contentRange)) {
            String[] chunks = contentRange.replaceAll(PREFIX_REGEX, "").split(RANGE_SEPARATOR_REGEX);
            Long totalBytes = valueOf(chunks[1]);

            String[] range = chunks[0].split(RANGE_INNER_SEPARATOR_REGEX);
            Long start = valueOf(range[0]);
            Long end = valueOf(range[1]);

            if ((end - start) > valueOf(maxChunkSize)) {
                throw new StorageException("chunk size not allowed");
            }

            lastChunk = totalBytes <= end + 1L;
            totalChunks = lastChunk ? (int) ceil((double) totalBytes / valueOf(maxChunkSize)) : null;
        }

        storageService.store(userId, file, totalChunks, lastChunk, now);

        return ResponseEntity.ok("{}");
    }

    private boolean isChunkedRequest(String contentRange) {
        if (isBlank(contentRange)) {
            return false;
        }

        if (!contentRange.matches(CONTENT_RANGE_REGEX)) {
            throw new StorageException("Content-Header not accepted");
        }

        return true;
    }

    private void checkParams(String value, String paramName) throws MissingServletRequestParameterException {
        if (isBlank(value)) {
            throw new MissingServletRequestParameterException(paramName, "string");
        }
    }

}
