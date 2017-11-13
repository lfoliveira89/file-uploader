package fileuploader.controller;

import fileuploader.controller.resources.UploadedFileResource;
import fileuploader.services.StorageService;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static fileuploader.enums.FileUploadStatus.COMPLETED;
import static fileuploader.utils.RestConstants.FILE_UPLOAD_URL_SERVICE;
import static fileuploader.utils.RestConstants.ID_PATH_VARIABLE;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
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
        mockMvc.perform(
                get(FILE_UPLOAD_URL_SERVICE + ID_PATH_VARIABLE, "   "))
                .andExpect(status().isUnprocessableEntity());

        verifyNoMoreInteractions(service);
    }

    @Test
    public void getUploadedFileShouldReturnSuccessfully() throws Exception {
        //given
        UploadedFileResource dummyUploadedFileResource = UploadedFileResource.builder()
                .filename("test.pdf")
                .inputStream(IOUtils.toInputStream("test data", "UTF-8"))
                .build();

        when(service.findById(1L)).thenReturn(dummyUploadedFileResource);

        //when
        mockMvc.perform(
                get(FILE_UPLOAD_URL_SERVICE + ID_PATH_VARIABLE, "1"))
                .andExpect(status().isOk());

        //then
        verify(service).findById(1L);
        verifyNoMoreInteractions(service);
    }

}