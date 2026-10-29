package com.ssafy.keepick.core.data.photo
import android.net.Uri
import com.ssafy.keepick.feature.gallery.model.GalleryImage
import com.ssafy.keepick.feature.gallery.model.ImageStatus
import com.ssafy.keepick.feature.gallery.similarphotos.PhotoCluster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class AnalysisState(
    val isAnalyzing: Boolean = false,
    val progress: Float = 0f
)
@Singleton
class ImageRepository @Inject constructor() {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // 1. 메인 갤러리 이미지 목록
    private val _images = MutableStateFlow<List<GalleryImage>>(emptyList())
    val images: StateFlow<List<GalleryImage>> = _images.asStateFlow()

    // --- 핵심 수정 1: 유사 사진 클러스터 상태를 별도로 영속 관리 ---
    // nullable: null = 분석한 적 없음, emptyList = 분석했으나 결과 없음
    private val _similarPhotoClusters = MutableStateFlow<List<PhotoCluster>?>(null)
    val similarPhotoClusters: StateFlow<List<PhotoCluster>?> = _similarPhotoClusters.asStateFlow()

    private val _analysisState = MutableStateFlow(AnalysisState())
    val analysisState: StateFlow<AnalysisState> = _analysisState.asStateFlow()


    /**
     * 특정 그룹의 이미지 목록을 로드 (가상 데이터)
     */
    suspend fun loadImagesForGroup(groupName: String) {
        // 실제로는 여기서 DB나 서버 API를 호출합니다.
        delay(500) // 네트워크 지연 시뮬레이션
        val newImages = when (groupName) {
            "SSAFY 13기 D207" -> (1..10).map {
                GalleryImage(id = "ssafy-${it}", imageUrl = "https://picsum.photos/id/${10 + it}/200", date = "2025.08.10", tags = listOf("#인물", "#스터디"), status = ImageStatus.COMPLETE)
            }
            "가족방" -> (1..5).map {
                GalleryImage(id = "family-${it}", imageUrl = "https://picsum.photos/id/${20 + it}/200", date = "2025.08.09", tags = listOf("#풍경", "#여행"), status = ImageStatus.COMPLETE)
            }
            else -> (1..20).map {
                GalleryImage(id = "default-${it}", imageUrl = "https://picsum.photos/id/${30 + it}/200", date = "2025.08.08", tags = listOf("#음식", "#도시"), status = ImageStatus.COMPLETE)
            }
        }
        _images.update { newImages }
    }

    /**
     * 새로운 이미지를 추가하고, 백그라운드에서 업로드 및 분석 프로세스를 시작합니다.
     */
    fun addAndProcessImages(uris: List<Uri>) {
        val newImages = uris.map { uri ->
            GalleryImage(id = UUID.randomUUID().toString(), imageUrl = uri.toString(), date = "방금 전", tags = emptyList(), status = ImageStatus.UPLOADING)
        }
        // 먼저 UI에 '업로드 중' 상태로 즉시 반영
        _images.update { newImages + it }

        // 백그라운드에서 실제 처리 시작
        repositoryScope.launch {
            startUploadAndAnalysis(newImages)
        }
    }

    /**
     * (Private) 이미지 업로드 및 분석 과정을 시뮬레이션하고 상태를 업데이트합니다.
     */
    private suspend fun startUploadAndAnalysis(images: List<GalleryImage>) {
        // 1. 업로드 상태 -> 분석 상태로 전환 시뮬레이션
        images.forEach { image ->
            delay(200) // 개별 업로드 시간 시뮬레이션
            _images.update { list -> list.map { if (it.id == image.id) it.copy(status = ImageStatus.ANALYZING) else it } }
        }

        // 2. 전체 분석 진행률 업데이트 (이 로직은 유사 사진 분석과 공유될 수 있습니다)
        _analysisState.update { it.copy(isAnalyzing = true, progress = 0f) }
        val totalAnalysisTime = 1000L + (images.size * 500L)
        var elapsedTime = 0L
        val updateInterval = 100L

        while (elapsedTime < totalAnalysisTime) {
            delay(updateInterval)
            elapsedTime += updateInterval
            _analysisState.update { it.copy(progress = (elapsedTime.toFloat() / totalAnalysisTime).coerceAtMost(1.0f)) }
        }

        // 3. 분석 완료 처리: 상태를 COMPLETE로 바꾸고 태그 추가
        val analyzedIds = images.map { it.id }
        _images.update { list ->
            list.map { if (it.id in analyzedIds) it.copy(status = ImageStatus.COMPLETE, tags = listOf("#풍경", "#바다", "#김민수").shuffled()) else it }
        }
        _analysisState.update { it.copy(isAnalyzing = false, progress = 1f) }
    }
    /**
     * 유사 사진 분석을 요청하고, 결과를 _similarPhotoClusters에 업데이트
     */
    suspend fun analyzeSimilarPhotos() {
        _analysisState.update { it.copy(isAnalyzing = true, progress = 0f) }
        (1..10).forEach { step ->
            delay(400)
            _analysisState.update { it.copy(progress = step / 10f) }
        }
        _analysisState.update { it.copy(isAnalyzing = false, progress = 1f) }

        val currentImages = _images.value
        val result = if (currentImages.size > 5) {
            listOf(
                PhotoCluster(id = "clusterA", images = currentImages.subList(0, 2)),
                PhotoCluster(id = "clusterB", images = currentImages.subList(2, 5))
            )
        } else {
            emptyList()
        }
        // 결과를 별도의 StateFlow에 저장
        _similarPhotoClusters.value = result
    }

    /**
     * --- 핵심 수정 2: 사진 삭제 시, 두 데이터 소스 모두에서 제거 ---
     */
    suspend fun deletePhotos(idsToDelete: List<String>) {
        delay(300) // API 호출 시뮬레이션

        // 1. 메인 이미지 목록에서 삭제
        _images.update { currentList ->
            currentList.filterNot { it.id in idsToDelete }
        }

        // 2. 유사 사진 클러스터 목록에서도 삭제
        _similarPhotoClusters.update { currentClusters ->
            currentClusters?.map { cluster ->
                cluster.copy(images = cluster.images.filterNot { it.id in idsToDelete })
            }?.filter { it.images.isNotEmpty() }
        }
    }
}
