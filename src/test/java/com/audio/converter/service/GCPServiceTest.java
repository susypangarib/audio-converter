package com.audio.converter.service;

import com.audio.converter.util.BusinessLogicException;
import com.google.cloud.storage.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ByteArrayResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GCPServiceTest {

    @Mock
    private Storage storage;

    @InjectMocks
    private GCPService gcpService;

    @Before
    public void setUp() throws IllegalAccessException, NoSuchFieldException {
        MockitoAnnotations.initMocks(this);
        gcpService = new GCPService(storage);
        // Inject mocked storage object
        java.lang.reflect.Field bucketNameField = GCPService.class.getDeclaredField("bucketName");
        bucketNameField.setAccessible(true);
        bucketNameField.set(gcpService, "audio_converter_thp");

        java.lang.reflect.Field folderNameField = GCPService.class.getDeclaredField("folderName");
        folderNameField.setAccessible(true);
        folderNameField.set(gcpService, "converted-audio");


    }

    @Test
    public void testUploadFile() throws IOException {

        BlobInfo mockBlobInfo = BlobInfo.newBuilder("bucket-name", "file-name").build();
        Blob mockBlob = Mockito.mock(Blob.class);

        Mockito.when(storage.create(Mockito.any(BlobInfo.class), Mockito.any(byte[].class)))
                .thenReturn(mockBlob);

        File tempFile = File.createTempFile("test-audio", ".wav");
        Files.write(tempFile.toPath(), "dummy data".getBytes());
        gcpService.uploadFile(tempFile);

        Mockito.verify(storage).create(Mockito.any(BlobInfo.class), Mockito.any(byte[].class));
    }

    @Test
    public void testGetFileBytes_Success() throws IOException {
        String objectName = "converted-audio/test.wav";
        BlobId blobId = BlobId.of("audio_converter_thp", objectName);
        Blob blob = mock(Blob.class);
        byte[] fileContent = "dummy data".getBytes();

        when(storage.get(blobId)).thenReturn(blob);
        when(blob.getContent()).thenReturn(fileContent);

        Resource resource = gcpService.getFileBytes(objectName);

        assertNotNull(resource);
        assertArrayEquals(fileContent, ((ByteArrayResource) resource).getByteArray());
    }

    @Test(expected = BusinessLogicException.class)
    public void testGetFileBytes_FileNotFound() throws IOException {
        String objectName = "converted-audio/nonexistent.wav";
        when(storage.get(BlobId.of("audio_converter_thp", objectName))).thenReturn(null);

        gcpService.getFileBytes(objectName);
    }
}
