package com.ssafy.keepick.external.s3;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.ssafy.keepick.global.exception.BaseException;
import com.ssafy.keepick.global.exception.ErrorCode;
import com.ssafy.keepick.global.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Slf4j
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3", matchIfMissing = true)
@Service
@RequiredArgsConstructor
public class S3FileOperationService {

    private final S3Client s3Client;

    @Value("${app.aws.s3.bucket-name}")
    private String bucketName;

    @Value("${app.aws.s3.thumbnails-prefix}")
    private String thumbnailsPrefix;

    /**
     * S3에서 파일 다운로드
     */
    public byte[] downloadFile(String objectKey) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getObjectRequest);
            byte[] fileContent = response.readAllBytes();

            log.debug("S3에서 파일 다운로드: {} ({} bytes)", objectKey, fileContent.length);
            return fileContent;

        } catch (IOException e) {
            log.error("S3 파일 읽기 실패: {}", objectKey, e);
            throw new BaseException(ErrorCode.INTERNAL_S3_ERROR);
        } catch (Exception e) {
            log.error("S3 파일 다운로드 실패3: {}", objectKey, e);
            throw new BaseException(ErrorCode.INTERNAL_S3_ERROR);
        }
    }

    /**
     * S3에 파일 업로드
     */
    public void uploadFile(String objectKey, byte[] fileContent, String contentType) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(contentType)
                    .contentLength((long) fileContent.length)
                    .build();

            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(new ByteArrayInputStream(fileContent), fileContent.length));

            log.debug("S3 파일 업로드: {} ({} bytes)", objectKey, fileContent.length);

        } catch (Exception e) {
            log.error("S3 파일 업로드 실패: {}", objectKey, e);
            throw new BaseException(ErrorCode.INTERNAL_S3_ERROR);
        }
    }

    /**
     * 썸네일 파일을 S3에 업로드
     */
    public String uploadThumbnail(String originalObjectKey, byte[] thumbnailContent) {
        String fileName = FileUtils.extractFileName(originalObjectKey);
        String ImageNumber = FileUtils.extractImageNumber(originalObjectKey);
        String thumbnailKey = thumbnailsPrefix + ImageNumber + "/" + fileName;
        uploadFile(thumbnailKey, thumbnailContent, "image/jpeg");
        log.info("썸네일 업로드: {} -> {}", originalObjectKey, thumbnailKey);
        return thumbnailKey;
    }

    /**
     * 파일이 S3에 존재하는지 확인
     */
    public boolean fileExists(String objectKey) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;

        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.error("S3 파일이 존재하지 않음: {}", objectKey, e);
            return false;
        }
    }
}