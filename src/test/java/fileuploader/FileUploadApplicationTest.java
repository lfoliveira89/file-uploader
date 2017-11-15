package fileuploader;

import fileuploader.domain.UploadedFile;
import fileuploader.repositories.UploadedFileRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static fileuploader.enums.FileUploadStatus.COMPLETED;
import static fileuploader.utils.RestConstants.CONTENT_RANGE_HEADER;
import static fileuploader.utils.RestConstants.FILE_UPLOAD_URL_SERVICE;
import static fileuploader.utils.RestConstants.USER_ID_PARAM;
import static java.lang.Integer.valueOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = FileUploadApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {"upload.tmp.directory=fileuploader_test", "upload.max.chunk.size.bytes=1"})
public class FileUploadApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UploadedFileRepository repository;

    @Value("${upload.max.chunk.size.bytes}")
    private String maxChunkSize;

    @Before
    public void setup() throws IOException {
        repository.deleteAll();
    }

    @Test
    public void uploadShouldReturnSuccessfullyForChunkedTransfer() throws Exception {
        //given
        byte[] bytes = "1234".getBytes();
        Integer chunks = bytes.length / valueOf(maxChunkSize);
        String filename = "123456_test.pdf";

        //when
        for (int i = 0; i < chunks; i++) {
            int from = i * valueOf(maxChunkSize);
            int to = i * valueOf(maxChunkSize) + valueOf(maxChunkSize);

            byte[] chunkBytes = Arrays.copyOfRange(bytes, from, to);
            MockMultipartFile file = dummyMultipartFile(filename, chunkBytes);

            String contentRange = "bytes " + from + "-" + (to - 1) + "/" + chunks;

            mockMvc.perform(
                    fileUpload(FILE_UPLOAD_URL_SERVICE)
                            .file(file)
                            .header(CONTENT_RANGE_HEADER, contentRange)
                            .param(USER_ID_PARAM, "userId"))
                    .andExpect(status().isOk());

        }

        //then
        List<UploadedFile> result = new ArrayList<>();
        repository.findAll().forEach(result::add);

        assertNotNull(result);
        assertEquals(result.size(), 1);
        assertEquals(result.get(0).getFilename(), filename);
        assertEquals(result.get(0).getUserId(), "userId");
        assertEquals(result.get(0).getStatus(), COMPLETED);
        assertTrue(Arrays.equals(result.get(0).getContent(), bytes));
        assertEquals(result.get(0).getChunks().intValue(), chunks.intValue());

    }

    @Test
    public void uploadShouldReturnSuccessfullyForMultipartTransfer() throws Exception {
        //given
        byte[] bytes = "1234".getBytes();
        String filename = "123456_test.pdf";
        MockMultipartFile file = dummyMultipartFile(filename, bytes);

        //when
        mockMvc.perform(
                fileUpload(FILE_UPLOAD_URL_SERVICE)
                        .file(file)
                        .param(USER_ID_PARAM, "userId"))
                .andExpect(status().isOk());


        //then
        List<UploadedFile> result = new ArrayList<>();
        repository.findAll().forEach(result::add);

        assertNotNull(result);
        assertEquals(result.size(), 1);
        assertEquals(result.get(0).getFilename(), filename);
        assertEquals(result.get(0).getUserId(), "userId");
        assertEquals(result.get(0).getStatus(), COMPLETED);
        assertTrue(Arrays.equals(result.get(0).getContent(), bytes));
        assertEquals(result.get(0).getChunks().intValue(), 1);

    }

    @Test
    public void getUploadedFilesShouldReturnSuccessfully() throws Exception {
        //given
        UploadedFile uploadedFile = UploadedFile.builder()
                .userId("userId")
                .filename("123_test.pdf")
                .status(COMPLETED)
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now().plusMillis(1000))
                .chunks(1)
                .content("bytes".getBytes())
                .build();

        repository.save(uploadedFile);

        //when
        final ResultActions resultActions = mockMvc.perform(
                get(FILE_UPLOAD_URL_SERVICE))
                .andExpect(status().isOk());

        //then
        resultActions
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].userId").value("userId"))
                .andExpect(jsonPath("$[0].filename").value("test.pdf"))
                .andExpect(jsonPath("$[0].status").value("Completed"))
                .andExpect(jsonPath("$[0].uploadedTimeInMilliseconds").exists())
                .andExpect(jsonPath("$[0].chunks").value("1"))
                .andExpect(jsonPath("$[0].inputStream").doesNotExist())
                .andExpect(jsonPath("$[0].links.rel").value("uploadedFile"))
                .andExpect(jsonPath("$[0].links.href").exists());

    }

    private MockMultipartFile dummyMultipartFile(String filename, byte[] bytes) {
        return new MockMultipartFile("file", filename, "image/jpeg", bytes);
    }

}