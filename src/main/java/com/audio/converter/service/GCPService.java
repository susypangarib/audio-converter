package com.audio.converter.service;

import com.audio.converter.model.ResponseCode;
import com.audio.converter.util.BusinessLogicException;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Slf4j
@Service
public class GCPService {

    @Value("${gcp.storage.bucket-name}")
    private String bucketName;

    @Value("${gcp.storage.folder-name}")
    private String folderName;

    private final Storage storage;

    public GCPService(Storage storage) {
        this.storage = storage;
    }

    public String uploadFile(File file) throws IOException {
        String objectName = folderName.concat("/").concat(file.getName());

        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName).build();
        storage.create(blobInfo, Files.readAllBytes(file.toPath()));

        return objectName;
    }

    public Resource getFileBytes(String objectName) throws IOException {
        Blob blob = storage.get(BlobId.of(bucketName, objectName));
        if (blob == null) {
            log.error("Failed to retrieve File not found in GCS:  path={}", objectName);
            throw new BusinessLogicException(ResponseCode.FILE_NOT_EXIST.getCode(), ResponseCode.FILE_NOT_EXIST.getMessage());
        }

        byte[] content = blob.getContent();
        return new ByteArrayResource(content);
    }
}
