package com.audio.converter.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class GCPService {

    @Value("${gcp.storage.bucket-name}")
    private String bucketName;

    @Value("${gcp.storage.folder-name}")
    private String folderName;

    private final Storage storage;

    public GCPService() throws IOException {
        storage = StorageOptions.newBuilder()
                .setCredentials(GoogleCredentials.fromStream(getClass().getResourceAsStream("/audio_converter_key.json")))
                .build()
                .getService();
    }

    public String uploadFile(File file) throws IOException {
        Storage storage = StorageOptions.getDefaultInstance().getService();
        String objectName = folderName.concat("/").concat(file.getName());

        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName).build();
        storage.create(blobInfo, Files.readAllBytes(file.toPath()));

        return objectName;
    }

    public Resource getFileBytes(String objectName) throws IOException {
        Blob blob = storage.get(BlobId.of(bucketName, objectName));
        if (blob == null) {
            throw new RuntimeException("File not found in GCS: " + objectName);
        }

        byte[] content = blob.getContent();
        return new ByteArrayResource(content);
    }
}
