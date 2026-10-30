package com.ssafy.keepick.external.storage;

import com.ssafy.keepick.external.s3.dto.S3ImagePathDto;
import com.ssafy.keepick.global.utils.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

/** Local demo storage. URLs are capabilities; do not log or share them publicly. */
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local")
public class LocalImageStorage implements ImageStorage {
    private final Path root;
    private final String baseUrl;
    private final String secret;

    public LocalImageStorage(@Value("${app.storage.local.root}") String root,
                             @Value("${app.storage.local.base-url}") String baseUrl,
                             @Value("${app.storage.local.secret}") String secret) {
        this.root = Path.of(root).toAbsolutePath().normalize();
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        if (secret.length() < 32) throw new IllegalArgumentException("Local storage secret must be at least 32 characters");
        this.secret = secret;
    }

    public S3ImagePathDto generatePresignedUrl(String name, String type) {
        FileUtils.validateFileName(name);
        FileUtils.validateContentType(type, name);
        String key = "originals/" + FileUtils.generateUniqueFileName(name);
        path(key);
        long expires = Instant.now().getEpochSecond() + 900;
        String uploadUrl = baseUrl + "/api/local-files?key=" + encode(key)
                + "&expires=" + expires + "&contentType=" + encode(type)
                + "&signature=" + sign("PUT\n" + key + "\n" + expires + "\n" + type);
        return S3ImagePathDto.of(uploadUrl, publicUrl(key));
    }

    public String publicUrl(String key) {
        path(key);
        return baseUrl + "/api/local-files?key=" + encode(key) + "&signature=" + sign("GET\n" + key);
    }

    public void verifyRead(String key, String signature) { path(key); verify("GET\n" + key, signature); }

    public void verifyUpload(String key, long expires, String type, String signature) {
        path(key);
        if (!key.startsWith("originals/") || expires < Instant.now().getEpochSecond()) forbidden();
        FileUtils.validateContentType(type, key);
        verify("PUT\n" + key + "\n" + expires + "\n" + type, signature);
    }

    public byte[] downloadFile(String key) {
        try { return Files.readAllBytes(path(key)); }
        catch (java.io.IOException e) { throw new ResponseStatusException(HttpStatus.NOT_FOUND); }
    }

    public void uploadFile(String key, byte[] content, String type) {
        Path dest = path(key);
        try {
            Files.createDirectories(dest.getParent());
            Path temp = Files.createTempFile(dest.getParent(), ".upload-", ".tmp");
            try {
                Files.write(temp, content);
                Files.move(temp, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } finally { Files.deleteIfExists(temp); }
        } catch (java.io.IOException e) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File write failed"); }
    }

    public String uploadThumbnail(String key, byte[] content) {
        String thumbnail = "thumbnails/" + FileUtils.extractImageNumber(key) + "/" + FileUtils.extractFileName(key);
        uploadFile(thumbnail, content, "image/jpeg");
        return thumbnail;
    }

    public boolean fileExists(String key) { return Files.isRegularFile(path(key)); }

    private Path path(String key) {
        if (key == null || key.contains("..") || key.contains("\\") || key.startsWith("/") || key.chars().anyMatch(Character::isISOControl))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file key");
        Path dest = root.resolve(key).normalize();
        if (!dest.startsWith(root) || dest.equals(root)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        for (Path p = dest; p != null && p.startsWith(root); p = p.getParent())
            if (Files.isSymbolicLink(p)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return dest;
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) { throw new IllegalStateException(e); }
    }

    private void verify(String value, String signature) {
        if (signature == null || !MessageDigest.isEqual(sign(value).getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) forbidden();
    }
    private static void forbidden() { throw new ResponseStatusException(HttpStatus.FORBIDDEN); }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
