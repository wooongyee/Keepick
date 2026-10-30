package com.ssafy.keepick.photo.application;
import com.ssafy.keepick.external.storage.ImageStorage;
import com.ssafy.keepick.external.s3.dto.S3ImagePathDto;
import com.ssafy.keepick.photo.application.dto.GroupPhotoCommandDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageStorage storage;

    // Presigned URL 관련 메서드들
    public S3ImagePathDto generatePresignedUrl(String fileName, String contentType) {
        return storage.generatePresignedUrl(fileName, contentType);
    }

    public List<S3ImagePathDto> generatePresignedUrls(List<GroupPhotoCommandDto> groupPhotoCommandDtoList) {
        groupPhotoCommandDtoList.forEach(GroupPhotoCommandDto::validate);
        return groupPhotoCommandDtoList.stream().map(info -> storage.generatePresignedUrl(info.getFileName(), info.getContentType())).toList();
    }

    public String publicUrl(String objectKey) {
        return storage.publicUrl(objectKey);
    }

    // 파일 작업 관련 메서드들
    public byte[] downloadFile(String objectKey) {
        return storage.downloadFile(objectKey);
    }

    public void uploadFile(String objectKey, byte[] fileContent, String contentType) {
        storage.uploadFile(objectKey, fileContent, contentType);
    }

    public String uploadThumbnail(String originalObjectKey, byte[] thumbnailContent) {
        return storage.uploadThumbnail(originalObjectKey, thumbnailContent);
    }

    public boolean fileExists(String objectKey) {
        return storage.fileExists(objectKey);
    }
}