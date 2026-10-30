package com.ssafy.keepick.external.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class LocalImageStorageTest {
    @TempDir Path root;
    private LocalImageStorage storage() { return new LocalImageStorage(root.toString(), "http://localhost:18080", "a-test-secret-with-at-least-32-characters"); }
    private Map<String, String> query(String url) {
        return Arrays.stream(URI.create(url).getRawQuery().split("&"))
                .map(part -> part.split("=", 2)).collect(Collectors.toMap(p -> p[0], p -> URLDecoder.decode(p[1], StandardCharsets.UTF_8)));
    }

    @Test void signedUploadAndReadSurviveStorageRecreation() {
        var storage = storage();
        var urls = storage.generatePresignedUrl("42/photo.png", "image/png");
        var q = query(urls.getPresignedUrl());
        storage.verifyUpload(q.get("key"), Long.parseLong(q.get("expires")), q.get("contentType"), q.get("signature"));
        byte[] bytes = {1, 2, 3};
        storage.uploadFile(q.get("key"), bytes, "image/png");
        var read = query(urls.getPublicUrl());
        storage().verifyRead(read.get("key"), read.get("signature"));
        assertArrayEquals(bytes, storage().downloadFile(read.get("key")));
    }

    @Test void cannotChangeUploadKeyTypeExpiryOrUseReadSignatureForWrite() {
        var storage = storage();
        var urls = storage.generatePresignedUrl("42/photo.png", "image/png");
        var q = query(urls.getPresignedUrl());
        long expiry = Long.parseLong(q.get("expires"));
        assertThrows(ResponseStatusException.class, () -> storage.verifyUpload(q.get("key") + "other", expiry, "image/png", q.get("signature")));
        assertThrows(ResponseStatusException.class, () -> storage.verifyUpload(q.get("key"), expiry, "image/jpeg", q.get("signature")));
        assertThrows(ResponseStatusException.class, () -> storage.verifyUpload(q.get("key"), 1, "image/png", q.get("signature")));
        var read = query(urls.getPublicUrl());
        assertThrows(ResponseStatusException.class, () -> storage.verifyUpload(q.get("key"), expiry, "image/png", read.get("signature")));
    }

    @Test void pathTraversalAndUnsignedReadsAreRejected() {
        assertThrows(ResponseStatusException.class, () -> storage().downloadFile("../outside"));
        assertThrows(ResponseStatusException.class, () -> storage().downloadFile("/outside"));
        assertThrows(ResponseStatusException.class, () -> storage().verifyRead("originals/test.png", "wrong"));
    }

    @Test void groupThumbnailKeepsPhotoIdInKey() {
        var key = storage().uploadThumbnail("originals/42/photo.png", new byte[]{4});
        assertEquals("thumbnails/42/photo.png", key);
        assertTrue(storage().fileExists(key));
    }
}
