package com.ssafy.keepick.album.tier.controller;

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
import java.util.LinkedHashMap;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.ssafy.keepick.global.response.PagingResponse;
import com.ssafy.keepick.global.exception.GlobalExceptionHandler;
import com.ssafy.keepick.album.tier.application.TierAlbumService;
import com.ssafy.keepick.album.tier.application.dto.TierAlbumDetailDto;
import com.ssafy.keepick.album.tier.application.dto.TierAlbumDto;
import com.ssafy.keepick.album.tier.controller.request.CreateTierAlbumRequest;
import com.ssafy.keepick.album.tier.controller.request.UpdateTierAlbumRequest;

@ExtendWith(MockitoExtension.class)
class TierAlbumControllerTest extends BaseTest {

    private MockMvc mockMvc;

    @Mock
    private TierAlbumService tierAlbumService;

    @InjectMocks
    private TierAlbumController tierAlbumController;

    private ObjectMapper objectMapper;

    private TierAlbumDto tierAlbumDto;
    private TierAlbumDetailDto tierAlbumDetailDto;
    private PagingResponse<TierAlbumDto> tierAlbumListDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(tierAlbumController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
        objectMapper = new ObjectMapper();
        // Spring 설정과 동일하게 설정
        objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, false);
        
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

        // UNASSIGNED 사진들을 포함한 테스트 데이터 생성
        Map<String, List<TierAlbumDetailDto.TierAlbumPhotoDto>> testPhotos = new LinkedHashMap<>();
        testPhotos.put("S", Arrays.asList());
        testPhotos.put("A", Arrays.asList());
        testPhotos.put("B", Arrays.asList());
        testPhotos.put("C", Arrays.asList());
        testPhotos.put("D", Arrays.asList());
        testPhotos.put("UNASSIGNED", Arrays.asList(
            TierAlbumDetailDto.TierAlbumPhotoDto.builder()
                .photoId(1L)
                .thumbnailUrl("https://test.com/thumb1.jpg")
                .originalUrl("https://test.com/original1.jpg")
                .sequence(0)
                .build(),
            TierAlbumDetailDto.TierAlbumPhotoDto.builder()
                .photoId(2L)
                .thumbnailUrl("https://test.com/thumb2.jpg")
                .originalUrl("https://test.com/original2.jpg")
                .sequence(1)
                .build()
        ));

        tierAlbumDetailDto = TierAlbumDetailDto.builder()
            .title("테스트 앨범")
            .description("테스트 설명")
            .thumbnailUrl("https://test.com/thumb.jpg")
            .originalUrl("https://test.com/original.jpg")
            .photoCount(2)
            .photos(testPhotos)
            .build();
    }

    @Test
    @DisplayName("티어 앨범 생성 API 성공")
    void createTierAlbum_Success() throws Exception {
        // given
        Long groupId = 1L;
        CreateTierAlbumRequest request = CreateTierAlbumRequest.builder()
            .photoIds(Arrays.asList(1L, 2L))
            .build();

        when(tierAlbumService.createTierAlbum(eq(groupId), anyList()))
            .thenReturn(tierAlbumDto);

        // when & then
        mockMvc.perform(post("/api/groups/{groupId}/tier-albums", groupId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("티어 앨범 생성 API 실패 - 빈 사진 리스트")
    void createTierAlbum_Fail_EmptyPhotoList() throws Exception {
        // given
        Long groupId = 1L;
        CreateTierAlbumRequest request = CreateTierAlbumRequest.builder()
            .photoIds(Arrays.asList())
            .build();

        // when & then
        mockMvc.perform(post("/api/groups/{groupId}/tier-albums", groupId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("티어 앨범 목록 조회 API 성공")
    void getTierAlbumList_Success() throws Exception {
        // given
        Long groupId = 1L;
        int page = 0;
        int size = 10;

        when(tierAlbumService.getTierAlbumListWithPaging(eq(groupId), eq(page), eq(size)))
            .thenReturn(tierAlbumListDto);

        // when & then
        mockMvc.perform(get("/api/groups/{groupId}/tier-albums", groupId)
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.list").isArray())
            .andExpect(jsonPath("$.data.list[0].id").value(1))
            .andExpect(jsonPath("$.data.list[0].name").value("테스트 앨범"))
            .andExpect(jsonPath("$.data.list[0].photoCount").value(2))
            .andExpect(jsonPath("$.data.list[0].createdAt").exists())
            .andExpect(jsonPath("$.data.list[0].updatedAt").exists())
            .andExpect(jsonPath("$.data.pageInfo.page").value(0))
            .andExpect(jsonPath("$.data.pageInfo.size").value(10))
            .andExpect(jsonPath("$.data.pageInfo.totalElement").value(1))
            .andExpect(jsonPath("$.data.pageInfo.totalPage").value(1))
            .andExpect(jsonPath("$.data.pageInfo.hasNext").value(false));
    }

    @Test
    @DisplayName("티어 앨범 목록 조회 API - 기본값 테스트")
    void getTierAlbumList_DefaultValues() throws Exception {
        // given
        Long groupId = 1L;

        when(tierAlbumService.getTierAlbumListWithPaging(eq(groupId), eq(0), eq(10)))
            .thenReturn(tierAlbumListDto);

        // when & then (파라미터 없이 호출)
        mockMvc.perform(get("/api/groups/{groupId}/tier-albums", groupId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @DisplayName("티어 앨범 목록 조회 API - 빈 결과")
    void getTierAlbumList_EmptyResult() throws Exception {
        // given
        Long groupId = 1L;
        int page = 0;
        int size = 10;

        // 빈 결과를 위한 DTO 생성
        Pageable pageable = PageRequest.of(0, 10);
        Page<TierAlbumDto> emptyPage = new PageImpl<>(Arrays.asList(), pageable, 0L);
        PagingResponse<TierAlbumDto> emptyListDto = PagingResponse.from(emptyPage, album -> album);

        when(tierAlbumService.getTierAlbumListWithPaging(eq(groupId), eq(page), eq(size)))
            .thenReturn(emptyListDto);

        // when & then
        mockMvc.perform(get("/api/groups/{groupId}/tier-albums", groupId)
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.list").isArray())
            .andExpect(jsonPath("$.data.list").isEmpty())
            .andExpect(jsonPath("$.data.pageInfo.totalElement").value(0));
    }

    @Test
    @DisplayName("티어 앨범 상세 조회 API 성공")
    void getTierAlbum_Success() throws Exception {
        // given
        Long groupId = 1L;
        Long tierAlbumId = 1L;

        when(tierAlbumService.getTierAlbumDetail(eq(groupId), eq(tierAlbumId)))
            .thenReturn(tierAlbumDetailDto);

        // when & then
        String response = mockMvc.perform(get("/api/groups/{groupId}/tier-albums/{tierAlbumId}", groupId, tierAlbumId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.title").value("테스트 앨범"))
            .andExpect(jsonPath("$.data.photoCount").value(2))
            .andExpect(jsonPath("$.data.photos.S").isArray())
            .andExpect(jsonPath("$.data.photos.A").isArray())
            .andExpect(jsonPath("$.data.photos.B").isArray())
            .andExpect(jsonPath("$.data.photos.C").isArray())
            .andExpect(jsonPath("$.data.photos.D").isArray())
            .andExpect(jsonPath("$.data.photos.UNASSIGNED").isArray())
            .andExpect(jsonPath("$.data.photos.UNASSIGNED").isNotEmpty())
            .andExpect(jsonPath("$.data.photos.UNASSIGNED[0].photoId").value(1))
            .andExpect(jsonPath("$.data.photos.UNASSIGNED[1].photoId").value(2))
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        System.out.println("=== 실제 JSON 응답 ===");
        System.out.println(response);
        System.out.println("=====================");
    }

    @Test
    @DisplayName("티어 앨범 수정 API 성공")
    void updateTierAlbum_Success() throws Exception {
        // given
        Long groupId = 1L;
        Long tierAlbumId = 1L;
        Map<String, List<Long>> photos = new HashMap<>();
        photos.put("S", Arrays.asList(1L));
        photos.put("A", Arrays.asList(2L));
        
        UpdateTierAlbumRequest request = UpdateTierAlbumRequest.builder()
            .name("수정된 앨범")
            .description("수정된 설명")
            .thumbnailId(1L)
            .photos(photos)
            .build();

        when(tierAlbumService.updateTierAlbum(eq(groupId), eq(tierAlbumId), any(UpdateTierAlbumRequest.class)))
            .thenReturn(tierAlbumDto);

        // when & then
        mockMvc.perform(put("/api/groups/{groupId}/tier-albums/{tierAlbumId}", groupId, tierAlbumId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("티어 앨범 수정 API 거절 - 빈 이름")
    void updateTierAlbum_RejectsEmptyName() throws Exception {
        // given
        Long groupId = 1L;
        Long tierAlbumId = 1L;
        UpdateTierAlbumRequest request = UpdateTierAlbumRequest.builder()
            .name("")
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
    @DisplayName("티어 앨범 삭제 API 성공")
    void deleteTierAlbum_Success() throws Exception {
        // given
        Long groupId = 1L;
        Long tierAlbumId = 1L;

        doNothing().when(tierAlbumService).deleteTierAlbum(eq(groupId), eq(tierAlbumId));

        // when & then
        mockMvc.perform(delete("/api/groups/{groupId}/tier-albums/{tierAlbumId}", groupId, tierAlbumId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200));

        verify(tierAlbumService).deleteTierAlbum(groupId, tierAlbumId);
    }
}
