package com.ssafy.keepick.external.storage;

import com.ssafy.keepick.external.s3.S3FileOperationService;
import com.ssafy.keepick.external.s3.S3PresignedUrlService;
import com.ssafy.keepick.external.s3.dto.S3ImagePathDto;
import com.ssafy.keepick.global.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3", matchIfMissing = true)
public class AwsImageStorage implements ImageStorage {
    private final S3PresignedUrlService urls;
    private final S3FileOperationService files;
    @Value("${app.aws.s3.bucket-name}") private String bucket;
    @Value("${spring.cloud.aws.region.static}") private String region;

    public S3ImagePathDto generatePresignedUrl(String name, String type) { return urls.generatePresignedUrl(name, type); }
    public byte[] downloadFile(String key) { return files.downloadFile(key); }
    public void uploadFile(String key, byte[] data, String type) { files.uploadFile(key, data, type); }
    public String uploadThumbnail(String key, byte[] data) { return files.uploadThumbnail(key, data); }
    public boolean fileExists(String key) { return files.fileExists(key); }
    public String publicUrl(String key) { return FileUtils.generatePublicUrl(bucket, region, key); }
}
