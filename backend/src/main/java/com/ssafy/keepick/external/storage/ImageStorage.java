package com.ssafy.keepick.external.storage;

import com.ssafy.keepick.external.s3.dto.S3ImagePathDto;

public interface ImageStorage {
    S3ImagePathDto generatePresignedUrl(String fileName, String contentType);
    byte[] downloadFile(String objectKey);
    void uploadFile(String objectKey, byte[] content, String contentType);
    String uploadThumbnail(String originalObjectKey, byte[] content);
    boolean fileExists(String objectKey);
    String publicUrl(String objectKey);
}
