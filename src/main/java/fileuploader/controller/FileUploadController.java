package fileuploader.controller;

import fileuploader.controller.resources.UploadedFileResource;
import fileuploader.exceptions.UnprocessableEntityException;
import fileuploader.services.StorageService;
import fileuploader.utils.MultipartFileUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static fileuploader.utils.RestConstants.CONTENT_RANGE_HEADER;
import static fileuploader.utils.RestConstants.FILE_PARAM;
import static fileuploader.utils.RestConstants.FILE_UPLOAD_URL_SERVICE;
import static fileuploader.utils.RestConstants.ID_PARAM;
import static fileuploader.utils.RestConstants.ID_PATH_VARIABLE;
import static fileuploader.utils.RestConstants.USER_ID_PARAM;
import static java.lang.Long.valueOf;
import static java.lang.Math.ceil;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

/**
 * Created by luisoliveira on 11/11/17.
 */
@Slf4j
@RestController
@RequestMapping(FILE_UPLOAD_URL_SERVICE)
@Api(value = FILE_UPLOAD_URL_SERVICE, description = "provides endpoints for file upload management")
public class FileUploadController {

    private static final String CONTENT_RANGE_NOT_ALLOWED_ERROR = "Content-Range not allowed for %s";
    private static final String CHUNK_SIZE_NOT_ALLOWED_ERROR = "Chunk size not allowed. Max chunks size is %s bytes";

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
    @ApiOperation(value = "Returns uploaded files",
            notes = "Returns a complete list of uploaded files")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of uploaded files",
                    response = UploadedFileResource.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    public ResponseEntity<List<UploadedFileResource>> getUploadedFiles() {
        log.info("[FileUploadController.getUploadedFiles] retrieving all uploaded files");

        List<UploadedFileResource> uploadedFileResources = storageService.findAll();
        uploadedFileResources.forEach(
                file -> file.setLinks(linkTo(FileUploadController.class).slash(file.getId()).withRel("uploadedFile")));

        return ResponseEntity.ok(uploadedFileResources);
    }

    @GetMapping(ID_PATH_VARIABLE)
    @ApiOperation(value = "Returns an uploaded file",
            notes = "Returns an uploaded file by its id")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of uploaded file"),
            @ApiResponse(code = 404, message = "File not found"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    public void getUploadedFile(@PathVariable(value = ID_PARAM) String id, HttpServletResponse response)
            throws MissingServletRequestParameterException, IOException {
        log.info("[FileUploadController.getUploadedFile] retrieving uploaded file for {}", id);

        checkParams(id, ID_PARAM);

        UploadedFileResource uploadedFileResource = storageService.findById(valueOf(id));
        response.setHeader("Content-Disposition", "attachment; filename=" + uploadedFileResource.getFilename());
        IOUtils.copy(uploadedFileResource.getInputStream(), new BufferedOutputStream(response.getOutputStream()));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "Uploads a file",
            notes = "Uploads a file either via multipart or chunked transfer")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Uploaded file successfully", response = String.class),
            @ApiResponse(code = 422, message = "Unprocessable entity"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    public ResponseEntity<String> upload(@RequestParam(value = USER_ID_PARAM) String userId,
                                         @RequestParam(value = FILE_PARAM) MultipartFile file,
                                         @RequestHeader(value = CONTENT_RANGE_HEADER, required = false) String contentRange)
            throws MissingServletRequestParameterException {
        log.info("[FileUploadController.upload] uploading file {} for userId {}. Content-Rage: {}",
                file == null ? "" : file.getOriginalFilename(), userId, contentRange);

        Instant now = Instant.now();

        checkParams(userId, USER_ID_PARAM);

        MultipartFileUtils.validate(file);

        Integer totalChunks = null;
        boolean lastChunk = true;
        // chunked transfer
        if (isChunkedRequest(contentRange)) {
            String[] chunks = contentRange.replaceAll(PREFIX_REGEX, "").split(RANGE_SEPARATOR_REGEX);
            Long totalBytes = valueOf(chunks[1]);

            String[] range = chunks[0].split(RANGE_INNER_SEPARATOR_REGEX);
            Long start = valueOf(range[0]);
            Long end = valueOf(range[1]) + 1;

            if ((end - start) > valueOf(maxChunkSize)) {
                String err = format(CHUNK_SIZE_NOT_ALLOWED_ERROR, maxChunkSize);
                log.error("[FileUploadController.upload] " + err);
                throw new UnprocessableEntityException(err);
            }

            lastChunk = totalBytes <= end;
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
            String err = format(CONTENT_RANGE_NOT_ALLOWED_ERROR, contentRange);
            log.error("[FileUploadController.isChunkedRequest] " + err);
            throw new UnprocessableEntityException(err);
        }

        return true;
    }

    private void checkParams(String value, String paramName) throws MissingServletRequestParameterException {
        if (isBlank(value)) {
            log.error("[FileUploadController.checkParams] Required {} parameter '{}' is not present", "string", paramName);
            throw new MissingServletRequestParameterException(paramName, "string");
        }
    }

}
