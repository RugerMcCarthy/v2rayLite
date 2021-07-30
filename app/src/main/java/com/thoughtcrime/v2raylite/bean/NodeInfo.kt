package com.thoughtcrime.v2raylite.bean


import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

sealed class NodeSelectStatus {
    object Selected: NodeSelectStatus()
    object Unselected: NodeSelectStatus()
}


data class NodeInfo(val remark: String, val guid: String = "")

class NodeState(val nodeInfo: NodeInfo, val nodeIndex: Int, selectStatus: NodeSelectStatus = NodeSelectStatus.Unselected) {
    var selectStatus: NodeSelectStatus by mutableStateOf(selectStatus)
    private set

    val selectStatusBarPadding = Animatable(if (selectStatus == NodeSelectStatus.Unselected) 40.dp else 0.dp, Dp.VectorConverter)

    fun isSelected() = selectStatus == NodeSelectStatus.Selected

    suspend fun select() {
        selectStatus = NodeSelectStatus.Selected
        selectStatusBarPadding.animateTo(
            0.dp,
            tween(250)
        )
    }

    suspend fun unSelect() {
        selectStatus = NodeSelectStatus.Unselected
        selectStatusBarPadding.animateTo(
            40.dp,
            tween(250)
        )
    }
}

