package com.ssafy.keepick.album.tier.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ssafy.keepick.support.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.keepick.album.tier.application.TierAlbumService;
import com.ssafy.keepick.album.tier.application.dto.TierAlbumDetailDto;
import com.ssafy.keepick.album.tier.application.dto.TierAlbumDto;
import com.ssafy.keepick.album.tier.controller.TierAlbumController;
import com.ssafy.keepick.album.tier.controller.request.CreateTierAlbumRequest;
import com.ssafy.keepick.album.tier.controller.request.UpdateTierAlbumRequest;
import com.ssafy.keepick.album.tier.persistence.TierAlbumPhotoRepository;
import com.ssafy.keepick.album.tier.persistence.TierAlbumRepository;
import com.ssafy.keepick.global.response.PagingResponse;
import com.ssafy.keepick.photo.domain.Photo;
import com.ssafy.keepick.photo.persistence.PhotoRepository;

@ExtendWith(MockitoExtension.class)
class TierAlbumIntegrationTest extends BaseTest {

    private MockMvc mockMvc;

    @Mock
    private TierAlbumService tierAlbumService;

    @Mock
    private TierAlbumRepository tierAlbumRepository;

    @Mock
    private TierAlbumPhotoRepository tierAlbumPhotoRepository;

    @Mock
    private PhotoRepository photoRepository;

    @InjectMocks
    private TierAlbumController tierAlbumController;

    private ObjectMapper objectMapper;

    private Photo photo1, photo2;
    private TierAlbumDto tierAlbumDto;
    private TierAlbumDetailDto tierAlbumDetailDto;
    private PagingResponse<TierAlbumDto> tierAlbumListDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(tierAlbumController).build();
        objectMapper = new ObjectMapper();

        // 테스트 데이터 생성 (Builder 패턴 사용)
        photo1 = Photo.builder()
            .originalUrl("https://test.com/photo1_original.jpg")
            .takenAt(LocalDateTime.now())
            .width(1920)
            .height(1080)
            .build();

        photo2 = Photo.builder()
            .originalUrl("https://test.com/photo2_original.jpg")
            .takenAt(LocalDateTime.now())
            .width(1920)
            .height(1080)
            .build();

        // TierAlbumDto 설정
        tierAlbumDto = TierAlbumDto.builder()
            .id(1L)
            .name("테스트 앨범")
            .description("테스트 설명")
            .thumbnailUrl("https://example.com/thumb.jpg")
            .originalUrl("https://example.com/original.jpg")
            .photoCount(2)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        // PagingResponse<TierAlbumDto> 설정
        List<TierAlbumDto> albums = Arrays.asList(tierAlbumDto);
        Pageable pageable = PageRequest.of(0, 10);
        Page<TierAlbumDto> albumPage = new PageImpl<>(albums, pageable, 1L);
        tierAlbumListDto = PagingResponse.from(albumPage, album -> album);

        tierAlbumDetailDto = TierAlbumDetailDto.builder()
            .title("테스트 앨범")
            .description("테스트 설명")
            .thumbnailUrl("https://test.com/thumb.jpg")
            .originalUrl("https://test.com/original.jpg")
            .photoCount(2)
            .photos(new HashMap<>())
            .build();
    }

    @Test
    @DisplayName("티어 앨범 전체 플로우 테스트 - 생성부터 삭제까지")
    void tierAlbumFullFlowTest() throws Exception {
        // 1. 티어 앨범 생성
        Long groupId = 1L;
        CreateTierAlbumRequest createRequest = CreateTierAlbumRequest.builder()
            .photoIds(Arrays.asList(1L, 2L))
            .build();

        when(tierAlbumService.createTierAlbum(eq(groupId), anyList()))
            .thenReturn(tierAlbumDto);

        mockMvc.perform(post("/api/groups/{groupId}/tier-albums", groupId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data").value(1));

        // 2. 티어 앨범 목록 조회
        when(tierAlbumService.getTierAlbumListWithPaging(eq(groupId), eq(0), eq(10)))
            .thenReturn(tierAlbumListDto);

        mockMvc.perform(get("/api/groups/{groupId}/tier-albums", groupId)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.list").isArray());

        // 3. 티어 앨범 상세 조회
        Long tierAlbumId = 1L;
        when(tierAlbumService.getTierAlbumDetail(eq(groupId), eq(tierAlbumId)))
            .thenReturn(tierAlbumDetailDto);

        mockMvc.perform(get("/api/groups/{groupId}/tier-albums/{tierAlbumId}", groupId, tierAlbumId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.title").value("테스트 앨범"));

        // 4. 티어 앨범 수정
        Map<String, List<Long>> photos = new HashMap<>();
        photos.put("S", Arrays.asList(1L));
        photos.put("A", Arrays.asList(2L));
        
        UpdateTierAlbumRequest updateRequest = UpdateTierAlbumRequest.builder()
            .name("수정된 앨범")
            .description("수정된 설명")
            .thumbnailId(1L)
            .photos(photos)
            .build();

        when(tierAlbumService.updateTierAlbum(eq(groupId), eq(tierAlbumId), any(UpdateTierAlbumRequest.class)))
            .thenReturn(tierAlbumDto);

        mockMvc.perform(put("/api/groups/{groupId}/tier-albums/{tierAlbumId}", groupId, tierAlbumId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200));

        // 5. 티어 앨범 삭제
        doNothing().when(tierAlbumService).deleteTierAlbum(eq(groupId), eq(tierAlbumId));

        mockMvc.perform(delete("/api/groups/{groupId}/tier-albums/{tierAlbumId}", groupId, tierAlbumId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200));

        // 검증
        verify(tierAlbumService).createTierAlbum(eq(groupId), anyList());
        verify(tierAlbumService).getTierAlbumListWithPaging(eq(groupId), eq(0), eq(10));
        verify(tierAlbumService).getTierAlbumDetail(eq(groupId), eq(tierAlbumId));
        verify(tierAlbumService).updateTierAlbum(eq(groupId), eq(tierAlbumId), any(UpdateTierAlbumRequest.class));
        verify(tierAlbumService).deleteTierAlbum(eq(groupId), eq(tierAlbumId));
    }

    @Test
    @DisplayName("티어 앨범 생성 실패 - 유효하지 않은 요청")
    void createTierAlbum_InvalidRequest() throws Exception {
        // given
        Long groupId = 1L;
        CreateTierAlbumRequest request = CreateTierAlbumRequest.builder()
            .photoIds(Arrays.asList()) // 빈 리스트
            .build();

        // when & then
        mockMvc.perform(post("/api/groups/{groupId}/tier-albums", groupId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("티어 앨범 수정 거절 - 빈 이름")
    void updateTierAlbum_RejectsEmptyName() throws Exception {
        // given
        Long groupId = 1L;
        Long tierAlbumId = 1L;
        UpdateTierAlbumRequest request = UpdateTierAlbumRequest.builder()
            .name("") // 빈 이름
            .build();

        // when & then
        mockMvc.perform(put("/api/groups/{groupId}/tier-albums/{tierAlbumId}", groupId, tierAlbumId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
        org.mockito.Mockito.verify(tierAlbumService, org.mockito.Mockito.never())
            .updateTierAlbum(eq(groupId), eq(tierAlbumId), any(UpdateTierAlbumRequest.class));
    }

    @Test
    @DisplayName("티어 앨범 목록 조회 - 페이징 테스트")
    void getTierAlbumList_PagingTest() throws Exception {
        // given
        Long groupId = 1L;
        int page = 1;
        int size = 5;

        when(tierAlbumService.getTierAlbumListWithPaging(eq(groupId), eq(page), eq(size)))
            .thenReturn(tierAlbumListDto);

        // when & then
        mockMvc.perform(get("/api/groups/{groupId}/tier-albums", groupId)
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.pageInfo.page").value(0));

        verify(tierAlbumService).getTierAlbumListWithPaging(eq(groupId), eq(page), eq(size));
    }
}
