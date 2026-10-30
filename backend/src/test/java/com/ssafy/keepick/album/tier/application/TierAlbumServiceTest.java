package com.ssafy.keepick.album.tier.application;

import com.ssafy.keepick.album.tier.application.dto.TierAlbumDetailDto;
import com.ssafy.keepick.album.tier.application.dto.TierAlbumDto;
import com.ssafy.keepick.album.tier.controller.request.UpdateTierAlbumRequest;
import com.ssafy.keepick.album.tier.domain.Tier;
import com.ssafy.keepick.album.tier.domain.TierAlbum;
import com.ssafy.keepick.album.tier.domain.TierAlbumPhoto;
import com.ssafy.keepick.album.tier.persistence.TierAlbumPhotoRepository;
import com.ssafy.keepick.album.tier.persistence.TierAlbumRepository;
import com.ssafy.keepick.global.entity.BaseTimeEntity;
import com.ssafy.keepick.global.exception.BaseException;
import com.ssafy.keepick.global.exception.ErrorCode;
import com.ssafy.keepick.global.response.PagingResponse;
import com.ssafy.keepick.photo.domain.Photo;
import com.ssafy.keepick.photo.persistence.PhotoRepository;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TierAlbumServiceTest extends BaseTest {

    @Mock
    private TierAlbumRepository tierAlbumRepository;

    @Mock
    private TierAlbumPhotoRepository tierAlbumPhotoRepository;

    @Mock
    private PhotoRepository photoRepository;

    @InjectMocks
    private TierAlbumService tierAlbumService;

    private TierAlbum tierAlbum;
    private Photo photo1, photo2;
    private TierAlbumPhoto tierAlbumPhoto1, tierAlbumPhoto2;
    private UpdateTierAlbumRequest updateRequest;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 설정
        tierAlbum = TierAlbum.createTierAlbum("테스트 앨범", "테스트 설명", 
            "https://test.com/thumb.jpg", "https://test.com/original.jpg", 1L);
        tierAlbum.updatePhotoCount(2);
        
        // Reflection을 사용하여 BaseTimeEntity 필드 설정
        try {
            java.lang.reflect.Field createdAtField = BaseTimeEntity.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(tierAlbum, LocalDateTime.of(2024, 1, 15, 10, 30));
            
            java.lang.reflect.Field updatedAtField = BaseTimeEntity.class.getDeclaredField("updatedAt");
            updatedAtField.setAccessible(true);
            updatedAtField.set(tierAlbum, LocalDateTime.of(2024, 1, 15, 14, 45));
        } catch (Exception e) {
            // 테스트 환경에서는 무시
        }

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

        tierAlbumPhoto1 = TierAlbumPhoto.createTierAlbumPhoto(tierAlbum, photo1, Tier.S, 0);
        tierAlbumPhoto2 = TierAlbumPhoto.createTierAlbumPhoto(tierAlbum, photo2, Tier.A, 1);
        
        // Photo에 ID 설정
        try {
            java.lang.reflect.Field photo1IdField = Photo.class.getDeclaredField("id");
            photo1IdField.setAccessible(true);
            photo1IdField.set(photo1, 1L);
            
            java.lang.reflect.Field photo2IdField = Photo.class.getDeclaredField("id");
            photo2IdField.setAccessible(true);
            photo2IdField.set(photo2, 2L);
        } catch (Exception e) {
            // 테스트 환경에서는 무시
        }

        Map<String, List<Long>> photos = new HashMap<>();
        photos.put("S", Arrays.asList(1L));
        photos.put("A", Arrays.asList(2L));
        
        updateRequest = UpdateTierAlbumRequest.builder()
            .name("수정된 앨범")
            .description("수정된 설명")
            .thumbnailId(1L)
            .photos(photos)
            .build();
    }

    @Test
    @DisplayName("티어 앨범 생성 성공")
    void createTierAlbum_Success() {
        // given
        Long groupId = 1L;
        List<Long> photoIds = Arrays.asList(1L, 2L);

        when(tierAlbumRepository.save(any(TierAlbum.class))).thenReturn(tierAlbum);
        when(photoRepository.findById(1L)).thenReturn(Optional.of(photo1));
        when(photoRepository.findById(2L)).thenReturn(Optional.of(photo2));
        when(tierAlbumPhotoRepository.save(any(TierAlbumPhoto.class))).thenReturn(tierAlbumPhoto1);

        // when
        TierAlbumDto result = tierAlbumService.createTierAlbum(groupId, photoIds);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(tierAlbum.getId());
        verify(tierAlbumRepository).save(any(TierAlbum.class));
        verify(photoRepository, times(2)).findById(anyLong());
        verify(tierAlbumPhotoRepository, times(2)).save(any(TierAlbumPhoto.class));
    }

    @Test
    @DisplayName("티어 앨범 생성 실패 - 존재하지 않는 사진")
    void createTierAlbum_Fail_PhotoNotFound() {
        // given
        Long groupId = 1L;
        Long currentUserId = 1L;
        List<Long> photoIds = Arrays.asList(1L, 2L);


        when(tierAlbumRepository.save(any(TierAlbum.class))).thenReturn(tierAlbum);
        when(photoRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> tierAlbumService.createTierAlbum(groupId, photoIds))
            .isInstanceOf(BaseException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("티어 앨범 목록 조회 성공")
    void getTierAlbumListWithPaging_Success() {
        // given
        Long groupId = 1L;
        int page = 0;
        int size = 10;
        List<TierAlbum> albums = Arrays.asList(tierAlbum);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<TierAlbum> albumPage = new PageImpl<>(albums, pageable, 1L);

        when(tierAlbumRepository.findByGroupIdWithPaging(groupId, pageable)).thenReturn(albumPage);

        // when
        PagingResponse<TierAlbumDto> result = tierAlbumService.getTierAlbumListWithPaging(groupId, page, size);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getName()).isEqualTo("테스트 앨범");
        assertThat(result.getList().get(0).getCreatedAt()).isNotNull();
        assertThat(result.getList().get(0).getUpdatedAt()).isNotNull();
        assertThat(result.getPageInfo().getTotalElement()).isEqualTo(1L);
        assertThat(result.getPageInfo().getPage()).isEqualTo(0);
        assertThat(result.getPageInfo().getSize()).isEqualTo(10);
        assertThat(result.getPageInfo().getTotalPage()).isEqualTo(1);
        assertThat(result.getPageInfo().isHasNext()).isFalse();
        verify(tierAlbumRepository).findByGroupIdWithPaging(groupId, pageable);
    }

    @Test
    @DisplayName("티어 앨범 목록 조회 - 빈 결과")
    void getTierAlbumListWithPaging_EmptyResult() {
        // given
        Long groupId = 1L;
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<TierAlbum> albumPage = new PageImpl<>(Arrays.asList(), pageable, 0L);

        when(tierAlbumRepository.findByGroupIdWithPaging(groupId, pageable)).thenReturn(albumPage);

        // when
        PagingResponse<TierAlbumDto> result = tierAlbumService.getTierAlbumListWithPaging(groupId, page, size);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getList()).isEmpty();
        assertThat(result.getPageInfo().getTotalElement()).isEqualTo(0L);
        assertThat(result.getPageInfo().getTotalPage()).isEqualTo(0);
        assertThat(result.getPageInfo().isHasNext()).isFalse();
    }

    @Test
    @DisplayName("티어 앨범 목록 조회 - 페이징 계산 검증")
    void getTierAlbumListWithPaging_PagingCalculation() {
        // given
        Long groupId = 1L;
        int page = 2;
        int size = 5;
        List<TierAlbum> albums = Arrays.asList(tierAlbum);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending()); // API uses zero-based pages
        Page<TierAlbum> albumPage = new PageImpl<>(albums, pageable, 15L);

        when(tierAlbumRepository.findByGroupIdWithPaging(groupId, pageable)).thenReturn(albumPage);

        // when
        PagingResponse<TierAlbumDto> result = tierAlbumService.getTierAlbumListWithPaging(groupId, page, size);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPageInfo().getPage()).isEqualTo(2);
        assertThat(result.getPageInfo().getSize()).isEqualTo(5);
        assertThat(result.getPageInfo().getTotalElement()).isEqualTo(15L);
        assertThat(result.getPageInfo().getTotalPage()).isEqualTo(3);
        assertThat(result.getPageInfo().isHasNext()).isFalse(); // 마지막 페이지
        verify(tierAlbumRepository).findByGroupIdWithPaging(groupId, pageable);
    }

    @Test
    @DisplayName("티어 앨범 상세 조회 성공")
    void getTierAlbumDetail_Success() {
        // given
        Long groupId = 1L;
        Long tierAlbumId = 1L;

        when(tierAlbumRepository.findAlbumWithPhotosById(tierAlbumId))
            .thenReturn(Optional.of(tierAlbum));

        // when
        TierAlbumDetailDto result = tierAlbumService.getTierAlbumDetail(groupId, tierAlbumId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo(tierAlbum.getName());
        verify(tierAlbumRepository).findAlbumWithPhotosById(tierAlbumId);
    }

    @Test
    @DisplayName("티어 앨범 상세 조회 실패 - 존재하지 않는 앨범")
    void getTierAlbumDetail_Fail_AlbumNotFound() {
        // given
        Long groupId = 1L;
        Long tierAlbumId = 1L;

        when(tierAlbumRepository.findAlbumWithPhotosById(tierAlbumId))
            .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> tierAlbumService.getTierAlbumDetail(groupId, tierAlbumId))
            .isInstanceOf(BaseException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALBUM_NOT_FOUND);
    }

    @Test
    @DisplayName("티어 앨범 수정 성공")
    void updateTierAlbum_Success() {
        // given
        Long groupId = 1L;
        Long tierAlbumId = 1L;

        when(tierAlbumRepository.findAlbumWithPhotosById(tierAlbumId))
            .thenReturn(Optional.of(tierAlbum))
            .thenReturn(Optional.of(tierAlbum)); // 두 번째 호출을 위한 모킹

        // TierAlbum을 Mock으로 생성하여 getAllPhotos() 메서드를 모킹
        TierAlbum mockTierAlbum = mock(TierAlbum.class);
        when(mockTierAlbum.getId()).thenReturn(tierAlbumId);
        when(mockTierAlbum.getName()).thenReturn("테스트 앨범");
        when(mockTierAlbum.getDescription()).thenReturn("테스트 설명");
        when(mockTierAlbum.getThumbnailUrl()).thenReturn("https://test.com/thumb.jpg");
        when(mockTierAlbum.getOriginalUrl()).thenReturn("https://test.com/original.jpg");
        when(mockTierAlbum.getPhotoCount()).thenReturn(2);
        when(mockTierAlbum.getAllPhotos()).thenReturn(Arrays.asList(tierAlbumPhoto1, tierAlbumPhoto2));

        when(tierAlbumRepository.findAlbumWithPhotosById(tierAlbumId))
            .thenReturn(Optional.of(mockTierAlbum))
            .thenReturn(Optional.of(mockTierAlbum)); // 두 번째 호출을 위한 모킹
        when(photoRepository.findById(1L)).thenReturn(Optional.of(photo1));
        when(tierAlbumPhotoRepository.findByAlbumId(tierAlbumId)).thenReturn(Arrays.asList(tierAlbumPhoto1, tierAlbumPhoto2));

        // when
        TierAlbumDto result = tierAlbumService.updateTierAlbum(groupId, tierAlbumId, updateRequest);

        // then
        assertThat(result).isNotNull();
        verify(tierAlbumRepository).findAlbumWithPhotosById(tierAlbumId);
        verify(photoRepository).findById(1L);
        verify(tierAlbumPhotoRepository).findByAlbumId(tierAlbumId);
    }

    @Test
    @DisplayName("티어 앨범 수정 실패 - 존재하지 않는 앨범")
    void updateTierAlbum_Fail_AlbumNotFound() {
        // given
        Long groupId = 1L;
        Long tierAlbumId = 1L;

        when(tierAlbumRepository.findAlbumWithPhotosById(tierAlbumId))
            .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> tierAlbumService.updateTierAlbum(groupId, tierAlbumId, updateRequest))
            .isInstanceOf(BaseException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALBUM_NOT_FOUND);
    }

    @Test
    @DisplayName("티어 앨범 삭제 성공")
    void deleteTierAlbum_Success() {
        // given
        Long groupId = 1L;
        Long tierAlbumId = 1L;

        when(tierAlbumRepository.findAlbumById(tierAlbumId))
            .thenReturn(Optional.of(tierAlbum));

        // when
        tierAlbumService.deleteTierAlbum(groupId, tierAlbumId);

        // then
        verify(tierAlbumRepository).findAlbumById(tierAlbumId);
        assertThat(tierAlbum.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("티어 앨범 삭제 실패 - 존재하지 않는 앨범")
    void deleteTierAlbum_Fail_AlbumNotFound() {
        // given
        Long groupId = 1L;
        Long tierAlbumId = 1L;

        when(tierAlbumRepository.findAlbumById(tierAlbumId))
            .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> tierAlbumService.deleteTierAlbum(groupId, tierAlbumId))
            .isInstanceOf(BaseException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALBUM_NOT_FOUND);
    }

    @Test
    @DisplayName("티어 앨범에 사진 추가 성공")
    void addPhotoToTierAlbum_Success() {
        // given
        Long groupId = 1L;
        Long tierAlbumId = 1L;
        List<Long> photoIds = Arrays.asList(3L, 4L);
        Photo newPhoto1 = Photo.builder().originalUrl("https://test.com/photo3.jpg").build();
        Photo newPhoto2 = Photo.builder().originalUrl("https://test.com/photo4.jpg").build();
        newPhoto1.uploadThumbnail("https://test.com/thumb3.jpg");
        newPhoto2.uploadThumbnail("https://test.com/thumb4.jpg");

        when(tierAlbumRepository.findAlbumWithPhotosById(tierAlbumId)).thenReturn(Optional.of(tierAlbum));
        when(photoRepository.findById(3L)).thenReturn(Optional.of(newPhoto1));
        when(photoRepository.findById(4L)).thenReturn(Optional.of(newPhoto2));
        when(tierAlbumPhotoRepository.save(any(TierAlbumPhoto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        var result = tierAlbumService.uploadPhotoToTierAlbum(groupId, tierAlbumId, photoIds);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getOriginalUrl()).isEqualTo("https://test.com/photo3.jpg");
        assertThat(result.get(1).getOriginalUrl()).isEqualTo("https://test.com/photo4.jpg");
        verify(tierAlbumPhotoRepository, times(2)).save(any(TierAlbumPhoto.class));
    }

    @Test
    @DisplayName("티어 앨범에 사진 추가 실패 - 이미 앨범에 포함된 사진")
    void addPhotoToTierAlbum_Fail_PhotoAlreadyInAlbum() {
        // given
        Long groupId = 1L;
        Long tierAlbumId = 1L;
        List<Long> photoIds = Arrays.asList(1L); // 이미 앨범에 있는 사진

        // Mock TierAlbum 생성
        TierAlbum mockTierAlbum = mock(TierAlbum.class);
        when(mockTierAlbum.getAllPhotos()).thenReturn(Arrays.asList(tierAlbumPhoto1, tierAlbumPhoto2));
        
        when(tierAlbumRepository.findAlbumWithPhotosById(tierAlbumId)).thenReturn(Optional.of(mockTierAlbum));
        when(photoRepository.findById(1L)).thenReturn(Optional.of(photo1));

        // when & then
        assertThatThrownBy(() -> tierAlbumService.uploadPhotoToTierAlbum(groupId, tierAlbumId, photoIds))
            .isInstanceOf(BaseException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PARAMETER);
    }

    @Test
    @DisplayName("티어 앨범에서 사진 삭제 성공")
    void deletePhotoFromTierAlbum_Success() {
        // given
        Long groupId = 1L;
        Long tierAlbumId = 1L;
        List<Long> photoIds = Arrays.asList(1L);
        List<TierAlbumPhoto> remainingPhotos = Arrays.asList(tierAlbumPhoto2);

        // Mock TierAlbum 생성
        TierAlbum mockTierAlbum = mock(TierAlbum.class);
        when(mockTierAlbum.getAllPhotos()).thenReturn(Arrays.asList(tierAlbumPhoto1, tierAlbumPhoto2));
        
        when(tierAlbumRepository.findAlbumWithPhotosById(tierAlbumId)).thenReturn(Optional.of(mockTierAlbum));
        when(tierAlbumPhotoRepository.findByAlbumId(tierAlbumId)).thenReturn(remainingPhotos);

        // when
        tierAlbumService.deletePhotoFromTierAlbum(groupId, tierAlbumId, photoIds);

        // then
        verify(tierAlbumPhotoRepository).delete(tierAlbumPhoto1);
        verify(tierAlbumPhotoRepository).findByAlbumId(tierAlbumId);
    }

    @Test
    @DisplayName("티어 앨범에서 사진 삭제 실패 - 앨범에 포함되지 않은 사진")
    void deletePhotoFromTierAlbum_Fail_PhotoNotInAlbum() {
        // given
        Long groupId = 1L;
        Long tierAlbumId = 1L;
        List<Long> photoIds = Arrays.asList(999L); // 앨범에 없는 사진

        when(tierAlbumRepository.findAlbumWithPhotosById(tierAlbumId)).thenReturn(Optional.of(tierAlbum));

        // when & then
        assertThatThrownBy(() -> tierAlbumService.deletePhotoFromTierAlbum(groupId, tierAlbumId, photoIds))
            .isInstanceOf(BaseException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }
}
