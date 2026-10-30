package com.ssafy.keepick.external.storage;

import com.ssafy.keepick.photo.application.ThumbnailService;
import com.ssafy.keepick.photo.domain.Photo;
import com.ssafy.keepick.photo.persistence.PhotoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LocalFileControllerTest {
    @TempDir Path root;
    LocalImageStorage storage;
    PhotoRepository photos;
    MockMvc mvc;

    @BeforeEach void setup() {
        storage = new LocalImageStorage(root.toString(), "http://localhost:18080", "a-test-secret-with-at-least-32-characters");
        photos = mock(PhotoRepository.class);
        mvc = MockMvcBuilders.standaloneSetup(new LocalFileController(storage, mock(ThumbnailService.class), photos)).build();
    }

    Map<String, String> query(String url) {
        return Arrays.stream(URI.create(url).getRawQuery().split("&")).map(s -> s.split("=", 2))
                .collect(Collectors.toMap(p -> p[0], p -> URLDecoder.decode(p[1], StandardCharsets.UTF_8)));
    }

    @Test void signedReadReturnsExactBytesButTamperingIsForbidden() throws Exception {
        var image = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", image);
        storage.uploadFile("originals/avatar.png", image.toByteArray(), "image/png");
        var q = query(storage.publicUrl("originals/avatar.png"));
        mvc.perform(get("/api/local-files").param("key", q.get("key")).param("signature", q.get("signature")))
                .andExpect(status().isOk()).andExpect(content().bytes(image.toByteArray()))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        mvc.perform(get("/api/local-files").param("key", q.get("key")).param("signature", "wrong"))
                .andExpect(status().isForbidden());
    }

    @Test void deletedPhotoCannotBeReadOrReuploadedEvenWithValidSignatures() throws Exception {
        var photo = Photo.builder().build();
        photo.delete();
        when(photos.findById(42L)).thenReturn(Optional.of(photo));
        var urls = storage.generatePresignedUrl("42/photo.png", "image/png");
        var read = query(urls.getPublicUrl());
        mvc.perform(get("/api/local-files").param("key", read.get("key")).param("signature", read.get("signature")))
                .andExpect(status().isNotFound());
        var put = query(urls.getPresignedUrl());
        mvc.perform(put("/api/local-files").param("key", put.get("key")).param("expires", put.get("expires"))
                .param("contentType", put.get("contentType")).param("signature", put.get("signature")))
                .andExpect(status().isNotFound());
    }

    @Test void nonImageUploadIsRejectedBeforeAnyFileIsStored() throws Exception {
        var q = query(storage.generatePresignedUrl("avatar.png", "image/png").getPresignedUrl());
        mvc.perform(put("/api/local-files").param("key", q.get("key")).param("expires", q.get("expires"))
                .param("contentType", q.get("contentType")).param("signature", q.get("signature"))
                .content("not an image").contentType("image/png")).andExpect(status().isUnsupportedMediaType());
        org.junit.jupiter.api.Assertions.assertFalse(storage.fileExists(q.get("key")));
    }
}
