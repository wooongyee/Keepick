package com.ssafy.keepick.feature.tier.tiermaker.components

import android.content.ClipData
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ssafy.keepick.feature.group.DisplayGroupAlbum
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraggableImage(
    item: DisplayGroupAlbum,
    sourceTierId: String,
    onDragStart: (item: DisplayGroupAlbum, sourceTierId: String) -> Unit
) {
    Box(
        modifier = Modifier.dragAndDropSource {
            detectDragGestures(
                onDragStart = {
                    Log.d("TierMaker", "DragStart: ${item.title} from $sourceTierId")
                    onDragStart(item, sourceTierId)
                    startTransfer(
                        DragAndDropTransferData(
                            ClipData.newPlainText("item_id", item.id.toString())
                        )
                    )
                },
                onDrag = { _, _ -> }
            )
        }
    ) {
        AsyncImage(
            model = item.thumbnailUrl,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .shadow(2.dp, RoundedCornerShape(8.dp))
        )
    }
}
