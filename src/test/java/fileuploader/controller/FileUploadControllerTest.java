package fileuploader.controller;

import fileuploader.controller.resources.UploadedFileResource;
import fileuploader.exceptions.ResourceNotFoundException;
import fileuploader.services.StorageService;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static fileuploader.enums.FileUploadStatus.COMPLETED;
import static fileuploader.utils.RestConstants.CONTENT_RANGE_HEADER;
import static fileuploader.utils.RestConstants.FILE_UPLOAD_URL_SERVICE;
import static fileuploader.utils.RestConstants.ID_PATH_VARIABLE;
import static fileuploader.utils.RestConstants.USER_ID_PARAM;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by luisoliveira on 11/13/17.
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class FileUploadControllerTest {

    @Mock
    private StorageService service;

    @InjectMocks
    private FileUploadController controller;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new ControllerExceptionHandler())
                .build();
    }

    @Test
    public void getUploadedFilesShouldReturnSuccessfully() throws Exception {
        //given
        UploadedFileResource dummyUploadedFileResource = UploadedFileResource.builder()
                .id(1L)
                .userId("userId")
                .filename("test.pdf")
                .status(COMPLETED.getDescription())
                .build();
        when(service.findAll()).thenReturn(singletonList(dummyUploadedFileResource));

        //when
        final ResultActions resultActions = mockMvc.perform(
                get(FILE_UPLOAD_URL_SERVICE))
                .andExpect(status().isOk());

        //then
        resultActions
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].userId").value("userId"))
                .andExpect(jsonPath("$[0].filename").value("test.pdf"))
                .andExpect(jsonPath("$[0].status").value("Completed"))
                .andExpect(jsonPath("$[0].uploadedTimeInMilliseconds").doesNotExist())
                .andExpect(jsonPath("$[0].chunks").doesNotExist())
                .andExpect(jsonPath("$[0].inputStream").doesNotExist())
                .andExpect(jsonPath("$[0].links.rel").value("uploadedFile"))
                .andExpect(jsonPath("$[0].links.href").value("http://localhost/api/files/1"));

        verify(service).findAll();
        verifyNoMoreInteractions(service);
    }

    @Test
    public void getUploadedFileReturnHttpStatus422WhenIdParameterIsMissing() throws Exception {
        //when
        mockMvc.perform(
                get(FILE_UPLOAD_URL_SERVICE + ID_PATH_VARIABLE, "   "))
                .andExpect(status().isUnprocessableEntity());

        //then
        verifyNoMoreInteractions(service);
    }

    @Test
    public void getUploadedFileReturnHttpStatus404WhenResourceDoesNotExist() throws Exception {
        //given
        Long id = 1L;

        doThrow(ResourceNotFoundException.class).when(service).findById(id);

        //when
        mockMvc.perform(
                get(FILE_UPLOAD_URL_SERVICE + ID_PATH_VARIABLE, id))
                .andExpect(status().isNotFound());

        //then
        verify(service).findById(id);

        verifyNoMoreInteractions(service);
    }

    @Test
    public void getUploadedFileShouldReturnSuccessfully() throws Exception {
        //given
        Long id = 1L;

        UploadedFileResource dummyUploadedFileResource = UploadedFileResource.builder()
                .filename("test.pdf")
                .inputStream(IOUtils.toInputStream("test data", "UTF-8"))
                .build();

        when(service.findById(id)).thenReturn(dummyUploadedFileResource);

        //when
        MvcResult mvcResult = mockMvc.perform(
                get(FILE_UPLOAD_URL_SERVICE + ID_PATH_VARIABLE, id))
                .andExpect(status().isOk())
                .andReturn();

        //then
        assertEquals(mvcResult.getResponse().getHeader("Content-Disposition"), "attachment; filename=" + "test.pdf");

        verify(service).findById(id);
        verifyNoMoreInteractions(service);
    }

    @Test
    public void uploadReturnHttpStatus422WhenUserIdIsMissing() throws Exception {
        //when
        mockMvc.perform(
                fileUpload(FILE_UPLOAD_URL_SERVICE)
                        .file(dummyMultipartFile("test.pdf", "test".getBytes())))
                .andExpect(status().isUnprocessableEntity());

        //then
        verifyNoMoreInteractions(service);
    }

    @Test
    public void uploadReturnHttpStatus422WhenUserIdIsBlank() throws Exception {
        //when
        mockMvc.perform(
                fileUpload(FILE_UPLOAD_URL_SERVICE)
                        .file(dummyMultipartFile("test.pdf", "test".getBytes()))
                        .param(USER_ID_PARAM, "     "))
                .andExpect(status().isUnprocessableEntity());

        //then
        verifyNoMoreInteractions(service);
    }

    @Test
    public void uploadReturnHttpStatus422WhenContentRangIsNotValid() throws Exception {
        //when
        mockMvc.perform(
                fileUpload(FILE_UPLOAD_URL_SERVICE)
                        .file(dummyMultipartFile("test.pdf", "test".getBytes()))
                        .header(CONTENT_RANGE_HEADER, "bytes 1aaa-1000/10000")
                        .param(USER_ID_PARAM, "userId"))
                .andExpect(status().isUnprocessableEntity());

        //then
        verifyNoMoreInteractions(service);
    }

    @Test
    public void uploadReturnHttpStatus422WhenFileIsEmpty() throws Exception {
        //given
        MockMultipartFile file = dummyMultipartFile("test.pdf", "".getBytes());

        //when
        mockMvc.perform(
                fileUpload(FILE_UPLOAD_URL_SERVICE)
                        .file(file)
                        .param(USER_ID_PARAM, "userId"))
                .andExpect(status().isUnprocessableEntity());

        //then
        verifyNoMoreInteractions(service);
    }

    @Test
    public void uploadReturnHttpStatus422WhenFilenameMayRiskSystemSecurity() throws Exception {
        //given
        MockMultipartFile file = dummyMultipartFile("../test.pdf", "test".getBytes());

        //when
        mockMvc.perform(
                fileUpload(FILE_UPLOAD_URL_SERVICE)
                        .file(file)
                        .param(USER_ID_PARAM, "userId"))
                .andExpect(status().isUnprocessableEntity());

        //then
        verifyNoMoreInteractions(service);
    }

    @Test
    public void uploadReturnHttpStatus422WhenFileSizeIsGreaterThanMaxFileSizeForChunkedTransfer() throws Exception {
        //given
        ReflectionTestUtils.setField(controller, "maxFileSize", "1");

        MockMultipartFile file = dummyMultipartFile("test.pdf", "test".getBytes());

        //when
        mockMvc.perform(
                fileUpload(FILE_UPLOAD_URL_SERVICE)
                        .file(file)
                        .header(CONTENT_RANGE_HEADER, "bytes 0-1/4")
                        .param(USER_ID_PARAM, "userId"))
                .andExpect(status().isUnprocessableEntity());

        //then
        verifyNoMoreInteractions(service);
    }

    @Test
    public void uploadReturnHttpStatus422WhenChunkSizeIsGreaterThanMaxChunkSize() throws Exception {
        //given
        ReflectionTestUtils.setField(controller, "maxFileSize", "100");
        ReflectionTestUtils.setField(controller, "maxChunkSize", "5");

        MockMultipartFile file = dummyMultipartFile("test.pdf", "test".getBytes());

        //when
        mockMvc.perform(
                fileUpload(FILE_UPLOAD_URL_SERVICE)
                        .file(file)
                        .header(CONTENT_RANGE_HEADER, "bytes 0-9/100")
                        .param(USER_ID_PARAM, "userId"))
                .andExpect(status().isUnprocessableEntity());

        //then
        verifyNoMoreInteractions(service);
    }

    @Test
    public void uploadReturnHttpStatus422WhenFileSizeIsGreaterThanMaxFileSizeForMultipartTransfer() throws Exception {
        //given
        ReflectionTestUtils.setField(controller, "maxFileSize", "1");

        MockMultipartFile file = dummyMultipartFile("test.pdf", "test".getBytes());

        //when
        mockMvc.perform(
                fileUpload(FILE_UPLOAD_URL_SERVICE)
                        .file(file)
                        .param(USER_ID_PARAM, "userId"))
                .andExpect(status().isUnprocessableEntity());

        //then
        verifyNoMoreInteractions(service);
    }

    @Test
    public void uploadShouldReturnSuccessfullyForMultipartTransfer() throws Exception {
        //given
        ReflectionTestUtils.setField(controller, "maxFileSize", "100");

        MockMultipartFile file = dummyMultipartFile("test.pdf", "test".getBytes());

        doNothing().when(service).store(eq("userId"), eq(file), eq(null), eq(true), any(Instant.class));

        //when
        mockMvc.perform(
                fileUpload(FILE_UPLOAD_URL_SERVICE)
                        .file(file)
                        .param(USER_ID_PARAM, "userId"))
                .andExpect(status().isOk());

        //then
        verify(service).store(eq("userId"), eq(file), eq(null), eq(true), any(Instant.class));

        verifyNoMoreInteractions(service);
    }

    @Test
    public void uploadShouldReturnSuccessfullyForFirstChunkedTransfer() throws Exception {
        //given
        ReflectionTestUtils.setField(controller, "maxFileSize", "100");
        ReflectionTestUtils.setField(controller, "maxChunkSize", "5");

        MockMultipartFile file = dummyMultipartFile("test.pdf", "test".getBytes());

        doNothing().when(service).store(eq("userId"), eq(file), eq(null), eq(false), any(Instant.class));

        //when
        mockMvc.perform(
                fileUpload(FILE_UPLOAD_URL_SERVICE)
                        .file(file)
                        .header(CONTENT_RANGE_HEADER, "bytes 0-4/10")
                        .param(USER_ID_PARAM, "userId"))
                .andExpect(status().isOk());

        //then
        verify(service).store(eq("userId"), eq(file), eq(null), eq(false), any(Instant.class));

        verifyNoMoreInteractions(service);
    }

    @Test
    public void uploadShouldReturnSuccessfullyForFirstLastTransfer() throws Exception {
        //given
        ReflectionTestUtils.setField(controller, "maxFileSize", "100");
        ReflectionTestUtils.setField(controller, "maxChunkSize", "5");

        MockMultipartFile file = dummyMultipartFile("test.pdf", "test".getBytes());

        doNothing().when(service).store(eq("userId"), eq(file), eq(10), eq(true), any(Instant.class));

        //when
        mockMvc.perform(
                fileUpload(FILE_UPLOAD_URL_SERVICE)
                        .file(file)
                        .header(CONTENT_RANGE_HEADER, "bytes 45-49/50")
                        .param(USER_ID_PARAM, "userId"))
                .andExpect(status().isOk());

        //then
        verify(service).store(eq("userId"), eq(file), eq(10), eq(true), any(Instant.class));

        verifyNoMoreInteractions(service);
    }

    private MockMultipartFile dummyMultipartFile(String filename, byte[] bytes) {
        return new MockMultipartFile("file", filename, "image/jpeg", bytes);
    }

}