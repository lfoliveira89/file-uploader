package fileuploader.services;

import fileuploader.controller.resources.UploadedFileResource;
import fileuploader.domain.UploadedFile;
import fileuploader.exceptions.ResourceNotFoundException;
import fileuploader.exceptions.StorageException;
import fileuploader.projection.UploadedFileInfo;
import fileuploader.repositories.UploadedFileRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static fileuploader.enums.FileUploadStatus.COMPLETED;
import static fileuploader.enums.FileUploadStatus.FAILED;
import static fileuploader.enums.FileUploadStatus.PENDING;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class StorageServiceImplTest {

    @Mock
    private UploadedFileRepository repository;

    @InjectMocks
    private StorageServiceImpl service;

    @Before
    public void setup() {
        initMocks(this);
        ReflectionTestUtils.setField(service, "tmpDirectory", "fileUploaderTest");
    }

    @Test
    public void findAllShouldReturnSuccessfully() {
        //given
        UploadedFileInfo uploadedFileInfo = Mockito.mock(UploadedFileInfo.class);
        when(uploadedFileInfo.getId()).thenReturn(1L);
        when(uploadedFileInfo.getUserId()).thenReturn("userId");
        when(uploadedFileInfo.getFilename()).thenReturn("test.pdf");
        when(uploadedFileInfo.getStatus()).thenReturn(COMPLETED);
        Instant createdAt = Instant.now();
        when(uploadedFileInfo.getCreatedAt()).thenReturn(createdAt);
        Instant lastModifiedAt = Instant.now().plusMillis(1532);
        when(uploadedFileInfo.getLastModifiedAt()).thenReturn(lastModifiedAt);
        when(uploadedFileInfo.getChunks()).thenReturn(5);

        when(repository.findAllByOrderByUserIdAscFilenameAsc()).thenReturn(singletonList(uploadedFileInfo));

        //when
        List<UploadedFileResource> result = service.findAll();

        //then
        assertNotNull(result);
        assertEquals(result.get(0).getId().longValue(), 1L);
        assertEquals(result.get(0).getUserId(), "userId");
        assertEquals(result.get(0).getFilename(), "test.pdf");
        assertEquals(result.get(0).getStatus(), COMPLETED.getDescription());
        assertEquals(result.get(0).getUploadedTimeInMilliseconds().longValue(), lastModifiedAt.toEpochMilli() - createdAt.toEpochMilli());
        assertEquals(result.get(0).getChunks().intValue(), 5);
        assertNull(result.get(0).getInputStream());

        verify(repository).findAllByOrderByUserIdAscFilenameAsc();
        verifyNoMoreInteractions(repository);
    }

    @Test
    public void findAllShouldReturnSuccessfullyWhenLastModifiedAtIsNull() {
        //given
        UploadedFileInfo uploadedFileInfo = Mockito.mock(UploadedFileInfo.class);
        when(uploadedFileInfo.getId()).thenReturn(2L);
        when(uploadedFileInfo.getUserId()).thenReturn("userId");
        when(uploadedFileInfo.getFilename()).thenReturn("test.pdf");
        when(uploadedFileInfo.getStatus()).thenReturn(PENDING);
        Instant createdAt = Instant.now();
        when(uploadedFileInfo.getCreatedAt()).thenReturn(createdAt);
        when(uploadedFileInfo.getLastModifiedAt()).thenReturn(null);
        when(uploadedFileInfo.getChunks()).thenReturn(1);

        when(repository.findAllByOrderByUserIdAscFilenameAsc()).thenReturn(singletonList(uploadedFileInfo));

        //when
        List<UploadedFileResource> result = service.findAll();

        //then
        assertNotNull(result);
        assertEquals(result.get(0).getId().longValue(), 2L);
        assertEquals(result.get(0).getUserId(), "userId");
        assertEquals(result.get(0).getFilename(), "test.pdf");
        assertEquals(result.get(0).getStatus(), PENDING.getDescription());
        assertNull(result.get(0).getUploadedTimeInMilliseconds());
        assertEquals(result.get(0).getChunks().intValue(), 1);
        assertNull(result.get(0).getInputStream());

        verify(repository).findAllByOrderByUserIdAscFilenameAsc();
        verifyNoMoreInteractions(repository);
    }

    @Test
    public void findByIdShouldReturnSuccessfully() {
        //given
        Long id = 1L;

        UploadedFile uploadedFile = UploadedFile.builder()
                .id(1L)
                .filename("test.pdf")
                .content("test data".getBytes())
                .build();

        when(repository.findOne(id)).thenReturn(uploadedFile);

        //when
        UploadedFileResource uploadedFileResource = service.findById(id);

        //then
        assertNotNull(uploadedFileResource);
        assertNull(uploadedFileResource.getId());
        assertEquals(uploadedFileResource.getFilename(), "test.pdf");
        assertNotNull(uploadedFileResource.getInputStream());

        verify(repository).findOne(id);
        verifyNoMoreInteractions(repository);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void findByIdShouldThrowResourceNotFoundExceptionWhenUploadedFileDoesNotExist() {
        //given
        Long id = 1L;

        when(repository.findOne(id)).thenReturn(null);

        //when
        try {
            service.findById(id);
        } finally {
            //then
            verify(repository).findOne(id);
            verifyNoMoreInteractions(repository);
        }
    }

    @Test
    public void storeShouldReturnSuccessfullyWhenTransferIsNotChunked() throws Exception {
        //given
        String userId = "userId";
        String filename = "test.pdf";
        Instant uploadedTime = Instant.now();
        Integer totalChunks = null;
        MockMultipartFile multipartFile = dummyMultipartFile(filename);

        when(repository.existsByUserIdAndFilename(userId, filename)).thenReturn(false);

        //when
        service.store(userId, multipartFile, totalChunks, true, uploadedTime);

        //then
        verify(repository).existsByUserIdAndFilename(userId, filename);
        verify(repository).save(any(UploadedFile.class));
        verifyNoMoreInteractions(repository);
    }

    @Test
    public void storeShouldReturnSuccessfullyForChunkedTransferWhenReceivingFirstBytes() throws Exception {
        //given
        String userId = "userId";
        String filename = "test.pdf";
        Instant uploadedTime = Instant.now();
        Integer totalChunks = 1;
        MockMultipartFile multipartFile = dummyMultipartFile(filename);

        when(repository.existsByUserIdAndFilename(userId, filename)).thenReturn(false);

        //when
        service.store(userId, multipartFile, totalChunks, false, uploadedTime);

        //then
        verify(repository).existsByUserIdAndFilename(userId, filename);
        verify(repository).save(any(UploadedFile.class));
        verifyNoMoreInteractions(repository);
    }

    @Test
    public void storeShouldReturnSuccessfullyForChunkedTransferWhenReceivingLastBytes() throws Exception {
        //given
        String userId = "userId";
        String filename = "test.pdf";
        Instant uploadedTime = Instant.now();
        Integer totalChunks = 10;
        MockMultipartFile multipartFile = dummyMultipartFile(filename);

        when(repository.existsByUserIdAndFilename(userId, filename)).thenReturn(true);

        //when
        service.store(userId, multipartFile, totalChunks, true, uploadedTime);

        //then
        verify(repository).existsByUserIdAndFilename(userId, filename);
        verify(repository).updateByUserIdAndFilename(userId, filename, uploadedTime, COMPLETED, totalChunks, multipartFile.getBytes());
        verifyNoMoreInteractions(repository);
    }

    //TODO implement on before method: delete all files created for tests

    @Test(expected = StorageException.class)
    public void storeShouldThrowStorageExceptionForNotChunkedTransferWhenSomethingGoesWrong() throws Exception {
        //given
        String userId = "userId";
        String filename = "test.pdf";
        Instant uploadedTime = Instant.now();
        Integer totalChunks = 10;
        MockMultipartFile multipartFile = dummyMultipartFile(filename);

        when(repository.existsByUserIdAndFilename(userId, filename)).thenReturn(true);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                throw new IOException("error");
            }
        }).when(repository)
                .updateByUserIdAndFilename(userId, filename, uploadedTime, COMPLETED, totalChunks, multipartFile.getBytes());

        //when
        try {
            service.store(userId, multipartFile, totalChunks, true, uploadedTime);
        } finally {
            //then
            verify(repository).existsByUserIdAndFilename(userId, filename);
            verify(repository).updateByUserIdAndFilename(userId, filename, uploadedTime, COMPLETED, totalChunks, multipartFile.getBytes());
            verify(repository).updateByUserIdAndFilename(userId, filename, uploadedTime, FAILED, totalChunks, "Could not process given file: userId userId, filename test.pdf. Exception: error");
            verifyNoMoreInteractions(repository);
        }
    }

    private MockMultipartFile dummyMultipartFile(String filename) {
        return new MockMultipartFile("test", filename, "image/jpeg", "test".getBytes());
    }

}