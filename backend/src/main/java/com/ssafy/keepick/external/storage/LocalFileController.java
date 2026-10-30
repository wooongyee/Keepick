package com.ssafy.keepick.external.storage;

import com.ssafy.keepick.photo.application.ThumbnailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local")
public class LocalFileController {
    private static final int MAX_BYTES = 10 * 1024 * 1024;
    private final LocalImageStorage storage;
    private final ThumbnailService thumbnails;
    private final com.ssafy.keepick.photo.persistence.PhotoRepository photos;

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Void> statusError(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).build();
    }

    @PutMapping("/api/local-files")
    public ResponseEntity<Void> upload(@RequestParam String key, @RequestParam long expires,
            @RequestParam String contentType, @RequestParam String signature, HttpServletRequest request) throws IOException {
        storage.verifyUpload(key, expires, contentType, signature);
        verifyActivePhoto(key);
        if (request.getContentLengthLong() > MAX_BYTES) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
        byte[] data = request.getInputStream().readNBytes(MAX_BYTES + 1);
        if (data.length > MAX_BYTES) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
        // Inspect dimensions before decoding to bound demo thumbnail work.
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Image format not supported locally");
            var reader = readers.next();
            try {
                reader.setInput(input);
                if ((long) reader.getWidth(0) * reader.getHeight(0) > 40_000_000L)
                    throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
            } finally { reader.dispose(); }
        }
        byte[] thumbnail = thumbnails.generateThumbnail(data);
        storage.uploadFile(key, data, contentType);
        // Group photos use originals/{photoId}/{filename}; profile photos have no photoId.
        if (key.matches("originals/\\d+/[^/]+")) {
            String thumbnailKey = storage.uploadThumbnail(key, thumbnail);
            thumbnails.updatePhotoAddThumbnail(thumbnailKey);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/local-files")
    public ResponseEntity<byte[]> read(@RequestParam String key, @RequestParam String signature) throws IOException {
        storage.verifyRead(key, signature);
        verifyActivePhoto(key);
        byte[] bytes = storage.downloadFile(key);
        String type = key.startsWith("thumbnails/") ? "image/jpeg" : detectType(bytes);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(type))
                .header("X-Content-Type-Options", "nosniff")
                .header("Referrer-Policy", "no-referrer")
                .cacheControl(CacheControl.noStore()).body(bytes);
    }

    private String detectType(byte[] bytes) throws IOException {
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) return "application/octet-stream";
            var reader = readers.next();
            try { return "image/" + reader.getFormatName().toLowerCase().replace("jpg", "jpeg"); }
            finally { reader.dispose(); }
        }
    }

    private void verifyActivePhoto(String key) {
        if (key.matches("(?:originals|thumbnails)/\\d+/[^/]+")) {
            long id = Long.parseLong(key.split("/")[1]);
            var photo = photos.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            if (photo.getDeletedAt() != null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
