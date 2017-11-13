package fileuploader.services;

import fileuploader.controller.resources.UploadedFileResource;
import fileuploader.domain.UploadedFile;
import fileuploader.exceptions.ResourceNotFoundException;
import fileuploader.projection.UploadedFileInfo;
import fileuploader.repositories.UploadedFileRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;

import static fileuploader.enums.FileUploadStatus.COMPLETED;
import static fileuploader.enums.FileUploadStatus.PENDING;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

}